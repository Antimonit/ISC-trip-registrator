package cz.cvut.isc.tripregistrator.dialog

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import cz.cvut.isc.tripregistrator.R
import cz.cvut.isc.tripregistrator.model.Student
import cz.cvut.isc.tripregistrator.model.Trip

/**
 * @author David Khol
 * @since 26.08.2018
 */
class ConfirmationDialog : DialogFragment() {

	companion object {

		private const val KEY_TRIP = "trip"
		private const val KEY_STUDENT = "student"
		private const val KEY_IS_REGISTERED = "is_registered"

		fun newInstance(trip: Trip, student: Student, isRegistered: Boolean): ConfirmationDialog {
			return ConfirmationDialog().apply {
				arguments = Bundle().apply {
					putParcelable(KEY_TRIP, trip)
					putParcelable(KEY_STUDENT, student)
					putBoolean(KEY_IS_REGISTERED, isRegistered)
				}
			}
		}
	}

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val arguments = arguments ?: throw IllegalArgumentException("Missing arguments")
		return AlertDialog.Builder(context!!).apply {
			val trip = arguments.getParcelable<Trip>(KEY_TRIP)
			val student = arguments.getParcelable<Student>(KEY_STUDENT)
			val isRegistered = arguments.getBoolean(KEY_IS_REGISTERED)

			val studentText = "${getString(R.string.confirm_dialog_student)}: ${student.fullName}"
			val tripText = "${getString(R.string.confirm_dialog_trip)}: ${trip.name}"

			setMessage("$studentText\n$tripText")
			setTitle(if (isRegistered) {
				R.string.confirm_dialog_register_title
			} else {
				R.string.confirm_dialog_unregister_title
			})
			setPositiveButton(if (isRegistered) {
				R.string.confirm_dialog_register_yes
			} else {
				R.string.confirm_dialog_unregister_yes
			}) { _, _ ->
				val callback = activity as? Callback ?: targetFragment as? Callback
				callback?.onConfirmation(trip, isRegistered)
			}
			setNegativeButton(android.R.string.cancel, null)
		}.create().apply {
			setCancelable(true)
			setCanceledOnTouchOutside(false)
		}
	}

	interface Callback {
		fun onConfirmation(trip: Trip, isRegistered: Boolean)
	}

}
