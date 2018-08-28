package cz.cvut.isc.tripregistrator.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.view.ViewAnimationUtils
import android.os.Build
import android.support.annotation.RequiresApi
import android.view.View
import android.view.animation.AccelerateInterpolator

/**
 * @author David Khol [david.khol@ackee.cz]
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun Activity.revealActivity(view: View, x: Int, y: Int) {
	val finalRadius = Math.max(view.width, view.height) * 1.1f

	// make the view visible and start the animation
	view.visibility = View.VISIBLE

	// create the animator for this view (the start radius is zero)
	ViewAnimationUtils.createCircularReveal(view, x, y, 0f, finalRadius).apply {
		duration = 2000
		interpolator = AccelerateInterpolator()

	}.start()

}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
fun Activity.unRevealActivity(view: View, x: Int, y: Int) {
	val finalRadius = Math.max(view.width, view.height) * 1.1f

	ViewAnimationUtils.createCircularReveal(view, x, y, finalRadius, 0f).apply {
		duration = 2000
		addListener(object : AnimatorListenerAdapter() {
			override fun onAnimationEnd(animation: Animator) {
				view.visibility = View.INVISIBLE
				finish()
			}
		})
	}.start()
}
