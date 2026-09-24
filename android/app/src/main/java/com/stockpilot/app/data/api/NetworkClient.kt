package com.stockpilot.app.data.api

import com.stockpilot.app.data.local.TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {

    @Volatile
    private var apiService: StockPilotApiService? = null
    private var currentBaseUrl: String? = null

    fun getApiService(tokenManager: TokenManager): StockPilotApiService {
        val targetBaseUrl = tokenManager.getBaseUrl()

        if (apiService == null || currentBaseUrl != targetBaseUrl) {
            synchronized(this) {
                if (apiService == null || currentBaseUrl != targetBaseUrl) {
                    currentBaseUrl = targetBaseUrl

                    val loggingInterceptor = HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    }

                    val okHttpClient = OkHttpClient.Builder()
                        .addInterceptor(AuthInterceptor(tokenManager))
                        .addInterceptor(loggingInterceptor)
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .writeTimeout(30, TimeUnit.SECONDS)
                        .build()

                    val retrofit = Retrofit.Builder()
                        .baseUrl(targetBaseUrl)
                        .client(okHttpClient)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()

                    apiService = retrofit.create(StockPilotApiService::class.java)
                }
            }
        }
        return apiService!!
    }

    fun resetClient() {
        synchronized(this) {
            apiService = null
            currentBaseUrl = null
        }
    }
}
