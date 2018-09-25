package cz.cvut.isc.tripregistrator.dialog

import android.app.Dialog
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import cz.cvut.isc.tripregistrator.PreferenceInteractor
import cz.cvut.isc.tripregistrator.R
import cz.cvut.isc.tripregistrator.api.ApiInteractor
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.settings_dialog.view.*

/**
 * Settings dialog in which user can change server url, username and password and test the
 * connection
 *
 * @author David Khol
 * @since 26.08.2018
 */
class SettingsDialog : DialogFragment() {

	private lateinit var preferences: PreferenceInteractor
	private lateinit var apiInteractor: ApiInteractor

	private val previousUrl by lazy { preferences.url }
	private val previousUsername by lazy { preferences.username }
	private val previousPassword by lazy { preferences.password }

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		preferences = PreferenceInteractor(context!!)
		apiInteractor = ApiInteractor(preferences)
	}

	private fun flushPreferences(view: View) {
		preferences.url = view.settings_url.text.toString()
		preferences.username = view.settings_username.text.toString()
		preferences.password = view.settings_password.text.toString()
	}

	override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
		return AlertDialog.Builder(context!!).apply {
			setView(LayoutInflater.from(context).inflate(R.layout.settings_dialog, null, false).apply {
				settings_url.setText(previousUrl)
				settings_username.setText(previousUsername)
				settings_password.setText(previousPassword)
				settings_test_button.setOnClickListener {
					flushPreferences(this)
					apiInteractor.ping()
							.subscribeOn(Schedulers.io())
							.observeOn(AndroidSchedulers.mainThread())
							.doOnSubscribe {
								settings_test_result.visibility = View.GONE
								settings_test_button.visibility = View.INVISIBLE
								settings_test_progress.visibility = View.VISIBLE
							}
							.doAfterTerminate {
								settings_test_result.visibility = View.VISIBLE
								settings_test_button.visibility = View.VISIBLE
								settings_test_progress.visibility = View.INVISIBLE
							}
							.subscribe({
								settings_test_result.setTextColor(ContextCompat.getColor(context, R.color.green))
								settings_test_result.text = context.getString(R.string.settings_dialog_success)
								settings_test_button.isEnabled = false
								postDelayed({
									val callback = activity as? Callback ?: targetFragment as? Callback
									callback?.onSuccessfulConnection()
									dismiss()
								}, 1000)
							}, { t ->
								t.printStackTrace()
								settings_test_result.setTextColor(ContextCompat.getColor(context, R.color.red))
								settings_test_result.text = t.localizedMessage
							})
				}
			})
		}.create().apply {
			setCancelable(true)
		}
	}

	interface Callback {
		fun onSuccessfulConnection()
	}

}
