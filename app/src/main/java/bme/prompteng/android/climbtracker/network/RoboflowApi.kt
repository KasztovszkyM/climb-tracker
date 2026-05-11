package bme.prompteng.android.climbtracker.network

import bme.prompteng.android.climbtracker.model.RoboflowResponse
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface RoboflowApi {

    @POST("climbing-holds-detector/4")
    suspend fun detectHolds(
        @Query("api_key") apiKey: String,
        @Query("confidence") confidence: Int = 15,
        @Body base64Image: RequestBody
    ): Response<RoboflowResponse>

    companion object {
        private const val BASE_URL = "https://detect.roboflow.com/"

        fun create(): RoboflowApi {
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(RoboflowApi::class.java)
        }
    }
}