package com.strmr.ai.data

data class TrendingMovie(
    val watchers: Int,
    val movie: Movie
)

data class TrendingShow(
    val watchers: Int,
    val show: Show
)

data class Movie(
    val title: String,
    val year: Int?,
    val ids: MovieIds
)

data class Show(
    val title: String,
    val year: Int?,
    val ids: ShowIds
)

data class MovieIds(
    val trakt: Int?,
    val slug: String?,
    val imdb: String?,
    val tmdb: Int?
)

data class ShowIds(
    val trakt: Int?,
    val slug: String?,
    val imdb: String?,
    val tmdb: Int?
)

data class MovieRating(
    val rating: Float,
    val votes: Int,
    val distribution: Map<String, Int>
)

data class ShowRating(
    val rating: Float,
    val votes: Int,
    val distribution: Map<String, Int>
)

// User profile and statistics models
data class TraktUserProfile(
    val username: String,
    val private: Boolean,
    val name: String?,
    val vip: Boolean,
    val vip_ep: Boolean,
    val ids: UserIds
)

data class UserIds(
    val slug: String,
    val uuid: String
)

data class TraktUserStats(
    val movies: MovieStats,
    val shows: ShowStats,
    val seasons: SeasonStats,
    val episodes: EpisodeStats,
    val network: NetworkStats,
    val ratings: RatingStats
)

data class MovieStats(
    val plays: Int,
    val watched: Int,
    val minutes: Int,
    val collected: Int,
    val ratings: Int,
    val comments: Int
)

data class ShowStats(
    val watched: Int,
    val collected: Int,
    val ratings: Int,
    val comments: Int
)

data class SeasonStats(
    val ratings: Int,
    val comments: Int
)

data class EpisodeStats(
    val plays: Int,
    val watched: Int,
    val minutes: Int,
    val collected: Int,
    val ratings: Int,
    val comments: Int
)

data class NetworkStats(
    val friends: Int,
    val followers: Int,
    val following: Int
)

data class RatingStats(
    val total: Int,
    val distribution: Map<String, Int>
)

data class TraktSyncSettings(
    val syncOnLaunch: Boolean = true,
    val syncAfterPlayback: Boolean = true,
    val lastSyncTimestamp: Long = 0L
)

data class PlaybackItem(
    val id: Long,
    val progress: Float,
    val paused_at: String,
    val type: String,
    val movie: Movie? = null,
    val episode: Episode? = null,
    val show: Show? = null
)

data class Episode(
    val season: Int,
    val number: Int,
    val title: String,
    val ids: EpisodeIds
)

data class EpisodeIds(
    val trakt: Int,
    val tmdb: Int?,
    val imdb: String?,
    val tvdb: Int?
)

// Collection models
data class BelongsToCollection(
    val id: Int,
    val name: String,
    val poster_path: String?,
    val backdrop_path: String?
)

data class Collection(
    val id: Int,
    val name: String,
    val overview: String?,
    val poster_path: String?,
    val backdrop_path: String?,
    val parts: List<CollectionMovie>
)

data class CollectionMovie(
    val id: Int,
    val title: String,
    val original_title: String,
    val overview: String?,
    val poster_path: String?,
    val backdrop_path: String?,
    val media_type: String,
    val original_language: String,
    val genre_ids: List<Int>,
    val popularity: Double,
    val release_date: String?,
    val video: Boolean,
    val vote_average: Double,
    val vote_count: Int
) 