package cz.cvut.isc.tripregistrator.screen.main

import cz.cvut.isc.tripregistrator.App
import cz.cvut.isc.tripregistrator.PreferenceInteractor
import cz.cvut.isc.tripregistrator.api.ApiInteractor
import cz.cvut.isc.tripregistrator.model.Student
import cz.cvut.isc.tripregistrator.model.Trip
import cz.cvut.isc.tripregistrator.screen.base.BaseViewModel
import io.reactivex.Observable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject

/**
 * @author David Khol
 * @since 01.09.2018
 **/
class MainViewModel : BaseViewModel() {

	private var preferences = PreferenceInteractor(App.context)
	private var apiInteractor = ApiInteractor(preferences)

	private val studentState = BehaviorSubject.createDefault<StudentState>(StudentState.Unknown)
	private val tripsState = BehaviorSubject.create<TripsState>()
	private val loadingTripState = BehaviorSubject.createDefault<LoadingTripState>(LoadingTripState.None)

	var student: Student? = null
		private set

	init {
		disposables += observeStudentState().subscribe {
			student = if (it is StudentState.Loaded) { it.student } else { null }
		}
	}

	fun observeStudentState(): Observable<StudentState> = studentState

	fun observeTripsState(): Observable<TripsState> = tripsState

	fun observeLoadingTripState(): Observable<LoadingTripState> = loadingTripState


	fun clearUser() {
		studentState.onNext(StudentState.Unknown)
		tripsState.onNext(TripsState.Cleared)
		student = null
		refreshTrips()
	}

	fun loadCardNumber(queryCardNumber: String) {
		disposables += apiInteractor.load(queryCardNumber)
				.doOnSubscribe {
					studentState.onNext(StudentState.Loading)
				}
				.subscribeOn(Schedulers.io())
				.subscribe({ result ->
					student = result.user
					studentState.onNext(StudentState.Loaded(result.user!!))
					tripsState.onNext(TripsState.Loaded(result.trips))
				}, { t ->
					t.printStackTrace()
					studentState.onNext(StudentState.Error(t.localizedMessage))
				})
	}

	fun refreshTrips() {
		val student = student
		disposables += apiInteractor
				.let {
					if (student == null) {
						it.trips()
					} else {
						it.refresh(student.id).map { it.trips }
					}
				}
				.subscribeOn(Schedulers.io())
				.doOnSubscribe {
					tripsState.onNext(TripsState.Loading)
				}
				.subscribe({ trips ->
					tripsState.onNext(TripsState.Loaded(trips))
				}, { t ->
					t.printStackTrace()
					tripsState.onNext(TripsState.Error(t.localizedMessage))
				})
	}

	fun register(trip: Trip, isRegistered: Boolean) {
		val student = student
		if (student == null) {
			loadingTripState.onNext(LoadingTripState.None)
		} else {
			disposables += apiInteractor
					.let {
						if (isRegistered) {
							it.unregister(student.id, trip.id)
						} else {
							it.register(student.id, trip.id)
						}
					}
					.subscribeOn(Schedulers.io())
					.doOnSubscribe {
						loadingTripState.onNext(LoadingTripState.Loading(trip))
					}
					.doFinally {
						loadingTripState.onNext(LoadingTripState.None)
					}
					.subscribe({ result ->
						tripsState.onNext(TripsState.Loaded(result.trips))
					}, { t ->
						t.printStackTrace()
						loadingTripState.onNext(LoadingTripState.Error(t.localizedMessage))
					})
		}
	}
}

sealed class StudentState {
	object Unknown : StudentState()
	object Loading : StudentState()
	class Loaded(val student: Student): StudentState()
	class Error(val message: String): StudentState()
}

sealed class TripsState {
	object Cleared : TripsState()
	object Loading : TripsState()
	class Loaded(val trips: List<Trip>): TripsState()
	class Error(val message: String): TripsState()
}

sealed class LoadingTripState {
	object None : LoadingTripState()
	class Loading(val loadingTrip: Trip) : LoadingTripState()
	class Error(val message: String): LoadingTripState()
}
