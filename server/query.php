<?php

// Disable error reporting
error_reporting(0);
ini_set('display_errors', 'off');

/**
 *
 * To use this script, the user must always provide following fields in the body of the request:
 * - username
 * - password
 * - action
 *
 * Failing to provide any of the fields will result into an error response.
 * Furthermore, the [username] and [password] must match the values below and
 * [action] must be one of 'load', 'register', 'unregister' or 'refresh'
 *
 */

// Authentication
define("USERNAME_KEY", "username");
define("PASSWORD_KEY", "password");

// PHP credentials, used by client app (such as the android app) to authenticate itself to use this script
define("APP_USERNAME", "ISC_username");
define("APP_PASSWORD", "ISC_password");

// MySQL credentials, used by the php script to access the database
define("DB_SERVER", "localhost");
define("DB_USERNAME", "isc");
define("DB_PASSWORD", "iscpass");
define("DB_DATABASE", "isc");

define("ACTION_KEY", "action");
define("CARD_NUMBER_KEY", "card_number");
define("USER_ID_KEY", "user_id");
define("TRIP_ID_KEY", "trip_id");

// Query actions
define("ACTION_PING", "ping");
define("ACTION_TRIPS", "trips");
define("ACTION_LOAD", "load");
define("ACTION_REGISTER", "register");
define("ACTION_UNREGISTER", "unregister");
define("ACTION_REFRESH", "refresh");

// Results
define("FIELD_STATUS", "status");
define("FIELD_USER", "user");
define("FIELD_TRIPS", "trips");
define("FIELD_TYPE", "type");
define("STATUS_SUCCESS", "success");
define("STATUS_ERROR", "error");

// Errors
define("ERR_AUTHENTICATION", "AUTH");
define("ERR_DATABASE", "DB");
define("ERR_INTERNAL", "INTERNAL");


class HttpException {

   var $status_code;
   var $message;

   function __construct($status_code, $message) {
       $this->status_code = $status_code;
       $this->message = $message;
   }

   function status_code() {
       return $this->status_code;
   }

   function message() {
       return $this->message;
   }
}

/**
 * Connects to the DB with data defined at the top of the script.
 * Client app should not access the DB directly, but rather ask for the
 * data throught this script.
 */
function connect_database() {
  $mysqli = new mysqli(DB_SERVER, DB_USERNAME, DB_PASSWORD, DB_DATABASE);

  // Return Internal error if we cannot access the database.
  if ($mysqli->connect_errno) {
    generate_error(500 /* internal error */, "Unable to connect to database. " . $mysqli->connect_error);
  }

  // activate reporting
  $driver = new mysqli_driver();
  $driver->report_mode = MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT;

  // required to correctly parse varchar in mysql as a string in php
  $mysqli->query("SET CHARACTER SET utf8");

  return $mysqli;
}

function get_user_from_esn_card_number($mysqli, $esn_card_number) {
  $sql = "
  SELECT id_user, first_name, last_name, sex, esn_card_number, faculty
  FROM exchange_students
  JOIN people USING (id_user)
  JOIN faculties USING (id_faculty)
  WHERE esn_card_number = '" . $mysqli->real_escape_string($esn_card_number) . "'";

  if ($rs = $mysqli->query($sql)) {
    $rows_returned = $rs->num_rows;

    if ($rows_returned < 1) {
      $mysqli->close();
      generate_error(400 /* Bad request */, "Card number not registered.");
    } else if ($rows_returned > 1) {
      $mysqli->close();
      generate_error(500 /* Internal error */, "Card number not unique.");
    }

    $rs->data_seek(0);
    $user = $rs->fetch_assoc();
    $rs->free();

    return $user;
  } else {
    generate_error(500 /* Internal error */, "Internal SQL error.");
  }
}

function get_trips($mysqli) {
  $sql = "
  SELECT id_trip, trip_name, trip_description, trip_organizers, trip_date_from,
    trip_date_to, trip_capacity, trip_price, COUNT(id_user) AS trip_participants
  FROM trips
  LEFT OUTER JOIN trip_registrations USING (id_trip)
  WHERE trip_active = 'y'
  GROUP BY id_trip
  ORDER BY ISNULL(trip_date_from), trip_date_from, ISNULL(trip_date_to), trip_date_to ASC";

  if ($stmt = $mysqli->prepare($sql)) {
    $stmt->execute();
    $result = $stmt->get_result();
    $trips = array();
    while ($trip = $result->fetch_assoc()) {
        $trips[] = $trip;
    }
    $stmt->close();
    return $trips;
  } else {
    generate_error(500 /* Internal error */, "Internal SQL error.");
  }
}

