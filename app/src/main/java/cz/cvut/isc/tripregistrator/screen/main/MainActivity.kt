package cz.cvut.isc.tripregistrator.screen.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.PopupMenu
import cz.cvut.isc.tripregistrator.PreferenceInteractor
import cz.cvut.isc.tripregistrator.R
import cz.cvut.isc.tripregistrator.api.ApiInteractor
import cz.cvut.isc.tripregistrator.dialog.ConfirmationDialog
import cz.cvut.isc.tripregistrator.dialog.ManualEntryDialog
import cz.cvut.isc.tripregistrator.dialog.SettingsDialog
import cz.cvut.isc.tripregistrator.model.Student
import cz.cvut.isc.tripregistrator.model.Trip
import cz.cvut.isc.tripregistrator.screen.scan.ScannerActivity
import cz.cvut.isc.tripregistrator.utils.SpacingDecoration
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.student_card.*

class MainActivity : AppCompatActivity(), ConfirmationDialog.Callback, ManualEntryDialog.Callback {

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

		preferences = PreferenceInteractor(this)
		apiInteractor = ApiInteractor(preferences)

		overflow.setOnClickListener {
			PopupMenu(overflow.context, overflow).apply {
				menuInflater.inflate(R.menu.main, menu)
				setOnMenuItemClickListener { item ->
					when (item.itemId) {
						R.id.action_settings -> {
							performActionSettings()
							true
						}
						else -> super.onOptionsItemSelected(item)
					}
				}
			}.show()
		}

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
		btn_student_clear.setOnClickListener {
			updateUser(null)
		}

		updateUser(null)
		refreshTrips()
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
		ManualEntryDialog().show(supportFragmentManager, "settings")
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
					updateUser(null)
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
					Snackbar.make(content, t.localizedMessage, Snackbar.LENGTH_LONG).show()
				})
	}

	override fun onConfirmation(esnCardNumber: String) {
		loadCardNumber(esnCardNumber)
	}

	private fun refreshTrips() {
		if (student == null) {
			apiInteractor.trips()
		} else {
			apiInteractor.refresh(student!!.id)
					.map { it.trips }
		}
				.subscribeOn(Schedulers.io())
				.observeOn(AndroidSchedulers.mainThread())
				.doOnSubscribe { trips_refresh.isRefreshing = true }
				.doFinally { trips_refresh.isRefreshing = false }
				.subscribe({ trips ->
					updateTrips(trips)
				}, { t ->
					t.printStackTrace()
					updateUser(null)
					updateTrips(emptyList())
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

		if (user == null) {
			showStudentUnknown()
		} else {
			showStudentLoaded()

			txt_student_name.text = user.fullName
			txt_student_esn_card_number.text = user.esnCardNumber
			txt_student_faculty.text = user.faculty

			val resource = user.sex?.drawable
			val drawable = if (resource != null) {
				ContextCompat.getDrawable(this, resource)
			} else {
				null
			}
			img_student_gender.setImageDrawable(drawable)
		}
	}

	private fun updateTrips(trips: List<Trip>) {
		tripsAdapter.trips = trips
	}

}
