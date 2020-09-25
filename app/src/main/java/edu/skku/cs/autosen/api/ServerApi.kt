package edu.skku.cs.autosen.api

import edu.skku.cs.autosen.Data
import edu.skku.cs.autosen.api.response.ApiResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ServerApi {
    @GET("/api/v1/hello")
    suspend fun hello(): ApiResponse<String>

    @GET("/api/v1/annyeong")
    suspend fun annyeong(): ApiResponse<String>

    @POST("/api/v1/data")
    suspend fun sendData(@Body data: Data): ApiResponse<String> // <T>는 서버로부터 응답메세지를 T 형태로 받는다는 거임

    companion object {
        val instance = ApiGenerator().generate(ServerApi::class.java)
    }
}