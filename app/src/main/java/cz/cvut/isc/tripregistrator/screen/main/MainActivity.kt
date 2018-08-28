package cz.cvut.isc.tripregistrator.screen.main

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
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
	private lateinit var apiInteractor: ApiInteractor

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
		apiInteractor = ApiInteractor(preferences)

		trips.adapter = tripsAdapter
		trips.addItemDecoration(SpacingDecoration(resources.getDimension(R.dimen.word_list_spacing).toInt()))

		trips_refresh.isEnabled = true
		trips_refresh.setOnRefreshListener {
			refreshTrips()
		}

		btn_scan_barcode.setOnClickListener {
			performActionScan()
		}
		btn_manual_barcode.setOnClickListener {
			performActionManualEntry()
		}
		btn_clear.setOnClickListener {
			performActionClear()
		}

		performActionClear()
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.main, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		return when (item.itemId) {
			R.id.action_settings -> {
				performActionSettings()
				true
			}
			else -> super.onOptionsItemSelected(item)
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
//		val options = ActivityOptionsCompat.makeSceneTransitionAnimation(this, btn_scan_barcode, "transition")
//		val revealX = (btn_scan_barcode.x + btn_scan_barcode.width / 2).toInt()
//		val revealY = (btn_scan_barcode.y + btn_scan_barcode.height / 2).toInt()
//
//		val intent = Intent(this, ScannerActivity::class.java)
//		intent.putExtra(ScannerActivity.EXTRA_CIRCULAR_REVEAL_X, revealX)
//		intent.putExtra(ScannerActivity.EXTRA_CIRCULAR_REVEAL_Y, revealY)
//
//		ActivityCompat.startActivityForResult(this, intent, RC_SCANNER, options.toBundle())

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
		updateUser(null)
		updateTrips(emptyList())
	}

	private fun performActionSettings() {
		SettingsDialog().show(supportFragmentManager, "settings")
	}


	private fun loadCardNumber(queryCardNumber: String) {
		apiInteractor.load(
				queryCardNumber
		).subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.doOnSubscribe {
					showStudentLoading()
				}
				.subscribe({ result ->
					updateUser(result.user)
					updateTrips(result.trips)
				}, { t ->
					t.printStackTrace()
					performActionClear()
					Snackbar.make(content, t.localizedMessage, Snackbar.LENGTH_LONG).show()
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
			apiInteractor.register(student!!.id, trip.id)
		} else {
			apiInteractor.unregister(student!!.id, trip.id)
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
					updateTrips(result.trips)
				}, { t ->
					t.printStackTrace()
					performActionClear()
					Snackbar.make(content, t.localizedMessage, Snackbar.LENGTH_LONG).show()
				})
	}

	private fun refreshTrips() {
		if (student == null) {
			trips_refresh.isRefreshing = false
			return
		}

		apiInteractor.refresh(student!!.id)
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.doOnSubscribe {
					trips_refresh.isRefreshing = true
				}
				.doFinally {
					trips_refresh.isRefreshing = false
				}
				.subscribe({ result ->
					updateTrips(result.trips)
				}, { t ->
					t.printStackTrace()
					performActionClear()
					Snackbar.make(content, t.localizedMessage, Snackbar.LENGTH_LONG).show()
				})
	}

	private fun showStudentLoading() {
		student_loading.show()
		student_loaded_layout.visibility = View.GONE
		student_unknown_layout.visibility = View.GONE
	}

	private fun showStudentUnknown() {
		student_loading.hide()
		student_loaded_layout.visibility = View.GONE
		student_unknown_layout.visibility = View.VISIBLE
	}

	private fun showStudentLoaded() {
		student_loading.hide()
		student_loaded_layout.visibility = View.VISIBLE
		student_unknown_layout.visibility = View.GONE
	}

	private fun updateUser(user: Student?) {
		student = user

		if (user != null) {
			showStudentLoaded()

			txt_name.text = user.fullName
			txt_esn_card_number.text = user.esnCardNumber
			txt_faculty.text = user.faculty

			val resource = user.sex?.drawable
			val drawable = if (resource != null) { ContextCompat.getDrawable(this, resource) } else { null }
			img_gender.setImageDrawable(drawable)
		} else {
			showStudentUnknown()
		}
	}

	private fun updateTrips(trips: List<Trip>) {
		tripsAdapter.trips = trips
	}

}
