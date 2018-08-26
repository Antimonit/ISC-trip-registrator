package cz.cvut.isctripregistrator.model

import com.squareup.moshi.Json
import java.util.*

/**
 * @author David Khol
 * @since 25.08.2018
 */
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
		val date_from: Date?,
		@Json(name = "trip_date_to")
		val date_to: Date?,
		@Json(name = "trip_capacity")
		val capacity: Int?,
		@Json(name = "trip_price")
		val price: Int,
		@Json(name = "trip_participants")
		val participants: Int,
		@Json(name = "registered")
		private val _registered: String
) {

	val registered: Boolean
		get() = _registered == "y"

}
