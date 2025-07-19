package com.strmr.ai.data

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {
    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(@Path("movie_id") movieId: Int): TmdbMovieDetails
    
    @GET("tv/{tv_id}")
    suspend fun getTvShowDetails(@Path("tv_id") tvId: Int): TmdbTvShowDetails
    
    @GET("movie/{movie_id}/images")
    suspend fun getMovieImages(@Path("movie_id") movieId: Int): TmdbImagesResponse

    @GET("tv/{tv_id}/images")
    suspend fun getTvShowImages(@Path("tv_id") tvId: Int): TmdbImagesResponse

    @GET("movie/{movie_id}/credits")
    suspend fun getMovieCredits(@Path("movie_id") movieId: Int): TmdbCreditsResponse

    @GET("tv/{tv_id}/credits")
    suspend fun getTvShowCredits(@Path("tv_id") tvId: Int): TmdbCreditsResponse

    @GET("tv/{tv_id}/season/{season_number}/episode/{episode_number}")
    suspend fun getEpisodeDetails(
        @Path("tv_id") tvId: Int,
        @Path("season_number") seasonNumber: Int,
        @Path("episode_number") episodeNumber: Int
    ): TmdbEpisodeDetails

    @GET("tv/{tv_id}/season/{season_number}")
    suspend fun getSeasonDetails(
        @Path("tv_id") tvId: Int,
        @Path("season_number") seasonNumber: Int
    ): TmdbSeasonDetails

    @GET("movie/{movie_id}/similar")
    suspend fun getSimilarMovies(
        @Path("movie_id") movieId: Int
    ): TmdbSimilarResponse

    @GET("tv/{tv_id}/similar")
    suspend fun getSimilarTvShows(
        @Path("tv_id") tvId: Int
    ): TmdbSimilarResponse

    @GET("collection/{collection_id}")
    suspend fun getCollectionDetails(
        @Path("collection_id") collectionId: Int
    ): Collection

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("search/tv")
    suspend fun searchTvShows(
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("search/person")
    suspend fun searchPeople(
        @Query("query") query: String,
        @Query("page") page: Int = 1
    ): TmdbSearchResponse

    @GET("movie/{movie_id}/videos")
    suspend fun getMovieVideos(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = "en-US"
    ): TmdbVideosResponse

    @GET("tv/{tv_id}/videos")
    suspend fun getTvShowVideos(
        @Path("tv_id") tvId: Int,
        @Query("language") language: String = "en-US"
    ): TmdbVideosResponse
}

data class TmdbMovieDetails(
    val id: Int,
    val imdb_id: String?,
    val title: String?,
    val overview: String?,
    val poster_path: String?,
    val backdrop_path: String?,
    val vote_average: Float?,
    val release_date: String?,
    val runtime: Int?,
    val genres: List<Genre>,
    val belongs_to_collection: BelongsToCollection?
)

data class TmdbTvShowDetails(
    val id: Int,
    val imdb_id: String?,
    val name: String?,
    val overview: String?,
    val poster_path: String?,
    val backdrop_path: String?,
    val vote_average: Float?,
    val first_air_date: String?,
    val last_air_date: String?,
    val episode_run_time: List<Int>?,
    val genres: List<Genre>
)

data class Genre(
    val id: Int,
    val name: String
)

data class TmdbImagesResponse(
    val backdrops: List<TmdbImage>,
    val logos: List<TmdbLogo>
)

data class TmdbImage(
    val file_path: String
)

data class TmdbLogo(
    val file_path: String?,
    val iso_639_1: String?
)

data class TmdbCreditsResponse(
    val cast: List<TmdbCastMember>
)

data class TmdbCastMember(
    val id: Int?,
    val name: String?,
    val character: String?,
    val profile_path: String?
)

data class TmdbEpisodeDetails(
    val still_path: String?,
    val name: String?,
    val overview: String?
) 

data class TmdbSeasonDetails(
    val id: Int,
    val name: String?,
    val overview: String?,
    val poster_path: String?,
    val season_number: Int,
    val air_date: String?,
    val episodes: List<TmdbEpisode>?
)

data class TmdbEpisode(
    val episode_number: Int,
    val name: String?,
    val overview: String?,
    val still_path: String?,
    val air_date: String?,
    val runtime: Int?
) 

data class TmdbSimilarResponse(
    val page: Int,
    val results: List<TmdbSimilarItem>,
    val total_pages: Int,
    val total_results: Int
)

data class TmdbSimilarItem(
    val id: Int,
    val title: String?, // For movies
    val name: String?,  // For TV shows
    val poster_path: String?,
    val backdrop_path: String?,
    val vote_average: Float?,
    val release_date: String?, // For movies
    val first_air_date: String?, // For TV shows
    val media_type: String? = null
)

data class TmdbSearchResponse(
    val page: Int,
    val results: List<TmdbSearchResult>,
    val total_pages: Int,
    val total_results: Int
)

data class TmdbSearchResult(
    val id: Int,
    val media_type: String?, // "movie", "tv", "person"
    val title: String?, // For movies
    val name: String?, // For TV shows and people
    val poster_path: String?,
    val backdrop_path: String?,
    val profile_path: String?, // For people
    val vote_average: Float?,
    val release_date: String?, // For movies
    val first_air_date: String?, // For TV shows
    val overview: String?,
    val known_for_department: String?, // For people
    val known_for: List<TmdbKnownFor>? // For people
)

data class TmdbKnownFor(
    val id: Int,
    val media_type: String,
    val title: String?,
    val name: String?,
    val poster_path: String?
)

data class TmdbVideosResponse(
    val id: Int,
    val results: List<TmdbVideo>
)

data class TmdbVideo(
    val id: String,
    val iso_639_1: String?,
    val iso_3166_1: String?,
    val key: String,
    val name: String,
    val official: Boolean,
    val published_at: String?,
    val site: String, // e.g., "YouTube"
    val size: Int?,
    val type: String // e.g., "Trailer", "Teaser", "Clip", "Featurette"
) 