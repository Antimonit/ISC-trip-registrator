package cz.cvut.isctripregistrator

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import cz.cvut.isctripregistrator.model.Trip

/**
 * @author David Khol
 * @since 26.08.2018
 **/
class ConfirmationDialog : DialogFragment() {

	companion object {

		fun newInstanceRegister(trip: Trip, message: String): ConfirmationDialog {
			return ConfirmationDialog().apply {
				arguments = Bundle().apply {
					putParcelable("trip", trip)
					putInt("title", R.string.confirm_register_dialog_title)
					putString("message", message)
					putInt("positive_button", R.string.confirm_register_dialog_yes)
					putBoolean("register", true)
				}
			}
		}

		fun newInstanceUnregister(trip: Trip, message: String): ConfirmationDialog {
			return ConfirmationDialog().apply {
				arguments = Bundle().apply {
					putParcelable("trip", trip)
					putInt("title", R.string.confirm_unregister_dialog_title)
					putString("message", message)
					putInt("positive_button", R.string.confirm_unregister_dialog_yes)
					putBoolean("register", false)
				}
			}
		}
	}

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		val arguments = arguments ?: throw IllegalArgumentException("Missing arguments")
		return AlertDialog.Builder(context!!).apply {
			val trip = arguments.getParcelable<Trip>("trip")
			val register = arguments.getBoolean("register")

			setTitle(arguments.getInt("title"))
			setMessage(arguments.getString("message"))
			setPositiveButton(arguments.getInt("positive_button")) { dialog, which ->
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
