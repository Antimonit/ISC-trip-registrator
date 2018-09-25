package cz.cvut.isc.tripregistrator.api

import cz.cvut.isc.tripregistrator.model.Response
import cz.cvut.isc.tripregistrator.model.Trip
import io.reactivex.Completable
import io.reactivex.Single
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

/**
 * Define server endpoints here.
 * When provided to Retrofit it will create a concrete implementation of the interface that
 * we can use to communicate with the server.
 */
interface ApiDescription {

	@FormUrlEncoded
	@POST("api/trips")
	fun ping(
			@Field("username") username: String,
			@Field("password") password: String,
			@Field("action") action: String
	): Completable

	@FormUrlEncoded
	@POST("api/trips")
	fun load(
			@Field("username") username: String,
			@Field("password") password: String,
			@Field("action") action: String,
			@Field("card_number") cardNumber: String
	): Single<Response>

	@FormUrlEncoded
	@POST("api/trips")
	fun register(
			@Field("username") username: String,
			@Field("password") password: String,
			@Field("action") action: String,
			@Field("user_id") userId: String,
			@Field("trip_id") tripId: String
	): Single<Response>

	@FormUrlEncoded
	@POST("api/trips")
	fun refresh(
			@Field("username") username: String,
			@Field("password") password: String,
			@Field("action") action: String,
			@Field("user_id") userId: String
	): Single<Response>

	@FormUrlEncoded
	@POST("api/trips")
	fun trips(
			@Field("username") username: String,
			@Field("password") password: String,
			@Field("action") action: String
	): Single<List<Trip>>

	@FormUrlEncoded
	@POST("api/trips")
	fun test(
			@Field("username") username: String,
			@Field("password") password: String,
			@Field("action") action: String
	): Single<Unit>

}