function get_trips_for_user($mysqli, $user_id) {
  $sql = "
  SELECT id_trip, trip_name, trip_description, trip_organizers, trip_date_from,
    trip_date_to, trip_capacity, trip_price, COUNT(id_user) AS trip_participants,
    IF(SUM(IF(id_user = ?, 1, 0)) > 0, 'y', 'n') AS registered
  FROM trips
  LEFT OUTER JOIN trip_registrations USING (id_trip)
  WHERE trip_active = 'y'
  GROUP BY id_trip
  ORDER BY ISNULL(trip_date_from), trip_date_from, ISNULL(trip_date_to), trip_date_to ASC";

  if ($stmt = $mysqli->prepare($sql)) {
    $stmt->bind_param("d", $user_id);
    $stmt->execute();
    $result = $stmt->get_result();
    $trips = array();
    while ($trip = $result->fetch_assoc()) {
        $trips[] = $trip;
    }
    $stmt->close();
    return $trips;
  } else {
    generate_error(500 /* Internal error */, "Internal SQL error.");
  }
}

function register_trip($mysqli, $trip_id, $user_id) {
  $sql = "
  INSERT INTO trip_registrations (id_user, id_trip, registration_date)
  SELECT '" . $mysqli->real_escape_string($user_id) . "', '" . $mysqli->real_escape_string($trip_id) . "', NOW()
  FROM trips as t
  WHERE
    t.id_trip = '" . $mysqli->real_escape_string($trip_id) . "'
    AND (
      t.trip_capacity IS NULL
      OR
      t.trip_capacity > (
        SELECT COUNT(tr.id_user)
        FROM trip_registrations as tr
        WHERE tr.id_trip = '" . $mysqli->real_escape_string($trip_id) ."'
      )
    )";
  $mysqli->query($sql);
  if ($mysqli->connect_errno) {
    generate_error(500 /* Internal error */, "Internal SQL error.");
  }
}

function unregister_trip($mysqli, $trip_id, $user_id) {
  $sql = "
  DELETE FROM trip_registrations
  WHERE
    id_user = '" . $mysqli->real_escape_string($user_id) . "'
    AND
    id_trip = '" . $mysqli->real_escape_string($trip_id) . "'";
  $mysqli->query($sql);
  if ($mysqli->connect_errno) {
    generate_error(500 /* Internal error */, "Internal SQL error.");
  }
}


function generate_result($mysqli, $user, $user_id) {
  $result = array();
  $result[FIELD_STATUS] = STATUS_SUCCESS;
  $result[FIELD_USER] = $user;
  $result[FIELD_TRIPS] = get_trips_for_user($mysqli, $user_id);

  return json_encode($result);
}

function generate_error($code, $message) {
  http_response_code($code);
  die(json_encode(new HttpException($code, $message)));
}


// Die if the client didn't specify credentials
if (!array_key_exists(USERNAME_KEY, $_REQUEST) ||
    !array_key_exists(PASSWORD_KEY, $_REQUEST)) {
  generate_error(401 /* Unauthorized */, "Missing credentials.");
}

// Die if the client provided invalid credentials
if ($_REQUEST[USERNAME_KEY] != APP_USERNAME ||
    $_REQUEST[PASSWORD_KEY] != APP_PASSWORD) {
  generate_error(401 /* Unauthorized */, "Invalid credentials.");
}

// Die if the client didn't specify action
if (!array_key_exists(ACTION_KEY, $_REQUEST)) {
  generate_error(400 /* Bad request */, "Invalid credentials.");
}


$action = $_REQUEST[ACTION_KEY];
switch ($action) {
  case ACTION_PING:
    http_response_code(200); // OK
    exit;


  case ACTION_TRIPS:
    $mysqli = connect_database();
    $trips = get_trips($mysqli);
    echo json_encode($trips);
    break;


  case ACTION_LOAD:
    if (!array_key_exists(CARD_NUMBER_KEY, $_REQUEST)) {
      generate_error(400 /* Bad request */, "Missing card number.");
    }
    $card_number = $_REQUEST[CARD_NUMBER_KEY];

    $mysqli = connect_database();
    $user = get_user_from_esn_card_number($mysqli, $card_number);
    echo generate_result($mysqli, $user, $user["id_user"]);
    break;


  case ACTION_REGISTER:
  case ACTION_UNREGISTER:
    if (!array_key_exists(USER_ID_KEY, $_REQUEST) ||
        !array_key_exists(TRIP_ID_KEY, $_REQUEST)) {
      generate_error(400 /* Bad request */, "Missing user ID or trip ID.");
    }
    $user_id = $_REQUEST[USER_ID_KEY];
    $trip_id = $_REQUEST[TRIP_ID_KEY];

    $mysqli = connect_database();
    if ($action == ACTION_REGISTER) {
      register_trip($mysqli, $trip_id, $user_id);
    } else {
      unregister_trip($mysqli, $trip_id, $user_id);
    }
    echo generate_result($mysqli, null, $user_id);
    break;


  case ACTION_REFRESH:
    if (!array_key_exists(USER_ID_KEY, $_REQUEST)) {
      generate_error(400 /* Bad request */, "Missing user ID.");
    }
    $user_id = $_REQUEST[USER_ID_KEY];

    $mysqli = connect_database();
    echo generate_result($mysqli, null, $user_id);
    break;


  default:
    generate_error(400 /* Bad request */, "Invalid action '" . $action ."'.");
    exit;
}

// Disconnect from the database
$mysqli->close();

?>
