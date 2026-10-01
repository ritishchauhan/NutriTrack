package com.example.macro_tracker.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NutritionApiClient {
    private const val BASE_URL = "https://world.openfoodfacts.org/"
    private const val USER_AGENT = "Nutritrack - Android - Version 1.0 - https://github.com/ritishchauhan/Nutritrack"
    private const val CACHE_SIZE = 10L * 1024 * 1024 // 10 MB

    @Volatile
    private var httpCache: okhttp3.Cache? = null

    fun initCache(cacheDir: java.io.File) {
        if (httpCache == null) {
            synchronized(this) {
                if (httpCache == null) {
                    try {
                        val cacheFolder = java.io.File(cacheDir, "openfoodfacts_http_cache")
                        if (!cacheFolder.exists()) cacheFolder.mkdirs()
                        httpCache = okhttp3.Cache(cacheFolder, CACHE_SIZE)
                    } catch (e: Exception) {
                        // Fallback gracefully if cache folder creation fails
                    }
                }
            }
        }
    }

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient by lazy {
        val builder = OkHttpClient.Builder()
            .connectionPool(ConnectionPool(5, 5, TimeUnit.MINUTES))
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                if (chain.request().method.equals("GET", ignoreCase = true) && response.isSuccessful) {
                    // Cache successful food item lookups locally for 2 hours
                    response.newBuilder()
                        .header("Cache-Control", "public, max-age=7200")
                        .removeHeader("Pragma")
                        .build()
                } else {
                    response
                }
            }
            .retryOnConnectionFailure(true)

        httpCache?.let { builder.cache(it) }
        builder.build()
    }

    val api: NutritionApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(NutritionApi::class.java)
    }
}

