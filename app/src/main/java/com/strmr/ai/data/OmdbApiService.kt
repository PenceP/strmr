package com.strmr.ai.data

import retrofit2.http.GET
import retrofit2.http.Query

interface OmdbApiService {
    @GET("/")
    suspend fun getOmdbRatings(
        @Query("apikey") apiKey: String,
        @Query("i") imdbId: String? = null,
        @Query("t") title: String? = null,
        @Query("y") year: String? = null
    ): OmdbResponse
}

data class OmdbResponse(
    val Title: String?,
    val Year: String?,
    val Rated: String?,
    val Released: String?,
    val Runtime: String?,
    val Genre: String?,
    val Director: String?,
    val Writer: String?,
    val Actors: String?,
    val Plot: String?,
    val Language: String?,
    val Country: String?,
    val Awards: String?,
    val Poster: String?,
    val Ratings: List<OmdbRating>?,
    val Metascore: String?,
    val imdbRating: String?,
    val imdbVotes: String?,
    val imdbID: String?,
    val Type: String?,
    val DVD: String?,
    val BoxOffice: String?,
    val Production: String?,
    val Website: String?,
    val Response: String?
)

data class OmdbRating(
    val Source: String?,
    val Value: String?
) 