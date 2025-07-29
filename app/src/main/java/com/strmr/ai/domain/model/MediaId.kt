package com.strmr.ai.domain.model

import androidx.compose.runtime.Immutable

/**
 * Value objects for type-safe media identifiers
 * @Immutable annotations ensure these value classes are treated as immutable by Compose
 */
@Immutable
@JvmInline
value class MovieId(val value: Int)

@Immutable
@JvmInline
value class TvShowId(val value: Int)

@Immutable
@JvmInline
value class TraktId(val value: Int)

@Immutable
@JvmInline
value class ImdbId(val value: String)

@Immutable
@JvmInline
value class TmdbId(val value: Int)