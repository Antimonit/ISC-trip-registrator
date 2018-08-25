package cz.cvut.isctripregistrator;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.isctripregistrator.R;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class MainActivity extends AppCompatActivity {

    public static final String PREFERENCES = "ISCPreferences";
    public static final String PREFERENCES_QUERY_URL_KEY = "query_url";
    public static final String PREFERENCES_USERNAME_KEY = "username";
    public static final String PREFERENCES_PASSWORD_KEY = "password";
    public static final String DEFAULT_QUERY_URL = "https://192.168.0.200:5555";
    public static final String DEFAULT_USERNAME = "ISC_test";
    public static final String DEFAULT_PASSWORD = "ISC_password";

    public static final String QUERY_USERNAME = "username";
    public static final String QUERY_PASSWORD = "password";
    public static final String QUERY_ACTION = "action";
    public static final String QUERY_ACTION_LOAD = "load";
    public static final String QUERY_ACTION_REGISTER = "register";
    public static final String QUERY_ACTION_UNREGISTER = "unregister";
    public static final String QUERY_ACTION_REFRESH = "refresh";
    public static final String QUERY_CARD_NUMBER = "card_number";
    public static final String QUERY_USER_ID = "user_id";
    public static final String QUERY_TRIP_ID = "trip_id";

    public static final String FIRST_NAME_KEY = "FIRST_NAME";
    public static final String LAST_NAME_KEY = "LAST_NAME";
    public static final String CARD_NUMBER_KEY = "CARD_NUMBER";
    public static final String SEX_KEY = "SEX";
    public static final String FACULTY_KEY = "FACULTY";

    public static final String STATUS_JSON_KEY = "status";
    public static final String DATA_JSON_KEY = "data";
    public static final String TRIPS_JSON_KEY = "trips";
    public static final String TYPE_JSON_KEY = "type";
    public static final String USER_ID_JSON_KEY = "id_user";
    public static final String FIRST_NAME_JSON_KEY = "first_name";
    public static final String LAST_NAME_JSON_KEY = "last_name";
    public static final String SEX_JSON_KEY = "sex";
    public static final String FACULTY_JSON_KEY = "faculty";
    public static final String TRIP_ID_JSON_KEY = "id_trip";
    public static final String TRIP_NAME_JSON_KEY = "trip_name";
    public static final String TRIP_PRICE_JSON_KEY = "trip_price";
    public static final String TRIP_DATE_FROM_JSON_KEY = "trip_date_from";
    public static final String TRIP_DATE_TO_JSON_KEY = "trip_date_to";
    public static final String TRIP_CAPACITY_JSON_KEY = "trip_capacity";
    public static final String TRIP_PARTICIPANTS_JSON_KEY = "trip_participants";
    public static final String REGISTERED_JSON_KEY = "registered";

    public static final String STATUS_JSON_VALUE_SUCCESS = "success";
    public static final String STATUS_JSON_VALUE_ERROR = "error";
    public static final String SEX_JSON_VALUE_MALE = "M";
    public static final String REGISTERED_JSON_VALUE_YES = "y";
    public static final String JSON_VALUE_NULL = "null";
    
    public static final String MYSQL_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String OUTPUT_DATE_FORMAT = "dd MMM";
    public static final String OUTPUT_TIME_FORMAT = "HH:mm";

    private SharedPreferences preferences;
    private ProgressDialog queryDialog;
    private QueryTask queryTask;

    private String cardNumber;
    private String userId, firstName, lastName;
    private String tripId;

    private TableLayout studentDataTable;
    private TableLayout tripsTable;
    private ImageButton refreshButton;

    private Map<String, TextView> studentDataFields;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferences = getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);

        // Student data table
        studentDataTable = (TableLayout) findViewById(R.id.table_student_data);
        studentDataFields = new HashMap<String, TextView>();
        addStudentDataRow(FIRST_NAME_KEY, R.string.label_first_name);
        addStudentDataRow(LAST_NAME_KEY, R.string.label_last_name);
        addStudentDataRow(CARD_NUMBER_KEY, R.string.label_card_number);
        addStudentDataRow(SEX_KEY, R.string.label_sex);
        addStudentDataRow(FACULTY_KEY, R.string.label_faculty);

        // Trips table
        tripsTable = (TableLayout) findViewById(R.id.table_trips);

        // Refresh button
        refreshButton = (ImageButton) findViewById(R.id.refresh_button);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshTrips();
            }
        });

        performActionClear();
    }
    
    @SuppressLint("RtlHardcoded")
    private void addStudentDataRow(String key, int labelId) {
        TableRow row = new TableRow(this);
        row.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        studentDataTable.addView(row);

        TextView label = new TextView(this);
        label.setGravity(Gravity.RIGHT);
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        label.setText(getString(labelId) + ":");
        row.addView(label);

        TableRow.LayoutParams params = new TableRow.LayoutParams();
        params.rightMargin = (int) (getResources().getDisplayMetrics().density * 8);
        params.bottomMargin = (int) (getResources().getDisplayMetrics().density * 2);
        label.setLayoutParams(params);
        
        TextView value = new TextView(this);
        value.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        value.setTypeface(null, Typeface.BOLD);
        studentDataFields.put(key, value);
        row.addView(value);        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_scan:
				performActionScan();
				return true;
			case R.id.action_manual_entry:
				performActionManualEntry();
				return true;
			case R.id.action_clear:
				performActionClear();
				return true;
			case R.id.action_settings:
				performActionSettings();
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (result == null || result.getContents() == null) {
            Toast.makeText(getApplicationContext(), "No scan data received!", Toast.LENGTH_SHORT).show();
        } else {
            loadCardNumber(result.getContents());
        }
    }

    private void performActionScan() {
        IntentIntegrator scanIntegrator = new IntentIntegrator(this);
        scanIntegrator.initiateScan();
    }
    
    private void performActionManualEntry() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.entry_dialog_title);
        builder.setMessage(R.string.entry_dialog_message);

        final EditText input = new EditText(this);
        builder.setView(input);

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                loadCardNumber(input.getText().toString());
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing
            }
        });

        final AlertDialog dialog = builder.create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        dialog.show();
    }

    private void performActionClear() {
        setCard(null);
        setUser(null, null, null);
        setTrip(null);

        for (TextView view : studentDataFields.values()) {
            view.setText(R.string.default_string);
        }
        tripsTable.removeAllViews();
        refreshButton.setVisibility(View.INVISIBLE);
    }

    @SuppressLint("InflateParams")
    private void performActionSettings() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.settings_dialog_title);
        View layout = getLayoutInflater().inflate(R.layout.settings_dialog, null, false);
        builder.setView(layout);

        final EditText inputUrl = (EditText) layout.findViewById(R.id.settings_dialog_url);
        final String previousUrl = preferences.getString(PREFERENCES_QUERY_URL_KEY, DEFAULT_QUERY_URL);
        inputUrl.setText(previousUrl);

        final EditText inputUsername = (EditText) layout.findViewById(R.id.settings_dialog_username);
        final String previousUsername = preferences.getString(PREFERENCES_USERNAME_KEY, DEFAULT_USERNAME);
        inputUsername.setText(previousUsername);

        final EditText inputPassword = (EditText) layout.findViewById(R.id.settings_dialog_password);

        final CheckBox checkBoxShowPassword = (CheckBox) layout.findViewById(R.id.settings_dialog_show_password);
        checkBoxShowPassword.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                int selectionStart = inputPassword.getSelectionStart();
                int selectionEnd = inputPassword.getSelectionEnd();
                inputPassword.setTransformationMethod(isChecked
						? HideReturnsTransformationMethod.getInstance()
						: PasswordTransformationMethod.getInstance());
                inputPassword.setSelection(selectionStart, selectionEnd);
            }
        });

        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String newUrl = inputUrl.getText().toString();
                final String newUsername = inputUsername.getText().toString();
                final String newPassword = inputPassword.getText().toString();

                if (!newUrl.equals(previousUrl) || !newUsername.equals(previousUsername) || !newPassword.equals("")) {
                    Editor editor = preferences.edit();
                    editor.putString(PREFERENCES_QUERY_URL_KEY, newUrl);
                    editor.putString(PREFERENCES_USERNAME_KEY, newUsername);
                    if (!newPassword.equals("")) {
                        editor.putString(PREFERENCES_PASSWORD_KEY, newPassword);
                    }
                    editor.commit();
                    performActionClear();
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing
            }
        });
        builder.show();
    }

    private void loadCardNumber(final String queryCardNumber) {
        setCard(queryCardNumber);

        // Set up progress dialog
        queryDialog = new ProgressDialog(this);
        queryDialog.setTitle(R.string.load_dialog_title);
        queryDialog.setMessage(getString(R.string.load_dialog_message) + " " + queryCardNumber);
        queryDialog.setCancelable(true);
        queryDialog.setCanceledOnTouchOutside(false);
        queryDialog.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (queryTask != null) {
                    queryTask.cancel(true);
                    queryTask = null;
                    queryDialog = null;
                    setCard(null);
                }
            }
        });
        queryDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        queryDialog.show();

        // Set up query task
        queryTask = new QueryTask(preferences.getString(PREFERENCES_QUERY_URL_KEY, DEFAULT_QUERY_URL),
				new Function<JSONObject>() {
					@Override
					public void execute(JSONObject param) {
						if (cardNumber == queryCardNumber && queryDialog != null) {
							queryDialog.dismiss();
							queryTask = null;
							queryDialog = null;

							try {
								if (param.getString(STATUS_JSON_KEY).equals(STATUS_JSON_VALUE_SUCCESS)) {
									populateData(param);
									populateTrips(param);
									refreshButton.setVisibility(View.VISIBLE);
								} else {
									showError(param.getString(TYPE_JSON_KEY));
									performActionClear();
								}
							} catch (JSONException e) {
								e.printStackTrace();
								performActionClear();
							}
						}
					}
					});
        queryTask.execute(QUERY_USERNAME,
                preferences.getString(PREFERENCES_USERNAME_KEY, DEFAULT_USERNAME), QUERY_PASSWORD,
                preferences.getString(PREFERENCES_PASSWORD_KEY, DEFAULT_PASSWORD), QUERY_ACTION,
                QUERY_ACTION_LOAD, QUERY_CARD_NUMBER, queryCardNumber);
    }

    private void confirmRegisterTrip(final String queryTripId, final String tripName, final boolean register) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(register ? R.string.confirm_register_dialog_title : R.string.confirm_unregister_dialog_title);
        builder.setMessage(getString(R.string.confirm_register_dialog_student) + ": " + firstName
                + " " + lastName + "\n" + getString(R.string.confirm_register_dialog_trip) + ": "
                + tripName);
        builder.setPositiveButton(register ? R.string.confirm_register_dialog_yes
                : R.string.confirm_unregister_dialog_yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                registerTrip(queryTripId, tripName, register);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Do nothing.
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    private void registerTrip(final String queryTripId, String tripName, boolean register) {
        setTrip(queryTripId);

        // Set up progress dialog
        queryDialog = new ProgressDialog(this);
        queryDialog.setTitle(register ? R.string.register_dialog_title : R.string.unregister_dialog_title);
        queryDialog.setMessage(getString(R.string.register_dialog_message) + " " + tripName);
        queryDialog.setCancelable(true);
        queryDialog.setCanceledOnTouchOutside(false);
        queryDialog.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (queryTask != null) {
                    queryTask.cancel(true);
                    queryTask = null;
                    queryDialog = null;
                    setTrip(null);
                }
            }
        });
        queryDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        queryDialog.show();

        // Set up query task
        queryTask = new QueryTask(preferences.getString(PREFERENCES_QUERY_URL_KEY, DEFAULT_QUERY_URL),
				new Function<JSONObject>() {
					@Override
					public void execute(JSONObject param) {
						if (tripId == queryTripId && queryDialog != null) {
							queryDialog.dismiss();
							queryTask = null;
							queryDialog = null;
							setTrip(null);

							try {
								if (param.getString(STATUS_JSON_KEY).equals(STATUS_JSON_VALUE_SUCCESS)) {
									populateTrips(param);
								} else {
									showError(param.getString(TYPE_JSON_KEY));
								}
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
					}
				});
        queryTask.execute(QUERY_USERNAME,
                preferences.getString(PREFERENCES_USERNAME_KEY, DEFAULT_USERNAME), QUERY_PASSWORD,
                preferences.getString(PREFERENCES_PASSWORD_KEY, DEFAULT_PASSWORD), QUERY_ACTION,
                register ? QUERY_ACTION_REGISTER : QUERY_ACTION_UNREGISTER,
                QUERY_USER_ID, userId,
                QUERY_TRIP_ID, queryTripId);
    }

    private void refreshTrips() {
        if (userId == null) {
            return;
        }

        // Set up progress dialog
        queryDialog = new ProgressDialog(this);
        queryDialog.setTitle(R.string.refresh_dialog_title);
        queryDialog.setMessage(getString(R.string.refresh_dialog_message));
        queryDialog.setCancelable(true);
        queryDialog.setCanceledOnTouchOutside(false);
        queryDialog.setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (queryTask != null) {
                    queryTask.cancel(true);
                    queryTask = null;
                    queryDialog = null;
                }
            }
        });
        queryDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        queryDialog.show();

        // Set up query task
        queryTask = new QueryTask(preferences.getString(PREFERENCES_QUERY_URL_KEY, DEFAULT_QUERY_URL),
				new Function<JSONObject>() {
					@Override
					public void execute(JSONObject param) {
						if (queryDialog != null) {
							queryDialog.dismiss();
							queryTask = null;
							queryDialog = null;

							try {
								if (param.getString(STATUS_JSON_KEY).equals(STATUS_JSON_VALUE_SUCCESS)) {
									populateTrips(param);
								} else {
									showError(param.getString(TYPE_JSON_KEY));
								}
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
					}
				});
        queryTask.execute(QUERY_USERNAME,
                preferences.getString(PREFERENCES_USERNAME_KEY, DEFAULT_USERNAME), QUERY_PASSWORD,
                preferences.getString(PREFERENCES_PASSWORD_KEY, DEFAULT_PASSWORD), QUERY_ACTION,
                QUERY_ACTION_REFRESH, QUERY_USER_ID, userId);
    }

    private void populateData(JSONObject response) throws JSONException {
        JSONObject data = response.getJSONObject(DATA_JSON_KEY);
        studentDataFields.get(FIRST_NAME_KEY).setText(data.getString(FIRST_NAME_JSON_KEY));
        studentDataFields.get(LAST_NAME_KEY).setText(data.getString(LAST_NAME_JSON_KEY));
        studentDataFields.get(CARD_NUMBER_KEY).setText(cardNumber);
        String sex = data.getString(SEX_JSON_KEY);
        studentDataFields.get(SEX_KEY).setText(sex.equals(JSON_VALUE_NULL) ? R.string.unknown : (sex.equals(SEX_JSON_VALUE_MALE) ? R.string.male : R.string.female));
        studentDataFields.get(FACULTY_KEY).setText(data.getString(FACULTY_JSON_KEY));
        setUser(data.getString(USER_ID_JSON_KEY),
				data.getString(FIRST_NAME_JSON_KEY),
                data.getString(LAST_NAME_JSON_KEY));
    }

    private void populateTrips(JSONObject response) throws JSONException {
        JSONArray trips = response.getJSONArray(TRIPS_JSON_KEY);
        tripsTable.removeAllViews();

        for (int i = 0; i < trips.length(); i++) {
            JSONObject trip = trips.getJSONObject(i);

            TableRow row = new TableRow(this);
            TableLayout.LayoutParams rowParams =
                    new TableLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                            LayoutParams.WRAP_CONTENT);
            rowParams.bottomMargin = (int) (getResources().getDisplayMetrics().density * 5);
            int padding = (int) (getResources().getDisplayMetrics().density * 5);
            row.setPadding(padding, padding, padding, padding);
            row.setLayoutParams(rowParams);
            row.setBackgroundColor(getResources().getColor(R.color.trip_background));
            tripsTable.addView(row);

            // Left linear layout
            LinearLayout leftLayout = new LinearLayout(this);
            leftLayout.setOrientation(LinearLayout.VERTICAL);
            leftLayout.setLayoutParams(new TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 1));
            leftLayout.setPadding(0, 0, padding, 0);
            row.addView(leftLayout);

            TextView name = new TextView(this);
            final String tripName = trip.getString(TRIP_NAME_JSON_KEY);
            name.setText(tripName);
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
            name.setTypeface(null, Typeface.BOLD);
            leftLayout.addView(name);

            TextView date = new TextView(this);
            date.setText(buildDateString(trip.getString(TRIP_DATE_FROM_JSON_KEY),
                    trip.getString(TRIP_DATE_TO_JSON_KEY)));
            date.setTypeface(null, Typeface.ITALIC);
            leftLayout.addView(date);

            TextView price = new TextView(this);
            price.setText(trip.getString(TRIP_PRICE_JSON_KEY) + " Kč");
            leftLayout.addView(price);

            // Right linear layout
            LinearLayout rightLayout = new LinearLayout(this);
            rightLayout.setOrientation(LinearLayout.VERTICAL);
            rightLayout.setLayoutParams(new TableRow.LayoutParams(
                    TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT));
            row.addView(rightLayout);

            Button toggle = new Button(this);
            toggle.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT));
            toggle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            final boolean isRegistered =
                    trip.getString(REGISTERED_JSON_KEY).equals(REGISTERED_JSON_VALUE_YES);
            boolean isFull = false;
            try {
                isFull = !trip.getString(TRIP_CAPACITY_JSON_KEY).equals(JSON_VALUE_NULL)
                        && Integer.parseInt(trip.getString(TRIP_CAPACITY_JSON_KEY)) <= Integer
                        .parseInt(trip.getString(TRIP_PARTICIPANTS_JSON_KEY));
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            toggle.setText(getString(isRegistered ? R.string.registered
                    : (isFull ? R.string.full : R.string.not_registered)));
            toggle.setBackgroundResource(isRegistered ? R.drawable.button_registered
                    : (isFull ? R.drawable.button_full : R.drawable.button_not_registered));
            final String tripId = trip.getString(TRIP_ID_JSON_KEY);
            toggle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmRegisterTrip(tripId, tripName, !isRegistered);
                }
            });
            rightLayout.addView(toggle);

            TextView capacity = new TextView(this);
            capacity.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.WRAP_CONTENT));
            capacity.setGravity(Gravity.CENTER);
            capacity.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            capacity.setText(trip.getString(TRIP_PARTICIPANTS_JSON_KEY)
                    + "/"
                    + (trip.getString(TRIP_CAPACITY_JSON_KEY).equals(JSON_VALUE_NULL)
                            ? getString(R.string.unlimited)
                            : trip.getString(TRIP_CAPACITY_JSON_KEY)));
            rightLayout.addView(capacity);
        }
    }

    private String buildDateString(String dateFromString, String dateToString) {
        Date dateFrom = parseDate(dateFromString);
        Date dateTo = parseDate(dateToString);

        if (dateFrom == null) {
            return "Unknown date";
        } else {
            String dateFromDateString = DateUtils.formatDate(dateFrom, OUTPUT_DATE_FORMAT);
            String dateFromTimeString = DateUtils.formatDate(dateFrom, OUTPUT_TIME_FORMAT);

            if (dateTo != null) {
                String dateToDateString = DateUtils.formatDate(dateTo, OUTPUT_DATE_FORMAT);
                String dateToTimeString = DateUtils.formatDate(dateTo, OUTPUT_TIME_FORMAT);

                if (dateFromDateString.equals(dateToDateString)) {
                    return dateFromDateString + " " + dateFromTimeString + "-" + dateToTimeString;
                } else {
                    return dateFromDateString + " " + dateFromTimeString + " - " + dateToDateString
                            + " " + dateToTimeString;
                }
            } else {
                return dateFromDateString + " " + dateFromTimeString;
            }
        }
    }

    private Date parseDate(String dateString) {
        try {
            return DateUtils.parseDate(dateString, new String[] { MYSQL_DATE_FORMAT });
        } catch (DateParseException e) {
            return null;        
        }
    }

    private void showError(String type) {
        Toast.makeText(getApplicationContext(), QueryError.getError(type).getMessageResourceId(),
                Toast.LENGTH_LONG).show();
    }

    private void setCard(String queryCardNumber) {
        cardNumber = queryCardNumber;
    }

    private void setUser(String queryUserId, String queryFirstName, String queryLastName) {
        userId = queryUserId;
        firstName = queryFirstName;
        lastName = queryLastName;
    }
    
    private void setTrip(String queryTripId) {
        tripId = queryTripId;
    }
}
