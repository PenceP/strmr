package com.strmr.ai.data

import com.strmr.ai.BuildConfig
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Header
import okhttp3.OkHttpClient
import android.content.Context

interface TraktApiService {
    @Headers("Content-Type: application/json")
    @GET("movies/trending")
    suspend fun getTrendingMovies(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): List<TrendingMovie>

    @Headers("Content-Type: application/json")
    @GET("shows/trending")
    suspend fun getTrendingTvShows(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): List<TrendingShow>

    @Headers("Content-Type: application/json")
    @GET("shows/popular")
    suspend fun getPopularTvShows(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): List<Show>

    @Headers("Content-Type: application/json")
    @GET("movies/popular")
    suspend fun getPopularMovies(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): List<Movie>

    @Headers("Content-Type: application/json")
    @GET("movies/{id}")
    suspend fun getMovieDetails(@Path("id") movieId: Int): Movie

    @Headers("Content-Type: application/json")
    @GET("shows/{id}")
    suspend fun getShowDetails(@Path("id") showId: Int): Show

    @Headers("Content-Type: application/json")
    @GET("movies/{id}/ratings")
    suspend fun getMovieRatings(@Path("id") movieId: Int): MovieRating

    @Headers("Content-Type: application/json")
    @GET("shows/{id}/ratings")
    suspend fun getShowRatings(@Path("id") showId: Int): ShowRating

    @Headers("Content-Type: application/json")
    @GET("sync/history")
    suspend fun getHistory(
        @Header("Authorization") token: String
    ): List<Map<String, Any>>

    @Headers("Content-Type: application/json")
    @GET("sync/playback")
    suspend fun getPlayback(@Header("Authorization") token: String): List<PlaybackItem>

    @GET("users/me")
    suspend fun getUserProfile(@Header("Authorization") token: String): TraktUserProfile

    @GET("users/me/stats")
    suspend fun getUserStats(@Header("Authorization") token: String): TraktUserStats
}

// Separate interface for authenticated user endpoints
interface TraktAuthenticatedApiService {
    @Headers(
        "Content-Type: application/json",
        "trakt-api-version: 2"
    )
    @GET("users/me")
    suspend fun getUserProfile(): TraktUserProfile

    @Headers(
        "Content-Type: application/json",
        "trakt-api-version: 2"
    )
    @GET("users/me/stats")
    suspend fun getUserStats(): TraktUserStats

    @Headers(
        "Content-Type: application/json",
        "trakt-api-version: 2"
    )
    @GET("sync/playback")
    suspend fun getPlayback(): List<PlaybackItem>
}

data class TraktTokenRequest(
    val code: String,
    val client_id: String,
    val client_secret: String,
    val redirect_uri: String,
    val grant_type: String
)

data class TraktTokenResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Int,
    val refresh_token: String,
    val scope: String,
    val created_at: Int
)

private fun getTraktOkHttpClient(context: Context): OkHttpClient {
    return OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("trakt-api-key", BuildConfig.TRAKT_API_KEY)
                .build()
            chain.proceed(request)
        }
        .build()
} 