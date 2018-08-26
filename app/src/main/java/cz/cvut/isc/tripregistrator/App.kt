package cz.cvut.isc.tripregistrator

import android.app.Application
import com.facebook.stetho.Stetho

/**
 * @author David Khol
 * @since 25.08.2018
 */
class App : Application() {

	override fun onCreate() {
		super.onCreate()

		Stetho.initializeWithDefaults(this)
	}
}
