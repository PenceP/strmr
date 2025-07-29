package com.strmr.ai.domain.model

/**
 * Value objects for type-safe media identifiers
 */
@JvmInline
value class MovieId(val value: Int)

@JvmInline
value class TvShowId(val value: Int)

@JvmInline
value class TraktId(val value: Int)

@JvmInline
value class ImdbId(val value: String)

@JvmInline
value class TmdbId(val value: Int)