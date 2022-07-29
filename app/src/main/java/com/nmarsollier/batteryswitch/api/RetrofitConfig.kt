package com.nmarsollier.batteryswitch.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.logging.Logger

private val logger: Logger = Logger.getLogger("OkHttp")

class RetrofitConfig(
    serverName: String
) {
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor { message: String? ->
            logger.info(
                message
            )
        }.apply {
            setLevel(HttpLoggingInterceptor.Level.BODY)
        })
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .client(client)
        .baseUrl("http://$serverName/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
}
