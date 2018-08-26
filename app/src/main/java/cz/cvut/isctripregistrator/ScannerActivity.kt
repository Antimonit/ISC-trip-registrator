package cz.cvut.isctripregistrator

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import me.dm7.barcodescanner.zbar.Result
import me.dm7.barcodescanner.zbar.ZBarScannerView

/**
 * @author David Khol
 * @since 26.08.2018
 **/
class ScannerActivity : AppCompatActivity(), ZBarScannerView.ResultHandler {

	companion object {
		const val KEY_CODE = "code"
	}

	private lateinit var mScannerView: ZBarScannerView

	override fun onCreate(state: Bundle?) {
		super.onCreate(state)
		// TODO: handle permission
		mScannerView = ZBarScannerView(this)
		setContentView(mScannerView)
	}

	override fun onResume() {
		super.onResume()
		mScannerView.setResultHandler(this)
		mScannerView.startCamera()
	}

	@Override
	override fun onPause() {
		super.onPause()
		mScannerView.stopCamera()
	}

	@Override
	override fun handleResult(rawResult: Result) {
		setResult(RESULT_OK, Intent().apply {
			putExtra(KEY_CODE, rawResult.contents)
		})
		finish()
	}

}
