package cz.cvut.isc.tripregistrator.screen.base

import android.arch.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable

/**
 * Automatically dispose of RxJava subscriptions/disposables
 *
 * @author David Khol
 * @since 26.09.2018
 **/
open class BaseViewModel : ViewModel() {

	protected val disposables = CompositeDisposable()

	override fun onCleared() {
		super.onCleared()
		disposables.clear()
	}
}
