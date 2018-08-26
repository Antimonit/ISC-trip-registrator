package cz.cvut.isc.tripregistrator.dialog

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import cz.cvut.isc.tripregistrator.R
import cz.cvut.isc.tripregistrator.model.Trip

/**
 * @author David Khol
 * @since 26.08.2018
 */
class ConfirmationDialog : DialogFragment() {

	companion object {

		private const val KEY_TRIP = "trip"
		private const val KEY_TITLE = "title"
		private const val KEY_MESSAGE = "message"
		private const val KEY_POSITIVE_BUTTON = "positive_button"
		private const val KEY_REGISTER = "register"

		fun newInstanceRegister(trip: Trip, message: String): ConfirmationDialog {
			return ConfirmationDialog().apply {
				arguments = Bundle().apply {
					putParcelable(KEY_TRIP, trip)
					putInt(KEY_TITLE, R.string.confirm_register_dialog_title)
					putString(KEY_MESSAGE, message)
					putInt(KEY_POSITIVE_BUTTON, R.string.confirm_register_dialog_yes)
					putBoolean(KEY_REGISTER, true)
				}
			}
		}

		fun newInstanceUnregister(trip: Trip, message: String): ConfirmationDialog {
			return ConfirmationDialog().apply {
				arguments = Bundle().apply {
					putParcelable(KEY_TRIP, trip)
					putInt(KEY_TITLE, R.string.confirm_unregister_dialog_title)
					putString(KEY_MESSAGE, message)
					putInt(KEY_POSITIVE_BUTTON, R.string.confirm_unregister_dialog_yes)
					putBoolean(KEY_REGISTER, false)
				}
			}
		}
	}

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val arguments = arguments ?: throw IllegalArgumentException("Missing arguments")
		return AlertDialog.Builder(context!!).apply {
			val trip = arguments.getParcelable<Trip>(KEY_TRIP)
			val register = arguments.getBoolean(KEY_REGISTER)

			setTitle(arguments.getInt(KEY_TITLE))
			setMessage(arguments.getString(KEY_MESSAGE))
			setPositiveButton(arguments.getInt(KEY_POSITIVE_BUTTON)) { _, _ ->
				val callback = activity as? Callback ?: targetFragment as? Callback
				callback?.onConfirmation(trip, register)
			}
			setNegativeButton(android.R.string.cancel, null)
		}.create().apply {
			setCancelable(true)
			setCanceledOnTouchOutside(false)
		}
	}

	interface Callback {

		fun onConfirmation(trip: Trip, register: Boolean)

	}

}
