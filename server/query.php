<?php

// Disable error reporting
error_reporting(0);
ini_set('display_errors', 'off');


// Query fields
define("USERNAME_KEY", "username");
define("PASSWORD_KEY", "password");
define("ACTION_KEY", "action");
define("CARD_NUMBER_KEY", "card_number");
define("USER_ID_KEY", "user_id");
define("TRIP_ID_KEY", "trip_id");

// Query actions
define("ACTION_LOAD", "load");
define("ACTION_REGISTER", "register");
define("ACTION_UNREGISTER", "unregister");
define("ACTION_REFRESH", "refresh");

// PHP credentials
define("APP_USERNAME", "ISC_test");
define("APP_PASSWORD", "ISC_p455w0rd");

// MySQL credentials
define("DB_SERVER", "sql7.freesqldatabase.com");
define("DB_USERNAME", "sql7253591");
define("DB_PASSWORD", "k5BiHsVY1Y");
define("DATABASE", "sql7253591");

// Results
define("FIELD_STATUS", "status");
define("FIELD_DATA", "data");
define("FIELD_TRIPS", "trips");
define("FIELD_TYPE", "type");
define("STATUS_SUCCESS", "success");
define("STATUS_ERROR", "error");

// Errors
define("ERR_AUTHENTICATION", "AUTH");
define("ERR_DATABASE", "DB");
define("ERR_INTERNAL", "INTERNAL");
define("ERR_CARD", "CARD");


function generate_result($data, $user_id) {
  $result = array();
  $result[FIELD_STATUS] = STATUS_SUCCESS;
  $result[FIELD_DATA] = $data;

  // Retrieve trips
  $trips_result = mysql_query("SELECT id_trip, trip_name, trip_description,"
      . "     trip_organizers, trip_date_from, trip_date_to, trip_capacity,"
      . "     trip_price, COUNT(id_user) AS trip_participants,"
      . "     IF(SUM(IF(id_user = '" . mysql_real_escape_string($user_id) . "'"
      . "         , 1, 0)) > 0, 'y', 'n') AS registered"
      . " FROM trips"
      . " LEFT OUTER JOIN trip_registrations USING (id_trip)"
      . " WHERE trip_active = 'y'"
      . " GROUP BY id_trip"
      . " ORDER BY ISNULL(trip_date_from), trip_date_from,"
      . "     ISNULL(trip_date_to), trip_date_to ASC");
  $result[FIELD_TRIPS] = array();
  while ($t = mysql_fetch_assoc($trips_result)) {
    $result[FIELD_TRIPS][] = $t;
  }

  return json_encode($result);
}

function generate_error($type) {
  $result = array();
  $result[FIELD_STATUS] = STATUS_ERROR;
  $result[FIELD_TYPE] = $type;
  return json_encode($result);
}

function connect_database() {
  $connection = mysql_connect(DB_SERVER, DB_USERNAME, DB_PASSWORD);
  if (!$connection) {
    die(generate_error(ERR_DATABASE)); // MySQL connection error
  }
  mysql_set_charset("utf8");
  $database = mysql_select_db(DATABASE);
  if (!$database) {
    die(generate_error(ERR_DATABASE)); // Database connection error
  }
}


// Authentication
if (!array_key_exists(USERNAME_KEY, $_REQUEST) ||
    !array_key_exists(PASSWORD_KEY, $_REQUEST) ||
    $_REQUEST[USERNAME_KEY] != APP_USERNAME ||
    $_REQUEST[PASSWORD_KEY] != APP_PASSWORD) {
  die(generate_error(ERR_AUTHENTICATION)); // Authentication error
}

// Retrieve action
if (!array_key_exists(ACTION_KEY, $_REQUEST)) {
  die(generate_error(ERR_INTERNAL)); // Missing action
}
$action = $_REQUEST[ACTION_KEY];

switch ($action) {
  // ===========================================================================
  //   LOAD ACTION
  // ===========================================================================
  case ACTION_LOAD:
    // Retrieve card number
    if (!array_key_exists(CARD_NUMBER_KEY, $_REQUEST)) {
      die(generate_error(ERR_INTERNAL)); // Missing card number
    }
    $card_number = $_REQUEST[CARD_NUMBER_KEY];

    // Connect to the database
    connect_database();

    // Retrieve student data
    $result = mysql_query("SELECT id_user, first_name, last_name, sex,"
        . " faculty"
        . " FROM exchange_students"
        . " JOIN people USING (id_user)"
        . " JOIN faculties USING (id_faculty)"
        . " WHERE esn_card_number = '" . mysql_real_escape_string($card_number)
        . "'");
    $rows = array();
    while ($r = mysql_fetch_assoc($result)) {
      $rows[] = $r;
    }

    if (count($rows) < 1) {
      echo generate_error(ERR_CARD); // Card number not registered
      mysql_close();
      exit;
    } else if (count($rows) > 1) {
      echo generate_error(ERR_INTERNAL); // Card number not unique
      mysql_close();
      exit;
    }

    $data = $rows[0];
    $user_id = $data["id_user"];

    // Return results
    echo generate_result($data, $user_id);
    break;

  // ===========================================================================
  //   (UN)REGISTER ACTION
  // ===========================================================================
  case ACTION_REGISTER:
  case ACTION_UNREGISTER:
    // Retrieve user ID and trip ID
    if (!array_key_exists(USER_ID_KEY, $_REQUEST) ||
        !array_key_exists(TRIP_ID_KEY, $_REQUEST)) {
      die(generate_error(ERR_INTERNAL)); // Missing user ID or trip ID
    }
    $user_id = $_REQUEST[USER_ID_KEY];
    $trip_id = $_REQUEST[TRIP_ID_KEY];

    // Connect to the database
    connect_database();

    // (Un)register the student
    if ($action == ACTION_REGISTER) {
      mysql_query("INSERT INTO trip_registrations (id_user, id_trip,"
          . "     registration_date)"
          . " SELECT '" . mysql_real_escape_string($user_id) . "'"
          . "     , '" . mysql_real_escape_string($trip_id) . "'"
          . "     , NOW()"
          . " FROM trips as t"
          . " WHERE t.id_trip = '" . mysql_real_escape_string($trip_id) . "'"
          . " AND (t.trip_capacity IS NULL"
          . "     OR t.trip_capacity >"
          . "         (SELECT COUNT(tr.id_user) FROM trip_registrations as tr"
          . "         WHERE tr.id_trip = '" . mysql_real_escape_string($trip_id)
          . "'         ));");
    } else {
      mysql_query("DELETE FROM trip_registrations"
          . " WHERE id_user = '" . mysql_real_escape_string($user_id) . "'"
          . "     AND id_trip = '" . mysql_real_escape_string($trip_id) . "';");
    }

    // Return results
    echo generate_result(array(), $user_id);
    break;

  // ===========================================================================
  //   REFRESH ACTION
  // ===========================================================================
  case ACTION_REFRESH:
    // Retrieve user ID
    if (!array_key_exists(USER_ID_KEY, $_REQUEST)) {
      die(generate_error(ERR_INTERNAL)); // Missing user ID
    }
    $user_id = $_REQUEST[USER_ID_KEY];

    // Connect to the database
    connect_database();

    // Return results
    echo generate_result(array(), $user_id);
    break;

  default:
    die(generate_error(ERR_INTERNAL)); // Invalid action
}

// Disconnect from the database
mysql_close();

?>