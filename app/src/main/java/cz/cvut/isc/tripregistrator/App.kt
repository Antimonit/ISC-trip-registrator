package cz.cvut.isc.tripregistrator

import android.app.Application
import com.facebook.stetho.Stetho

/**
 * @author David Khol
 * @since 25.08.2018
 */
class App : Application() {

	companion object {
		lateinit var context: Application
			private set
	}

	override fun onCreate() {
		super.onCreate()
		App.context = this

		Stetho.initializeWithDefaults(this)
	}
}
