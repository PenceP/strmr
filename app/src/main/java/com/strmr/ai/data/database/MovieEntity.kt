package com.strmr.ai.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.strmr.ai.data.database.converters.ListConverter
import com.strmr.ai.data.Actor
import com.strmr.ai.data.SimilarContent
import com.strmr.ai.data.BelongsToCollection

@Entity(tableName = "movies")
@TypeConverters(ListConverter::class)
data class MovieEntity(
    @PrimaryKey val tmdbId: Int,
    val imdbId: String?,
    val title: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val overview: String?,
    val rating: Float?,
    var logoUrl: String? = null,
    val traktRating: Float? = null,
    val traktVotes: Int? = null,
    val year: Int?,
    val releaseDate: String? = null, // Full release date string (yyyy-MM-dd)
    val runtime: Int?,
    val genres: List<String> = emptyList(),
    val cast: List<Actor> = emptyList(),
    val similar: List<SimilarContent> = emptyList(),
    val belongsToCollection: BelongsToCollection? = null,
    val trendingOrder: Int? = null,
    val popularOrder: Int? = null,
    val nowPlayingOrder: Int? = null,
    val upcomingOrder: Int? = null,
    val topRatedOrder: Int? = null,
    val topMoviesWeekOrder: Int? = null,
    val lastUpdated: Long = 0L // for cache expiry
) 