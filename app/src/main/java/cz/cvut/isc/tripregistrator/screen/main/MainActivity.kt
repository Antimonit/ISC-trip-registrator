package cz.cvut.isc.tripregistrator.screen.main

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.PopupMenu
import cz.cvut.isc.tripregistrator.R
import cz.cvut.isc.tripregistrator.dialog.ConfirmationDialog
import cz.cvut.isc.tripregistrator.dialog.ManualEntryDialog
import cz.cvut.isc.tripregistrator.dialog.SettingsDialog
import cz.cvut.isc.tripregistrator.model.Trip
import cz.cvut.isc.tripregistrator.screen.scan.ScannerActivity
import cz.cvut.isc.tripregistrator.utils.SpacingDecoration
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.student_card.*

class MainActivity : AppCompatActivity(), ConfirmationDialog.Callback, ManualEntryDialog.Callback, SettingsDialog.Callback {

	companion object {
		const val RC_SCANNER = 1
	}

	private lateinit var viewModel: MainViewModel
	private lateinit var disposables: CompositeDisposable

	private val tripsAdapter = TripsAdapter(
			tripConfirmation = { trip, isRegistered ->
				viewModel.student?.let {
					ConfirmationDialog.newInstance(trip, it, isRegistered).show(supportFragmentManager, "confirmation")
				}
			}
	)

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		overflow.setOnClickListener {
			PopupMenu(overflow.context, overflow).apply {
				menuInflater.inflate(R.menu.main, menu)
				setOnMenuItemClickListener { item ->
					when (item.itemId) {
						R.id.action_settings -> {
							SettingsDialog().show(supportFragmentManager, "settings")
							true
						}
						else -> super.onOptionsItemSelected(item)
					}
				}
			}.show()
		}

		trips.adapter = tripsAdapter
		trips.addItemDecoration(SpacingDecoration(resources.getDimension(R.dimen.word_list_spacing).toInt()))

		trips_refresh.setOnRefreshListener {
			viewModel.refreshTrips()
		}
		btn_scan_barcode.setOnClickListener {
			startActivityForResult(Intent(this, ScannerActivity::class.java), RC_SCANNER)
		}
		btn_manual_barcode.setOnClickListener {
			ManualEntryDialog().show(supportFragmentManager, "manual_entry")
		}
		btn_student_clear.setOnClickListener {
			viewModel.clearUser()
		}


		viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)

		disposables = CompositeDisposable()

		disposables += viewModel.observeStudentState()
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe {

					fun showStudentLoading() {
						student_loading.show()
						student_loaded_layout.visibility = View.GONE
						student_unknown_layout.visibility = View.GONE
					}

					fun showStudentUnknown() {
						student_loading.hide()
						student_loaded_layout.visibility = View.GONE
						student_unknown_layout.visibility = View.VISIBLE
					}

					fun showStudentLoaded() {
						student_loading.hide()
						student_loaded_layout.visibility = View.VISIBLE
						student_unknown_layout.visibility = View.GONE
					}

					when (it) {
						is StudentState.Unknown -> {
							showStudentUnknown()
						}
						is StudentState.Loading -> {
							showStudentLoading()
						}
						is StudentState.Loaded -> {
							showStudentLoaded()
							val user = it.student
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
						is StudentState.Error -> {
							showStudentUnknown()
							Snackbar.make(content, it.message, Snackbar.LENGTH_LONG).show()
						}
					}
				}

		disposables += viewModel.observeTripsState()
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe {
					when (it) {
						is TripsState.Cleared -> {
							trips_refresh.isRefreshing = false
							tripsAdapter.trips = emptyList()
						}
						is TripsState.Loading -> {
							trips_refresh.isRefreshing = true
						}
						is TripsState.Loaded -> {
							trips_refresh.isRefreshing = false
							tripsAdapter.trips = it.trips
						}
						is TripsState.Error -> {
							trips_refresh.isRefreshing = false
							Snackbar.make(content, it.message, Snackbar.LENGTH_LONG).show()
						}
					}
				}

		disposables += viewModel.observeLoadingTripState()
				.observeOn(AndroidSchedulers.mainThread())
				.subscribe {
					when (it) {
						is LoadingTripState.None -> {
							tripsAdapter.currentlyLoadingTrip = null
						}
						is LoadingTripState.Loading -> {
							tripsAdapter.currentlyLoadingTrip = it.loadingTrip
						}
						is LoadingTripState.Error -> {
							tripsAdapter.currentlyLoadingTrip = null
							Snackbar.make(content, it.message, Snackbar.LENGTH_LONG).show()
						}
					}
				}
	}

	override fun onDestroy() {
		super.onDestroy()
		disposables.clear()
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
		if (requestCode == RC_SCANNER) {
			if (resultCode == Activity.RESULT_OK) {
				intent?.getStringExtra(ScannerActivity.KEY_CODE)?.let {
					viewModel.loadCardNumber(it)
				}
			}
		}
	}

	override fun onConfirmation(trip: Trip, isRegistered: Boolean) {
		viewModel.register(trip, isRegistered)
	}

	override fun onConfirmation(esnCardNumber: String) {
		viewModel.loadCardNumber(esnCardNumber)
	}

	override fun onSuccessfulConnection() {
		viewModel.refreshTrips()
	}

}
