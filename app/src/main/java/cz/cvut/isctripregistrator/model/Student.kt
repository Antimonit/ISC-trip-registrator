package cz.cvut.isctripregistrator.model

import android.os.Parcelable
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import com.squareup.moshi.Json
import cz.cvut.isctripregistrator.R
import kotlinx.android.parcel.Parcelize

/**
 * @author David Khol
 * @since 25.08.2018
 */
@Parcelize
data class Student(
		@Json(name = "id_user")
		val id: String,
		@Json(name = "first_name")
		val firstName: String,
		@Json(name = "last_name")
		val lastName: String,
		val sex: Sex?,
		@Json(name = "esn_card_number")
		val esnCardNumber: String,
		val faculty: String
): Parcelable {

	val fullName: String
		get() = "$firstName $lastName"

}

enum class Sex(@StringRes val text: Int, @DrawableRes val drawable: Int) {
	M(R.string.male, R.drawable.gender_male),
	F(R.string.female, R.drawable.gender_female),
}
