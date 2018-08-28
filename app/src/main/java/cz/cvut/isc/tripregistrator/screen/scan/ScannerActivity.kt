package cz.cvut.isc.tripregistrator.screen.scan

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.ViewGroup.LayoutParams
import me.dm7.barcodescanner.zbar.Result
import me.dm7.barcodescanner.zbar.ZBarScannerView
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import android.widget.FrameLayout

/**
 * @author David Khol
 * @since 26.08.2018
 */
class ScannerActivity : AppCompatActivity(), ZBarScannerView.ResultHandler {

	companion object {
		const val KEY_CODE = "code"
		const val RC_PERMISSION_CAMERA = 1
		const val EXTRA_CIRCULAR_REVEAL_X = "EXTRA_CIRCULAR_REVEAL_X"
		const val EXTRA_CIRCULAR_REVEAL_Y = "EXTRA_CIRCULAR_REVEAL_Y"
	}

	private lateinit var scannerView: ZBarScannerView
	private lateinit var rootView: FrameLayout

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)

		scannerView = ZBarScannerView(this).apply {
			layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
		}

		rootView = FrameLayout(this).apply {
			setBackgroundColor(Color.DKGRAY)
			addView(scannerView)
		}
		setContentView(rootView)

//		if (state == null &&
//				Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
//				intent.hasExtra(EXTRA_CIRCULAR_REVEAL_X) &&
//				intent.hasExtra(EXTRA_CIRCULAR_REVEAL_Y)) {
//
//			rootView.visibility = View.INVISIBLE
//		} else {
//			rootView.visibility = View.VISIBLE
//		}
	}

	@AfterPermissionGranted(RC_PERMISSION_CAMERA)
	private fun requestCameraPermission() {
		if (EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA)) {
			scannerView.setResultHandler(this)
			scannerView.startCamera()

//			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
//					intent.hasExtra(EXTRA_CIRCULAR_REVEAL_X) &&
//					intent.hasExtra(EXTRA_CIRCULAR_REVEAL_Y)) {
//				val revealX = intent.getIntExtra(EXTRA_CIRCULAR_REVEAL_X, 0)
//				val revealY = intent.getIntExtra(EXTRA_CIRCULAR_REVEAL_Y, 0)
//				rootView.visibility = View.INVISIBLE
//
//				rootView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
//					override fun onGlobalLayout() {
//						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//							revealActivity(rootView, revealX, revealY)
//						}
//						rootView.viewTreeObserver.removeOnGlobalLayoutListener(this)
//					}
//				})
//			} else {
//				rootView.visibility = View.VISIBLE
//			}
		} else {
			EasyPermissions.requestPermissions(this, "App needs camera to scan bar codes.", RC_PERMISSION_CAMERA, Manifest.permission.CAMERA)
		}
	}

	override fun onResume() {
		super.onResume()
		requestCameraPermission()
	}

	override fun onPause() {
		super.onPause()
		scannerView.stopCamera()
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
	}

	override fun handleResult(rawResult: Result) {
		setResult(RESULT_OK, Intent().apply {
			putExtra(KEY_CODE, rawResult.contents)
		})
		finish()
	}

}
