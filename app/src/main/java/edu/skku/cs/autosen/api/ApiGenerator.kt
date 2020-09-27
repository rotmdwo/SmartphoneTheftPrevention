package edu.skku.cs.autosen.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiGenerator {
    fun <T> generate(api: Class<T>): T = Retrofit.Builder().baseUrl(HOST)
        .addConverterFactory(GsonConverterFactory.create()).client(httpClient()).build().create(api)

    private fun httpClient() = OkHttpClient.Builder().apply {
        addInterceptor(httpLoggingInterceptor())
    }.build()

    private fun httpLoggingInterceptor() = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    companion object {
        //const val HOST = "http://172.30.1.57:8080"
        const val HOST = "http://220.79.132.56:8080" // 랜선
        //const val HOST = "http://220.79.204.104:8080" // 공유기
    }
}