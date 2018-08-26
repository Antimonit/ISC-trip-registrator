package cz.cvut.isctripregistrator

import java.util.Date

import org.apache.http.impl.cookie.DateUtils
import org.json.JSONException

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.view.WindowManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast

import com.google.zxing.integration.android.IntentIntegrator
import cz.cvut.isctripregistrator.api.ApiInteractor
import cz.cvut.isctripregistrator.model.Student
import cz.cvut.isctripregistrator.model.Trip
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class MainActivity : AppCompatActivity() {

	companion object {

		val QUERY_USERNAME = "username"
		val QUERY_PASSWORD = "password"

		val QUERY_ACTION = "action"
		val QUERY_ACTION_LOAD = "load"
		val QUERY_ACTION_REGISTER = "register"
		val QUERY_ACTION_UNREGISTER = "unregister"
		val QUERY_ACTION_REFRESH = "refresh"

		val QUERY_CARD_NUMBER = "card_number"
		val QUERY_USER_ID = "user_id"
		val QUERY_TRIP_ID = "trip_id"

		val FIRST_NAME_KEY = "FIRST_NAME"
		val LAST_NAME_KEY = "LAST_NAME"
		val CARD_NUMBER_KEY = "CARD_NUMBER"
		val SEX_KEY = "SEX"
		val FACULTY_KEY = "FACULTY"

		val STATUS_JSON_KEY = "status"
		val TYPE_JSON_KEY = "type"

		val STATUS_JSON_VALUE_SUCCESS = "success"
		val STATUS_JSON_VALUE_ERROR = "error"

		val MYSQL_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"
		val OUTPUT_DATE_FORMAT = "dd MMM"
		val OUTPUT_TIME_FORMAT = "HH:mm"
	}

	private lateinit var preferences: PreferenceInteractor

	private var queryDialog: ProgressDialog? = null

	private var cardNumber: String? = null
	private var userId: String? = null
	private var firstName: String? = null
	private var lastName: String? = null
	private var tripId: String? = null

	private lateinit var tripsTable: TableLayout
	private lateinit var refreshButton: ImageButton

	private val studentDataFields: MutableMap<String, TextView> = hashMapOf()

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		preferences = PreferenceInteractor(this)

		// Student data table
		findViewById<TableLayout>(R.id.table_student_data).apply {
			addStudentDataRow(FIRST_NAME_KEY, R.string.label_first_name)
			addStudentDataRow(LAST_NAME_KEY, R.string.label_last_name)
			addStudentDataRow(CARD_NUMBER_KEY, R.string.label_card_number)
			addStudentDataRow(SEX_KEY, R.string.label_sex)
			addStudentDataRow(FACULTY_KEY, R.string.label_faculty)
		}

		// Trips table
		tripsTable = findViewById(R.id.table_trips)

		// Refresh button
		refreshButton = findViewById(R.id.refresh_button)
		refreshButton.setOnClickListener { refreshTrips() }

		performActionClear()
	}

	@SuppressLint("RtlHardcoded")
	private fun TableLayout.addStudentDataRow(key: String, labelId: Int) {
		addView(TableRow(context).apply {
			layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)

			addView(TextView(context).apply {
				gravity = Gravity.RIGHT
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
				text = "${getString(labelId)}:"

				layoutParams = TableRow.LayoutParams().apply {
					rightMargin = (resources.displayMetrics.density * 8).toInt()
					bottomMargin = (resources.displayMetrics.density * 2).toInt()
				}
			})

			addView(TextView(context).apply {
				setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
				setTypeface(null, Typeface.BOLD)

				studentDataFields[key] = this
			})
		})
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.main, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.action_scan -> {
				performActionScan()
				return true
			}
			R.id.action_manual_entry -> {
				performActionManualEntry()
				return true
			}
			R.id.action_clear -> {
				performActionClear()
				return true
			}
			R.id.action_settings -> {
				performActionSettings()
				return true
			}
			else -> return super.onOptionsItemSelected(item)
		}
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent) {
		val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent)
		if (result == null || result.contents == null) {
			Toast.makeText(applicationContext, "No scan data received!", Toast.LENGTH_SHORT).show()
		} else {
			loadCardNumber(result.contents)
		}
	}

	private fun performActionScan() {
		val scanIntegrator = IntentIntegrator(this)
		scanIntegrator.initiateScan()
	}

	private fun performActionManualEntry() {
		AlertDialog.Builder(this).apply {
			setTitle(R.string.entry_dialog_title)
			setMessage(R.string.entry_dialog_message)

			val input = EditText(context)

			setView(input)
			setPositiveButton(android.R.string.ok) { dialog, which ->
				loadCardNumber(input.text.toString())
			}
			setNegativeButton(android.R.string.cancel, null)
		}.create().apply {
			window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
		}.show()
	}

	private fun performActionClear() {
		setCard(null)
		setUser(null, null, null)
		setTrip(null)

		for (view in studentDataFields.values) {
			view.setText(R.string.default_string)
		}
		tripsTable.removeAllViews()
		refreshButton.visibility = View.INVISIBLE
	}

	@SuppressLint("InflateParams")
	private fun performActionSettings() {
		AlertDialog.Builder(this).apply {
			setTitle(R.string.settings_dialog_title)

			lateinit var inputUrl: EditText
			lateinit var inputUsername: EditText
			lateinit var inputPassword: EditText

			val previousUrl = preferences.url
			val previousUsername = preferences.username

			setView(layoutInflater.inflate(R.layout.settings_dialog, null, false).apply {
				inputUrl = findViewById<EditText>(R.id.settings_dialog_url).apply {
					setText(previousUrl)
				}

				inputUsername = findViewById<EditText>(R.id.settings_dialog_username).apply {
					setText(previousUsername)
				}

				inputPassword = findViewById(R.id.settings_dialog_password)

				findViewById<CheckBox>(R.id.settings_dialog_show_password).apply {
					setOnCheckedChangeListener { buttonView, isChecked ->
						val selectionStart = inputPassword.selectionStart
						val selectionEnd = inputPassword.selectionEnd
						inputPassword.transformationMethod = if (isChecked) {
							HideReturnsTransformationMethod.getInstance()
						} else {
							PasswordTransformationMethod.getInstance()
						}
						inputPassword.setSelection(selectionStart, selectionEnd)
					}
				}
			})

			setPositiveButton(android.R.string.ok) { dialog, which ->
				val newUrl = inputUrl.text.toString()
				val newUsername = inputUsername.text.toString()
				val newPassword = inputPassword.text.toString()

				if (newUrl != previousUrl || newUsername != previousUsername || newPassword != "") {
					preferences.url = newUrl
					preferences.username = newUsername
					if (newPassword != "") {
						preferences.password = newPassword
					}
					performActionClear()
				}
			}
			setNegativeButton(android.R.string.cancel, null)
		}.show()
	}

	private fun loadCardNumber(queryCardNumber: String) {
		setCard(queryCardNumber)

		// Set up progress dialog
		queryDialog = ProgressDialog(this).apply {
			setTitle(R.string.load_dialog_title)
			setMessage(getString(R.string.load_dialog_message) + " " + queryCardNumber)
			setCancelable(true)
			setCanceledOnTouchOutside(false)
			setOnCancelListener {
				queryDialog = null
				setCard(null)
			}
			setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel)) { dialog, which ->
				dialog.cancel()
			}
			show()
		}

		ApiInteractor.load(
				preferences.username,
				preferences.password,
				queryCardNumber
		).subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe({ result ->
					Toast.makeText(this@MainActivity, "Success", Toast.LENGTH_SHORT).show()
					if (cardNumber == queryCardNumber && queryDialog != null) {
						queryDialog!!.dismiss()
						queryDialog = null

						populateUser(result.user)
						populateTrips(result.trips)
						refreshButton.visibility = View.VISIBLE
					}
				}, { t ->
					t.printStackTrace()
					performActionClear()
					Toast.makeText(this@MainActivity, "Fail", Toast.LENGTH_SHORT).show()
				})
	}

	private fun confirmRegisterTrip(queryTripId: String, tripName: String, register: Boolean) {
		AlertDialog.Builder(this).apply {
			setTitle(if (register) R.string.confirm_register_dialog_title else R.string.confirm_unregister_dialog_title)
			setMessage(getString(R.string.confirm_register_dialog_student) + ": " + firstName
					+ " " + lastName + "\n" + getString(R.string.confirm_register_dialog_trip) + ": "
					+ tripName)
			setPositiveButton(if (register) {
				R.string.confirm_register_dialog_yes
			} else {
				R.string.confirm_unregister_dialog_yes
			}) { dialog, which ->
				registerTrip(queryTripId, tripName, register)
			}
			setNegativeButton(android.R.string.cancel, null)
		}.create().apply {
			setCancelable(true)
			setCanceledOnTouchOutside(false)
		}.show()
	}

	private fun registerTrip(queryTripId: String, tripName: String, register: Boolean) {
		setTrip(queryTripId)

		// Set up progress dialog
		queryDialog = ProgressDialog(this).apply {
			setTitle(if (register) R.string.register_dialog_title else R.string.unregister_dialog_title)
			setMessage(getString(R.string.register_dialog_message) + " " + tripName)
			setCancelable(true)
			setCanceledOnTouchOutside(false)
			setOnCancelListener {
				queryDialog = null
				setTrip(null)
			}
			setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel)) { dialog, which ->
				dialog.cancel()
			}
			show()
		}

		if (register) {
			ApiInteractor.register(
					preferences.username,
					preferences.password,
					userId!!,
					queryTripId
			)
		} else {
			ApiInteractor.unregister(
					preferences.username,
					preferences.password,
					userId!!,
					queryTripId
			)
		}
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe({ result ->
					Toast.makeText(this@MainActivity, "Success", Toast.LENGTH_SHORT).show()
					if (tripId === queryTripId && queryDialog != null) {
						queryDialog!!.dismiss()
						queryDialog = null
						setTrip(null)

						populateUser(result.user)
						populateTrips(result.trips)
					}
				}, { t ->
					t.printStackTrace()
					performActionClear()
					Toast.makeText(this@MainActivity, "Fail", Toast.LENGTH_SHORT).show()
				})
	}

	private fun refreshTrips() {
		if (userId == null) {
			return
		}

		// Set up progress dialog
		queryDialog = ProgressDialog(this).apply {
			setTitle(R.string.refresh_dialog_title)
			setMessage(getString(R.string.refresh_dialog_message))
			setCancelable(true)
			setCanceledOnTouchOutside(false)
			setOnCancelListener {
				queryDialog = null
			}
			setButton(DialogInterface.BUTTON_NEGATIVE, getString(android.R.string.cancel)) { dialog, which ->
				dialog.cancel()
			}
			show()
		}

		ApiInteractor.refresh(
				preferences.username,
				preferences.password,
				userId!!
		)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe({ result ->
					Toast.makeText(this@MainActivity, "Success", Toast.LENGTH_SHORT).show()
					if (queryDialog != null) {
						queryDialog!!.dismiss()
						queryDialog = null

						populateUser(result.user)
						populateTrips(result.trips)
					}
				}, { t ->
					t.printStackTrace()
					performActionClear()
					Toast.makeText(this@MainActivity, "Fail", Toast.LENGTH_SHORT).show()
				})
	}

	private fun populateUser(student: Student?) {
		if (student == null) {
			return
		}
		studentDataFields[FIRST_NAME_KEY]!!.text = student.first_name
		studentDataFields[LAST_NAME_KEY]!!.text = student.last_name
		studentDataFields[CARD_NUMBER_KEY]!!.text = cardNumber
		studentDataFields[SEX_KEY]!!.setText(student.sex?.text ?: R.string.unknown)
		studentDataFields[FACULTY_KEY]!!.text = student.faculty

		setUser(student.id_user, student.first_name, student.last_name)
	}

	private fun populateTrips(trips: List<Trip>) {
		tripsTable.removeAllViews()

		trips.forEach { trip ->
			val tripName = trip.name
			val tripId = trip.id

			val padding = (resources.displayMetrics.density * 5).toInt()

			tripsTable.addView(TableRow(this).apply {
				setPadding(padding, padding, padding, padding)
				layoutParams = TableLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
					bottomMargin = (resources.displayMetrics.density * 5).toInt()
				}
				setBackgroundColor(ContextCompat.getColor(context, R.color.trip_background))

				// Left linear layout
				addView(LinearLayout(context).apply {
					orientation = LinearLayout.VERTICAL
					layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT, 1f)
					setPadding(0, 0, padding, 0)

					addView(TextView(context).apply {
						text = tripName
						setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
						setTypeface(null, Typeface.BOLD)
					})

					addView(TextView(context).apply {
						text = buildDateString(trip.date_from, trip.date_to)
						setTypeface(null, Typeface.ITALIC)
					})

					addView(TextView(context).apply {
						text = getString(R.string.price_czk, trip.price)
					})
				})

				// Right linear layout
				addView(LinearLayout(context).apply {
					orientation = LinearLayout.VERTICAL
					layoutParams = TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT)

					addView(Button(context).apply {
						layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
						setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)

						val isRegistered = trip.registered
						val isFull = if (trip.capacity == null) {
							false
						} else {
							trip.capacity <= trip.participants
						}

						text = getString(when {
							isRegistered -> R.string.registered
							isFull -> R.string.full
							else -> R.string.not_registered
						})
						setBackgroundResource(when {
							isRegistered -> R.drawable.button_registered
							isFull -> R.drawable.button_full
							else -> R.drawable.button_not_registered
						})

						setOnClickListener { confirmRegisterTrip(tripId, tripName, !isRegistered) }
					})

					addView(TextView(context).apply {
						layoutParams = LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
						gravity = Gravity.CENTER
						setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
						text = context.getString(R.string.capacity,
								trip.participants,
								trip.capacity ?: getString(R.string.unlimited)
						)
					})
				})
			})
		}
	}

	private fun buildDateString(dateFrom: Date?, dateTo: Date?): String {
		if (dateFrom == null) {
			return "Unknown date"
		} else {
			val dateFromDateString = DateUtils.formatDate(dateFrom, OUTPUT_DATE_FORMAT)
			val dateFromTimeString = DateUtils.formatDate(dateFrom, OUTPUT_TIME_FORMAT)

			if (dateTo != null) {
				val dateToDateString = DateUtils.formatDate(dateTo, OUTPUT_DATE_FORMAT)
				val dateToTimeString = DateUtils.formatDate(dateTo, OUTPUT_TIME_FORMAT)

				return if (dateFromDateString == dateToDateString) {
					"$dateFromDateString $dateFromTimeString-$dateToTimeString"
				} else {
					"$dateFromDateString $dateFromTimeString - $dateToDateString $dateToTimeString"
				}
			} else {
				return "$dateFromDateString $dateFromTimeString"
			}
		}
	}

	private fun showError(type: String) {
		Toast.makeText(applicationContext, QueryError.getError(type).messageResourceId, Toast.LENGTH_LONG).show()
	}

	private fun setCard(queryCardNumber: String?) {
		cardNumber = queryCardNumber
	}

	private fun setUser(queryUserId: String?, queryFirstName: String?, queryLastName: String?) {
		userId = queryUserId
		firstName = queryFirstName
		lastName = queryLastName
	}

	private fun setTrip(queryTripId: String?) {
		tripId = queryTripId
	}

}
