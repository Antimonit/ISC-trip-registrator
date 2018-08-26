package cz.cvut.isc.tripregistrator.api

import android.annotation.SuppressLint
import com.squareup.moshi.*
import java.text.SimpleDateFormat
import java.util.Date

class DateAdapter {

	companion object {
		@SuppressLint("SimpleDateFormat")
		private val DATE_TIME_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
	}

	@FromJson
	fun fromJson(date: String?): Date? {
		return if (date == null) {
			null
		} else {
			DATE_TIME_FORMAT.parse(date)
		}
	}

	@ToJson
	fun toJson(date: Date?): String? {
		return if (date == null) {
			null
		} else {
			DATE_TIME_FORMAT.format(date)
		}
	}

}
