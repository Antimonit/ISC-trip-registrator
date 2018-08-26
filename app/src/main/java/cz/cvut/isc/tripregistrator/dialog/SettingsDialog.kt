package cz.cvut.isc.tripregistrator.dialog

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AlertDialog
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.widget.CheckBox
import cz.cvut.isc.tripregistrator.PreferenceInteractor
import cz.cvut.isc.tripregistrator.R
import kotlinx.android.synthetic.main.settings_dialog.view.*

/**
 * @author David Khol
 * @since 26.08.2018
 */
class SettingsDialog : DialogFragment() {

	private lateinit var preferences: PreferenceInteractor
	private val previousUrl by lazy { preferences.url }
	private val previousUsername by lazy { preferences.username }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		preferences = PreferenceInteractor(context!!)
	}

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		return AlertDialog.Builder(context!!).apply {
			val view = LayoutInflater.from(context).inflate(R.layout.settings_dialog, null, false).apply {
				settings_dialog_url.setText(previousUrl)
				settings_dialog_username.setText(previousUsername)

				findViewById<CheckBox>(R.id.settings_dialog_show_password).apply {
					setOnCheckedChangeListener { _, isChecked ->
						val selectionStart = settings_dialog_password.selectionStart
						val selectionEnd = settings_dialog_password.selectionEnd
						settings_dialog_password.transformationMethod = if (isChecked) {
							HideReturnsTransformationMethod.getInstance()
						} else {
							PasswordTransformationMethod.getInstance()
						}
						settings_dialog_password.setSelection(selectionStart, selectionEnd)
					}
				}
			}
			setView(view)
			setPositiveButton(android.R.string.ok) { _, _ ->
				val newUrl = view.settings_dialog_url.text.toString()
				val newUsername = view.settings_dialog_username.text.toString()
				val newPassword = view.settings_dialog_password.text.toString()

				if (newUrl != previousUrl || newUsername != previousUsername || newPassword != "") {
					preferences.url = newUrl
					preferences.username = newUsername
					if (newPassword != "") {
						preferences.password = newPassword
					}
				}
			}
			setNegativeButton(android.R.string.cancel, null)
		}.create().apply {
			setCancelable(true)
			setCanceledOnTouchOutside(false)
		}
	}

}
