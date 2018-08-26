package cz.cvut.isc.tripregistrator.api

import com.facebook.stetho.okhttp3.StethoInterceptor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import cz.cvut.isc.tripregistrator.model.Response
import io.reactivex.Single
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory

/**
 * @author David Khol
 * @since 25.08.2018
 */

object ApiInteractor {

	private val okHttpClient by lazy {
		OkHttpClient.Builder()
				.addNetworkInterceptor(HttpLoggingInterceptor().apply {
					level = HttpLoggingInterceptor.Level.BODY
				})
				.addNetworkInterceptor(StethoInterceptor())
				.build()
	}

	private val moshi by lazy {
		Moshi.Builder()
				.add(DateAdapter())
				.add(KotlinJsonAdapterFactory())
				.build()
	}

	private val retrofit by lazy {
		Retrofit.Builder()
				.client(okHttpClient)
				.baseUrl("http://192.168.0.200")
				.addCallAdapterFactory(RxJava2CallAdapterFactory.create())
				.addConverterFactory(MoshiConverterFactory.create(moshi))
				.build()
	}

	private val service by lazy {
		retrofit.create(ApiDescription::class.java)
	}

	fun load(username: String, password: String, esnNumber: String): Single<Response> {
		return service.load(username, password, "load", esnNumber)
	}

	fun register(username: String, password: String, userId: String, tripId: String): Single<Response> {
		return service.register(username, password, "register", userId, tripId)
	}

	fun unregister(username: String, password: String, userId: String, tripId: String): Single<Response> {
		return service.register(username, password, "unregister", userId, tripId)
	}

	fun refresh(username: String, password: String, userId: String): Single<Response> {
		return service.refresh(username, password, "refresh", userId)
	}

}
