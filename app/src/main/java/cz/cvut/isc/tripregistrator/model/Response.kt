package cz.cvut.isc.tripregistrator.model

/**
 * @author David Khol
 * @since 25.08.2018
 */
data class Response(
		val status: String,
		val user: Student?,
		val trips: List<Trip>
)
