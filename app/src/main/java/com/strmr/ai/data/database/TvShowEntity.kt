package com.strmr.ai.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.strmr.ai.data.database.converters.ListConverter
import com.strmr.ai.data.Actor
import com.strmr.ai.data.SimilarContent

@Entity(tableName = "tv_shows")
@TypeConverters(ListConverter::class)
data class TvShowEntity(
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
    val firstAirDate: String? = null, // Full first air date string (yyyy-MM-dd)
    val lastAirDate: String? = null,  // Full last air date string (yyyy-MM-dd)
    val runtime: Int?,
    val genres: List<String> = emptyList(),
    val cast: List<Actor> = emptyList(),
    val similar: List<SimilarContent> = emptyList(),
    val trendingOrder: Int? = null,
    val popularOrder: Int? = null,
    val topRatedOrder: Int? = null,
    val airingTodayOrder: Int? = null,
    val onTheAirOrder: Int? = null,
    val lastUpdated: Long = 0L // for cache expiry
) 