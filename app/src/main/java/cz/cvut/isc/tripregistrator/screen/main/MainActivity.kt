package cz.cvut.isc.tripregistrator.screen.main

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.*
import cz.cvut.isc.tripregistrator.PreferenceInteractor
import cz.cvut.isc.tripregistrator.R

import cz.cvut.isc.tripregistrator.api.ApiInteractor
import cz.cvut.isc.tripregistrator.dialog.ConfirmationDialog
import cz.cvut.isc.tripregistrator.dialog.SettingsDialog
import cz.cvut.isc.tripregistrator.model.Student
import cz.cvut.isc.tripregistrator.model.Trip
import cz.cvut.isc.tripregistrator.screen.scan.ScannerActivity
import cz.cvut.isc.tripregistrator.utils.SpacingDecoration
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.student_card.*

class MainActivity : AppCompatActivity(), ConfirmationDialog.Callback {

	companion object {
		const val RC_SCANNER = 1
	}

	private lateinit var preferences: PreferenceInteractor

	private var student: Student? = null

	private val tripsAdapter = TripsAdapter(
			registerTrip = { trip ->
				confirmRegisterTrip(trip, true)
			},
			unregisterTrip = { trip ->
				confirmRegisterTrip(trip, false)
			}
	)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		setSupportActionBar(toolbar)
		supportActionBar?.apply {
			setDisplayShowTitleEnabled(false)
			setDisplayHomeAsUpEnabled(false)
			setDisplayShowCustomEnabled(false)
		}

		preferences = PreferenceInteractor(this)

		trips.adapter = tripsAdapter
		trips.addItemDecoration(SpacingDecoration(resources.getDimension(R.dimen.word_list_spacing).toInt()))

		trips_refresh.setOnRefreshListener {
			refreshTrips()
		}

		performActionClear()
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

	override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
		if (requestCode == RC_SCANNER) {
			if (resultCode == Activity.RESULT_OK) {
				intent?.getStringExtra(ScannerActivity.KEY_CODE)?.let {
					loadCardNumber(it)
				}
			}
		}
	}


	private fun performActionScan() {
		startActivityForResult(Intent(this, ScannerActivity::class.java), RC_SCANNER)
	}

	private fun performActionManualEntry() {
		AlertDialog.Builder(this).apply {
			setTitle(R.string.entry_dialog_title)
			setMessage(R.string.entry_dialog_message)

			val input = EditText(context)

			setView(input)
			setPositiveButton(android.R.string.ok) { _, _ ->
				loadCardNumber(input.text.toString())
			}
			setNegativeButton(android.R.string.cancel, null)
		}.create().apply {
			window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
		}.show()
	}

	private fun performActionClear() {
		this.student = null

		tripsAdapter.trips = emptyList()
		trips_refresh.isEnabled = false
	}

	private fun performActionSettings() {
		SettingsDialog().show(supportFragmentManager, "settings")
	}


	private fun loadCardNumber(queryCardNumber: String) {
		ApiInteractor.load(
				preferences.username,
				preferences.password,
				queryCardNumber
		).subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.doOnSubscribe {
					progress.visibility = View.VISIBLE
				}
				.doFinally {
					progress.visibility = View.GONE
				}
				.subscribe({ result ->
					updateUser(result.user)
					updateTrips(result.trips)
					Toast.makeText(this@MainActivity, "Success", Toast.LENGTH_SHORT).show()
				}, { t ->
					t.printStackTrace()
					performActionClear()
					Toast.makeText(this@MainActivity, "Fail", Toast.LENGTH_SHORT).show()
				})
	}

	private fun confirmRegisterTrip(trip: Trip, register: Boolean) {
		val message = getString(R.string.confirm_register_dialog_student) + ": " + student?.fullName + "\n" + getString(R.string.confirm_register_dialog_trip) + ": " + trip.name
		if (register) {
			ConfirmationDialog.newInstanceRegister(trip, message).show(supportFragmentManager, "confirmation")
		} else {
			ConfirmationDialog.newInstanceUnregister(trip, message).show(supportFragmentManager, "confirmation")
		}
	}

	override fun onConfirmation(trip: Trip, register: Boolean) {
		if (register) {
			ApiInteractor.register(
					preferences.username,
					preferences.password,
					student!!.id,
					trip.id
			)
		} else {
			ApiInteractor.unregister(
					preferences.username,
					preferences.password,
					student!!.id,
					trip.id
			)
		}
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.doOnSubscribe {
					tripsAdapter.currentlyLoadingTrip = trip
				}
				.doFinally {
					tripsAdapter.currentlyLoadingTrip = null
				}
				.subscribe({ result ->
					updateUser(result.user)
					updateTrips(result.trips)
					Toast.makeText(this@MainActivity, "Success", Toast.LENGTH_SHORT).show()
				}, { t ->
					t.printStackTrace()
					performActionClear()
					Toast.makeText(this@MainActivity, "Fail", Toast.LENGTH_SHORT).show()
				})
	}

	private fun refreshTrips() {
		if (student == null) {
			return
		}

		ApiInteractor.refresh(
				preferences.username,
				preferences.password,
				student!!.id
		)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.doOnSubscribe {
					trips_refresh.isRefreshing = true
				}
				.doFinally {
					trips_refresh.isRefreshing = false
				}
				.subscribe({ result ->
					updateUser(result.user)
					updateTrips(result.trips)
					Toast.makeText(this@MainActivity, "Success", Toast.LENGTH_SHORT).show()
				}, { t ->
					t.printStackTrace()
					performActionClear()
					Toast.makeText(this@MainActivity, "Fail", Toast.LENGTH_SHORT).show()
				})
	}

	private fun updateUser(student: Student?) {
		if (student == null) {
			return
		}
		this.student = student

		txt_name.text = student.fullName
		txt_esn_card_number.text = student.esnCardNumber
		txt_faculty.text = student.faculty

		val resource = student.sex?.drawable
		val drawable = if (resource != null) { ContextCompat.getDrawable(this, resource) } else { null }
		img_gender.setImageDrawable(drawable)
	}

	private fun updateTrips(trips: List<Trip>) {
		tripsAdapter.trips = trips
	}

}
