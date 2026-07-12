package bme.prompteng.android.climbtracker.network

import bme.prompteng.android.climbtracker.BuildConfig
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeApiService {
    @GET("youtube/v3/search")
    suspend fun searchVideos(
        @Query("part") part: String = "snippet",
        @Query("q") query: String,
        @Query("type") type: String = "video",
        @Query("videoDuration") videoDuration: String = "short",
        @Query("relevanceLanguage") lang: String = "en",
        @Query("videoEmbeddable") videoEmbeddable: String = "true",
        @Query("maxResults") maxResults: Int = 1,
        @Query("key") apiKey: String = BuildConfig.YOUTUBE_API_KEY
    ): Response<YouTubeSearchResponse>

    companion object {
        private const val BASE_URL = "https://www.googleapis.com/"

        fun create(): YouTubeApiService {
            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(YouTubeApiService::class.java)
        }
    }
}

data class YouTubeSearchResponse(
    val items: List<YouTubeSearchItem>
)

data class YouTubeSearchItem(
    val id: YouTubeVideoId
)

data class YouTubeVideoId(
    val videoId: String
)
