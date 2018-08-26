package cz.cvut.isctripregistrator.api

import cz.cvut.isctripregistrator.model.Response
import io.reactivex.Single
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface ApiDescription {

	@FormUrlEncoded
	@POST("query.php")
	fun load(
			@Field("username") username: String,
			@Field("password") password: String,
			@Field("action") action: String,
			@Field("card_number") cardNumber: String
	): Single<Response>

	@FormUrlEncoded
	@POST("query.php")
	fun register(
			@Field("username") username: String,
			@Field("password") password: String,
			@Field("action") action: String,
			@Field("user_id") userId: String,
			@Field("trip_id") tripId: String
	): Single<Response>

	@FormUrlEncoded
	@POST("query.php")
	fun refresh(
			@Field("username") username: String,
			@Field("password") password: String,
			@Field("action") action: String,
			@Field("user_id") userId: String
	): Single<Response>

}
