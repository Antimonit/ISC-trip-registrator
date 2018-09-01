package cz.cvut.isc.tripregistrator.api

import com.facebook.stetho.okhttp3.StethoInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import cz.cvut.isc.tripregistrator.PreferenceInteractor
import cz.cvut.isc.tripregistrator.model.Response
import cz.cvut.isc.tripregistrator.model.Trip
import io.reactivex.Completable
import io.reactivex.Single
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.HttpException

/**
 * Sets up several networking libraries such as OkHttp, Moshi and Retrofit in order
 * to build [ApiDescription] that is used to communicate with server.
 *
 * @author David Khol
 * @since 25.08.2018
 */
class ApiInteractor(private val preferences: PreferenceInteractor) {

	private val baseUrl: String
		get() = preferences.url

	private var loadedUrl: String? = null

	private val okHttpClient = OkHttpClient.Builder()
			.addNetworkInterceptor(HttpLoggingInterceptor().apply {
				level = HttpLoggingInterceptor.Level.BODY
			})
			.addNetworkInterceptor(StethoInterceptor())
			.build()

	private val moshi = Moshi.Builder()
			.add(DateAdapter())
			.add(KotlinJsonAdapterFactory())
			.build()

	private val apiErrorAdapter = moshi.adapter(ApiError::class.java)

	private var service: ApiDescription? = null
		get() {
			if (loadedUrl == null || loadedUrl != baseUrl) {
				field = try {
					Retrofit.Builder()
							.client(okHttpClient)
							.baseUrl(baseUrl)
							.addCallAdapterFactory(RxJava2CallAdapterFactory.create())
							.addConverterFactory(MoshiConverterFactory.create(moshi))
							.build()
							.create(ApiDescription::class.java)
							.also { loadedUrl = baseUrl }
				} catch (e: IllegalArgumentException) {
					// URL can be malformed in which case IllegalArgumentException is thrown
					null
				}
			}
			return field
		}


	private fun <T> Single<T>.mapErrors() = onErrorResumeNext {	Single.error<T>(it.mapError()) }

	private fun Completable.mapErrors() = onErrorResumeNext { Completable.error(it.mapError()) }

	private fun Throwable.mapError(): Throwable? {
		if (this is HttpException) {
			response()?.errorBody()?.let {
				return apiErrorAdapter.fromJson(it.string())?.toException()
			}
		}
		return this
	}

	private fun <T> nullService() = Single.error<T>(IllegalArgumentException("Invalid URL"))


	fun load(esnNumber: String): Single<Response> {
		return service?.load(preferences.username, preferences.password, "load", esnNumber)?.mapErrors() ?: nullService()
	}

	fun register(userId: String, tripId: String): Single<Response> {
		return service?.register(preferences.username, preferences.password, "register", userId, tripId)?.mapErrors() ?: nullService()
	}

	fun unregister(userId: String, tripId: String): Single<Response> {
		return service?.register(preferences.username, preferences.password, "unregister", userId, tripId)?.mapErrors() ?: nullService()
	}

	fun refresh(userId: String): Single<Response> {
		return service?.refresh(preferences.username, preferences.password, "refresh", userId)?.mapErrors() ?: nullService()
	}

	fun trips(): Single<List<Trip>> {
		return service?.trips(preferences.username, preferences.password, "trips")?.mapErrors() ?: nullService()
	}

	fun ping(): Completable {
		return service?.ping(preferences.username, preferences.password, "ping")?.mapErrors() ?: nullService<Any>().ignoreElement()
	}

}
