package cz.cvut.isc.tripregistrator.api

/**
 * @author David Khol
 * @since 01.09.2018
 **/
data class ApiError(val status_code: Int, val message: String) {

	fun toException() = Exception(message)

}
