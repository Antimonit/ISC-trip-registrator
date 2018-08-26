package cz.cvut.isctripregistrator.model

import com.example.isctripregistrator.R

/**
 * @author David Khol
 * @since 25.08.2018
 */
data class Student(
		val id_user: String,
		val first_name: String,
		val last_name: String,
		val sex: Sex?,
		val faculty: String
)

enum class Sex(val text: Int) {
	M(R.string.male),
	F(R.string.female)
}
