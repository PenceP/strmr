package com.strmr.ai.data.mapper

import com.strmr.ai.data.Actor
import com.strmr.ai.data.SimilarContent
import com.strmr.ai.data.database.TvShowEntity
import com.strmr.ai.domain.model.CastMember
import com.strmr.ai.domain.model.Genre
import com.strmr.ai.domain.model.ImdbId
import com.strmr.ai.domain.model.MediaImages
import com.strmr.ai.domain.model.Person
import com.strmr.ai.domain.model.PersonId
import com.strmr.ai.domain.model.Rating
import com.strmr.ai.domain.model.Runtime
import com.strmr.ai.domain.model.SimilarTvShow
import com.strmr.ai.domain.model.TmdbId
import com.strmr.ai.domain.model.TvShow
import com.strmr.ai.domain.model.TvShowId
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Mapper for converting between TvShowEntity (database) and TvShow (domain model)
 * This establishes clean boundaries between data and domain layers
 */
class TvShowMapper
    @Inject
    constructor() {
        /**
         * Convert database entity to domain model
         */
        fun mapToDomain(entity: TvShowEntity): TvShow {
            return TvShow(
                id = TvShowId(entity.tmdbId),
                tmdbId = TmdbId(entity.tmdbId),
                imdbId = entity.imdbId?.let { ImdbId(it) },
                title = entity.title,
                overview = entity.overview,
                year = entity.year,
                firstAirDate = entity.firstAirDate?.let { parseDate(it) },
                lastAirDate = entity.lastAirDate?.let { parseDate(it) },
                runtime = entity.runtime?.let { Runtime(it) },
                rating = mapRating(entity),
                genres = entity.genres.map { Genre(it) },
                images = mapImages(entity),
                cast = entity.cast.map { mapCastMember(it) },
                similarShows = entity.similar.map { mapSimilarTvShow(it) },
                lastUpdated = entity.lastUpdated,
            )
        }

        /**
         * Convert domain model to database entity
         */
        fun mapToEntity(domain: TvShow): TvShowEntity {
            return TvShowEntity(
                tmdbId = domain.tmdbId.value,
                imdbId = domain.imdbId?.value,
                title = domain.title,
                posterUrl = domain.images.posterUrl,
                backdropUrl = domain.images.backdropUrl,
                overview = domain.overview,
                rating = domain.rating.primaryRating,
                logoUrl = domain.images.logoUrl,
                traktRating = domain.rating.traktRating,
                traktVotes = domain.rating.traktVotes,
                year = domain.year,
                firstAirDate = domain.firstAirDate?.let { formatDate(it) },
                lastAirDate = domain.lastAirDate?.let { formatDate(it) },
                runtime = domain.runtime?.minutes,
                genres = domain.genres.map { it.name },
                cast = domain.cast.map { mapCastMemberToData(it) },
                similar = domain.similarShows.map { mapSimilarTvShowToData(it) },
                lastUpdated = domain.lastUpdated,
            )
        }

        private fun mapRating(entity: TvShowEntity): Rating {
            return Rating(
                tmdbRating = entity.rating,
                traktRating = entity.traktRating,
                traktVotes = entity.traktVotes,
            )
        }

        private fun mapImages(entity: TvShowEntity): MediaImages {
            return MediaImages(
                posterUrl = entity.posterUrl,
                backdropUrl = entity.backdropUrl,
                logoUrl = entity.logoUrl,
            )
        }

        private fun mapCastMember(actor: Actor): CastMember {
            return CastMember(
                person =
                    Person(
                        id = actor.id?.let { PersonId(it) },
                        name = actor.name ?: "",
                        profileImageUrl = actor.profilePath,
                    ),
                character = actor.character,
            )
        }

        private fun mapSimilarTvShow(similar: SimilarContent): SimilarTvShow {
            return SimilarTvShow(
                id = TvShowId(similar.tmdbId),
                tmdbId = TmdbId(similar.tmdbId),
                title = similar.title,
                year = similar.year,
                posterUrl = similar.posterUrl,
                rating = similar.rating,
            )
        }

        // Reverse mapping helpers
        private fun mapCastMemberToData(cast: CastMember): Actor {
            return Actor(
                id = cast.person.id?.value,
                name = cast.person.name,
                character = cast.character,
                profilePath = cast.person.profileImageUrl,
            )
        }

        private fun mapSimilarTvShowToData(similar: SimilarTvShow): SimilarContent {
            return SimilarContent(
                tmdbId = similar.tmdbId.value,
                title = similar.title,
                posterUrl = similar.posterUrl,
                backdropUrl = null, // Similar shows don't typically have backdrop
                rating = similar.rating,
                year = similar.year,
                mediaType = "tv",
            )
        }

        private fun parseDate(dateString: String): LocalDate? {
            return try {
                LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (e: Exception) {
                null
            }
        }

        private fun formatDate(date: LocalDate): String {
            return date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        }
    }
