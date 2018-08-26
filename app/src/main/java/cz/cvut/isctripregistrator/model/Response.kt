package cz.cvut.isctripregistrator.model

/**
 * @author David Khol
 * @since 25.08.2018
 */
data class Response(
		val status: String,
		val data: Student,
		val trips: List<Trip>
)
