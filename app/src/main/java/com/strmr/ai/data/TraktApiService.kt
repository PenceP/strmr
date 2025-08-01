package com.strmr.ai.data

import android.content.Context
import com.strmr.ai.BuildConfig
import com.strmr.ai.ui.theme.StrmrConstants
import okhttp3.OkHttpClient
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

interface TraktApiService {
    @Headers("Content-Type: application/json")
    @GET("movies/trending")
    suspend fun getTrendingMovies(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = StrmrConstants.Api.LARGE_PAGE_SIZE,
    ): List<TrendingMovie>

    @Headers("Content-Type: application/json")
    @GET("shows/trending")
    suspend fun getTrendingTvShows(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = StrmrConstants.Api.LARGE_PAGE_SIZE,
    ): List<TrendingShow>

    @Headers("Content-Type: application/json")
    @GET("shows/popular")
    suspend fun getPopularTvShows(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = StrmrConstants.Api.LARGE_PAGE_SIZE,
    ): List<Show>

    @Headers("Content-Type: application/json")
    @GET("movies/popular")
    suspend fun getPopularMovies(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = StrmrConstants.Api.LARGE_PAGE_SIZE,
    ): List<Movie>

    @Headers("Content-Type: application/json")
    @GET("movies/{id}")
    suspend fun getMovieDetails(
        @Path("id") movieId: Int,
    ): Movie

    @Headers("Content-Type: application/json")
    @GET("shows/{id}")
    suspend fun getShowDetails(
        @Path("id") showId: Int,
    ): Show

    @Headers("Content-Type: application/json")
    @GET("movies/{id}/ratings")
    suspend fun getMovieRatings(
        @Path("id") movieId: Int,
    ): MovieRating

    @Headers("Content-Type: application/json")
    @GET("shows/{id}/ratings")
    suspend fun getShowRatings(
        @Path("id") showId: Int,
    ): ShowRating

    @Headers("Content-Type: application/json")
    @GET("sync/history")
    suspend fun getHistory(
        @Header("Authorization") token: String,
        @Query("limit") limit: Int = StrmrConstants.Api.LARGE_PAGE_SIZE,
    ): List<WatchedHistoryItem>

    @Headers("Content-Type: application/json")
    @GET("sync/playback")
    suspend fun getPlayback(
        @Header("Authorization") token: String,
    ): List<PlaybackItem>

    @Headers("Content-Type: application/json")
    @GET("sync/watched")
    suspend fun getWatched(
        @Header("Authorization") token: String,
        @Query("extended") extended: String = "full",
    ): List<WatchedItem>

    @Headers("Content-Type: application/json")
    @GET("shows/{id}/progress/watched")
    suspend fun getShowProgress(
        @Header("Authorization") token: String,
        @Path("id") showId: Int,
    ): ShowProgress

    @GET("users/me")
    suspend fun getUserProfile(
        @Header("Authorization") token: String,
    ): TraktUserProfile

    @GET("users/me/stats")
    suspend fun getUserStats(
        @Header("Authorization") token: String,
    ): TraktUserStats

    @Headers("Content-Type: application/json")
    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("limit") limit: Int = StrmrConstants.Api.DEFAULT_PAGE_SIZE,
    ): List<TraktSearchResult>

    @Headers("Content-Type: application/json")
    @GET("search/show")
    suspend fun searchTvShows(
        @Query("query") query: String,
        @Query("limit") limit: Int = StrmrConstants.Api.DEFAULT_PAGE_SIZE,
    ): List<TraktSearchResult>

    @Headers("Content-Type: application/json")
    @GET("search/person")
    suspend fun searchPeople(
        @Query("query") query: String,
        @Query("limit") limit: Int = StrmrConstants.Api.DEFAULT_PAGE_SIZE,
    ): List<TraktSearchResult>

    @Headers("Content-Type: application/json")
    @GET("users/{username}/lists/{list_slug}/items")
    suspend fun getUserListItems(
        @Path("username") username: String,
        @Path("list_slug") listSlug: String,
        @Query("extended") extended: String = "full",
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = StrmrConstants.Api.DEFAULT_PAGE_SIZE,
    ): List<TraktListItem>

    // Sync endpoints for authenticated users
    @Headers("Content-Type: application/json")
    @GET("sync/collection/movies")
    suspend fun getMovieCollection(
        @Header("Authorization") token: String,
        @Query("extended") extended: String = "full",
    ): List<TraktCollectionItem>

    @Headers("Content-Type: application/json")
    @GET("sync/collection/shows")
    suspend fun getShowCollection(
        @Header("Authorization") token: String,
        @Query("extended") extended: String = "full",
    ): List<TraktCollectionItem>

    @Headers("Content-Type: application/json")
    @GET("sync/watchlist/movies")
    suspend fun getMovieWatchlist(
        @Header("Authorization") token: String,
        @Query("extended") extended: String = "full",
    ): List<TraktWatchlistItem>

    @Headers("Content-Type: application/json")
    @GET("sync/watchlist/shows")
    suspend fun getShowWatchlist(
        @Header("Authorization") token: String,
        @Query("extended") extended: String = "full",
    ): List<TraktWatchlistItem>
}

// Separate interface for authenticated user endpoints
interface TraktAuthenticatedApiService {
    @Headers(
        "Content-Type: application/json",
        "trakt-api-version: 2",
    )
    @GET("users/me")
    suspend fun getUserProfile(): TraktUserProfile

    @Headers(
        "Content-Type: application/json",
        "trakt-api-version: 2",
    )
    @GET("users/me/stats")
    suspend fun getUserStats(): TraktUserStats

    @Headers(
        "Content-Type: application/json",
        "trakt-api-version: 2",
    )
    @GET("sync/playback")
    suspend fun getPlayback(): List<PlaybackItem>

    @Headers(
        "Content-Type: application/json",
        "trakt-api-version: 2",
    )
    @GET("sync/history")
    suspend fun getHistory(
        @Query("limit") limit: Int = 50,
    ): List<WatchedHistoryItem>

    @Headers(
        "Content-Type: application/json",
        "trakt-api-version: 2",
    )
    @GET("sync/watched")
    suspend fun getWatched(
        @Query("extended") extended: String = "full",
    ): List<WatchedItem>

    @Headers(
        "Content-Type: application/json",
        "trakt-api-version: 2",
    )
    @GET("shows/{id}/progress/watched")
    suspend fun getShowProgress(
        @Path("id") showId: Int,
    ): ShowProgress
}

data class TraktTokenRequest(
    val code: String,
    val client_id: String,
    val client_secret: String,
    val redirect_uri: String,
    val grant_type: String,
)

data class TraktTokenResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Int,
    val refresh_token: String,
    val scope: String,
    val created_at: Int,
)

data class TraktSearchResult(
    val type: String, // "movie", "show", "person"
    val score: Float,
    val movie: Movie?,
    val show: Show?,
    val person: TraktPerson?,
)

data class TraktPerson(
    val name: String,
    val ids: TraktPersonIds,
)

data class TraktPersonIds(
    val trakt: Int?,
    val slug: String?,
    val imdb: String?,
    val tmdb: Int?,
)

private fun getTraktOkHttpClient(context: Context): OkHttpClient {
    return OkHttpClient.Builder()
        .addInterceptor { chain ->
            val request =
                chain.request().newBuilder()
                    .addHeader("trakt-api-key", BuildConfig.TRAKT_API_KEY)
                    .build()
            chain.proceed(request)
        }
        .build()
}
