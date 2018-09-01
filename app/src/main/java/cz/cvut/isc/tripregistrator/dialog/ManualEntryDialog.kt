package cz.cvut.isc.tripregistrator.dialog

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import cz.cvut.isc.tripregistrator.PreferenceInteractor
import cz.cvut.isc.tripregistrator.R
import cz.cvut.isc.tripregistrator.api.ApiInteractor
import cz.cvut.isc.tripregistrator.model.Trip
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.manual_entry_dialog.view.*
import kotlinx.android.synthetic.main.settings_dialog.view.*
import retrofit2.HttpException

/**
 * @author David Khol
 * @since 26.08.2018
 */
class ManualEntryDialog : DialogFragment() {

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		return AlertDialog.Builder(context!!).apply {
			val view = LayoutInflater.from(context).inflate(R.layout.manual_entry_dialog, null, false)
			setView(view)
			setPositiveButton(android.R.string.ok) { _, _ ->
				val callback = activity as? Callback ?: targetFragment as? Callback
				callback?.onConfirmation(view.esn_card_number.text.toString())
			}
			setNegativeButton(android.R.string.cancel, null)
		}.create().apply {
			window!!.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
			setCancelable(true)
		}
	}

	interface Callback {
		fun onConfirmation(esnCardNumber: String)
	}

}
