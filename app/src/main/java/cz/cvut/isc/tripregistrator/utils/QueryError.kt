package cz.cvut.isc.tripregistrator.utils

import cz.cvut.isc.tripregistrator.R

enum class QueryError(
		val type: String,
		val messageResourceId: Int
) {
	ERR_AUTHENTICATION("AUTH", R.string.error_authentication),
	ERR_DATABASE("DB", R.string.error_database),
	ERR_INTERNAL("INTERNAL", R.string.error_internal),
	ERR_CARD("CARD", R.string.error_card),
	ERR_CONNECTION("CONNECTION", R.string.error_connection);

}
