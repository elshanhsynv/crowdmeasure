package com.example.crowdmeasure.data.measurement.net

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class OkHttpClientProvider {
    fun create(): OkHttpClient {
        return OkHttpClient.Builder()
            .callTimeout(20, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}