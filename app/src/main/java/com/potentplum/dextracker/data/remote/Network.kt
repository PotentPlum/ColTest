package com.potentplum.dextracker.data.remote

import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

object Network {
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun okHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .addInterceptor(RetryInterceptor())
        .build()

    fun pokeApi(client: OkHttpClient): PokeApiService =
        retrofit(PokeApiService.BASE_URL, client).create(PokeApiService::class.java)

    fun tcgApi(client: OkHttpClient): TcgApiService =
        retrofit(TcgApiService.BASE_URL, client).create(TcgApiService::class.java)

    private fun retrofit(baseUrl: String, client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}

/**
 * The keyless TCG API occasionally answers with a transient 5xx; retry idempotent GETs a
 * couple of times with a short backoff before surfacing an error to the user.
 */
private class RetryInterceptor(private val maxAttempts: Int = 3) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.method != "GET") return chain.proceed(request)

        var attempt = 1
        while (true) {
            val isLast = attempt >= maxAttempts
            try {
                val response = chain.proceed(request)
                if (response.code < 500 || isLast) return response
                response.close()
            } catch (e: IOException) {
                if (isLast || chain.call().isCanceled()) throw e
            }
            Thread.sleep(400L * attempt)
            attempt++
        }
    }
}
