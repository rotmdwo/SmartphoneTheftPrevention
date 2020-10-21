package edu.skku.cs.autosen.api

import edu.skku.cs.autosen.dataType.SensorData
import edu.skku.cs.autosen.api.response.ApiResponse
import edu.skku.cs.autosen.dataType.PictureData
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ServerApi {
    @GET("/api/v1/hello")
    suspend fun hello(): ApiResponse<String>

    @POST("/api/v1/getSecs")
    suspend fun getSecs(@Body userId: String): ApiResponse<String>

    @POST("/api/v1/saveData")
    suspend fun sendData(@Body sensorData: SensorData): ApiResponse<String> // <T>는 서버로부터 응답메세지를 T 형태로 받는다는 거임

    @POST("/api/v1/predict")
    suspend fun predict(@Body sensorData: SensorData): ApiResponse<String>

    @POST("/api/v1/buildModel")
    suspend fun buildModel(@Body userId: String): ApiResponse<String>

    @POST("/api/v1/checkModel")
    suspend fun checkIfModelExists(@Body userId: String): ApiResponse<String>

    @POST("/api/v1/savePic")
    suspend fun savePicture(@Body data: PictureData): ApiResponse<String>

    companion object {
        val instance = ApiGenerator().generate(ServerApi::class.java)
    }
}