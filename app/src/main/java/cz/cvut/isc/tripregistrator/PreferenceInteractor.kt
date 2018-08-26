package cz.cvut.isc.tripregistrator

import android.content.Context
import android.content.SharedPreferences

/**
 * @author David Khol
 * @since 25.08.2018
 */
class PreferenceInteractor(context: Context) {

	companion object {
		private const val PREFERENCES = "ISCPreferences"
		private const val KEY_URL = "url"
		private const val KEY_USERNAME = "username"
		private const val KEY_PASSWORD = "password"

		private const val DEFAULT_QUERY_URL = "http://192.168.0.200/query.php"
		private const val DEFAULT_USERNAME = "ISC_username"
		private const val DEFAULT_PASSWORD = "ISC_password"
	}

	private val preferences: SharedPreferences

	init {
		preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
	}

	var url: String
		get() = preferences.getString(KEY_URL, DEFAULT_QUERY_URL)
		set(value) = preferences.edit().putString(KEY_URL, value).apply()

	var username: String
		get() = preferences.getString(KEY_USERNAME, DEFAULT_USERNAME)
		set(value) = preferences.edit().putString(KEY_USERNAME, value).apply()

	var password: String
		get() = preferences.getString(KEY_PASSWORD, DEFAULT_PASSWORD)
		set(value) = preferences.edit().putString(KEY_PASSWORD, value).apply()

}
