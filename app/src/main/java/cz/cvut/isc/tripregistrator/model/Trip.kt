package cz.cvut.isc.tripregistrator.model

import android.os.Parcelable
import com.squareup.moshi.Json
import kotlinx.android.parcel.Parcelize
import java.util.*

/**
 * @author David Khol
 * @since 25.08.2018
 */
@Parcelize
data class Trip(
		@Json(name = "id_trip")
		val id: String,
		@Json(name = "trip_name")
		val name: String,
		@Json(name = "trip_description")
		val description: String?,
		@Json(name = "trip_organizers")
		val organizers: String?,
		@Json(name = "trip_date_from")
		val dateFrom: Date?,
		@Json(name = "trip_date_to")
		val dateTo: Date?,
		@Json(name = "trip_capacity")
		val capacity: Int?,
		@Json(name = "trip_price")
		val price: Int,
		@Json(name = "trip_participants")
		val participants: Int,
		@Json(name = "registered")
		private val _registered: String
) : Parcelable {

	val isRegistered: Boolean
		get() = _registered == "y"

	val isFull: Boolean
		get() = if (capacity == null) {
			false
		} else {
			capacity <= participants
		}

}
