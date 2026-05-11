package bme.prompteng.android.climbtracker.network

import bme.prompteng.android.climbtracker.BuildConfig
import bme.prompteng.android.climbtracker.model.BetaGenerationResponse
import bme.prompteng.android.climbtracker.model.GeminiRequest
import okhttp3.Interceptor
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Part
import java.util.concurrent.TimeUnit

class AuthInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
        val originalRequest = chain.request()
        val newRequest = originalRequest.newBuilder()
            .header("x-goog-api-key", apiKey)
            .header("Content-Type", "application/json")
            .build()
        return chain.proceed(newRequest)
    }
}

interface ClimbingBetaApi {
    @POST("v1beta/models/gemini-3.1-flash-lite-preview:generateContent")
    suspend fun generateBetaFromImage(
        @Body request: GeminiRequest
    ): Response<GeminiResponse>

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/"
        private val API_KEY = BuildConfig.GEMINI_API_KEY_BETA

        fun create(): ClimbingBetaApi {
            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(AuthInterceptor(API_KEY))
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ClimbingBetaApi::class.java)
        }
    }
}
