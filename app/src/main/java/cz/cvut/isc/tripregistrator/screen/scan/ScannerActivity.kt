package cz.cvut.isc.tripregistrator.screen.scan

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import me.dm7.barcodescanner.zbar.Result
import me.dm7.barcodescanner.zbar.ZBarScannerView
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions

/**
 * @author David Khol
 * @since 26.08.2018
 */
class ScannerActivity : AppCompatActivity(), ZBarScannerView.ResultHandler {

	companion object {
		const val KEY_CODE = "code"
		const val RC_PERMISSION_CAMERA = 1
	}

	private var mScannerView: ZBarScannerView? = null

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		mScannerView = ZBarScannerView(this)
		setContentView(mScannerView)
	}

	@AfterPermissionGranted(RC_PERMISSION_CAMERA)
	private fun requestCameraPermission() {
		if (EasyPermissions.hasPermissions(this, Manifest.permission.CAMERA)) {
			mScannerView?.setResultHandler(this)
			mScannerView?.startCamera()
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
		mScannerView?.stopCamera()
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
