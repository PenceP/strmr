package com.strmr.ai.data.mapper

import com.strmr.ai.data.database.MovieEntity
import com.strmr.ai.domain.model.Movie
import com.strmr.ai.domain.model.MovieId
import com.strmr.ai.domain.model.TmdbId
import com.strmr.ai.domain.model.ImdbId
import com.strmr.ai.domain.model.Runtime
import com.strmr.ai.domain.model.Rating
import com.strmr.ai.domain.model.Genre
import com.strmr.ai.domain.model.MediaImages
import com.strmr.ai.domain.model.CastMember
import com.strmr.ai.domain.model.Person
import com.strmr.ai.domain.model.PersonId
import com.strmr.ai.domain.model.Collection
import com.strmr.ai.domain.model.CollectionId
import com.strmr.ai.domain.model.SimilarMovie
import com.strmr.ai.data.BelongsToCollection
import com.strmr.ai.data.Actor
import com.strmr.ai.data.SimilarContent
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Mapper for converting between MovieEntity (database) and Movie (domain model)
 * This establishes clean boundaries between data and domain layers
 */
class MovieMapper @Inject constructor() {
    
    /**
     * Convert database entity to domain model
     */
    fun mapToDomain(entity: MovieEntity): Movie {
        return Movie(
            id = MovieId(entity.tmdbId),
            tmdbId = TmdbId(entity.tmdbId),
            imdbId = entity.imdbId?.let { ImdbId(it) },
            title = entity.title,
            overview = entity.overview,
            year = entity.year,
            releaseDate = entity.releaseDate?.let { parseDate(it) },
            runtime = entity.runtime?.let { Runtime(it) },
            rating = mapRating(entity),
            genres = entity.genres.map { Genre(it) },
            images = mapImages(entity),
            cast = entity.cast.map { mapCastMember(it) },
            collection = entity.belongsToCollection?.let { mapCollection(it) },
            similarMovies = entity.similar.map { mapSimilarMovie(it) },
            lastUpdated = entity.lastUpdated
        )
    }
    
    /**
     * Convert domain model to database entity
     */
    fun mapToEntity(domain: Movie): MovieEntity {
        return MovieEntity(
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
            releaseDate = domain.releaseDate?.let { formatDate(it) },
            runtime = domain.runtime?.minutes,
            genres = domain.genres.map { it.name },
            cast = domain.cast.map { mapCastMemberToData(it) },
            similar = domain.similarMovies.map { mapSimilarMovieToData(it) },
            belongsToCollection = domain.collection?.let { mapCollectionToData(it) },
            lastUpdated = domain.lastUpdated
        )
    }
    
    private fun mapRating(entity: MovieEntity): Rating {
        return Rating(
            tmdbRating = entity.rating,
            traktRating = entity.traktRating,
            traktVotes = entity.traktVotes
        )
    }
    
    private fun mapImages(entity: MovieEntity): MediaImages {
        return MediaImages(
            posterUrl = entity.posterUrl,
            backdropUrl = entity.backdropUrl,
            logoUrl = entity.logoUrl
        )
    }
    
    private fun mapCastMember(actor: Actor): CastMember {
        return CastMember(
            person = Person(
                id = actor.id?.let { PersonId(it) },
                name = actor.name ?: "",
                profileImageUrl = actor.profilePath
            ),
            character = actor.character
        )
    }
    
    private fun mapCollection(collection: BelongsToCollection): Collection {
        return Collection(
            id = CollectionId(collection.id),
            name = collection.name,
            posterUrl = collection.poster_path,
            backdropUrl = collection.backdrop_path
        )
    }
    
    private fun mapSimilarMovie(similar: SimilarContent): SimilarMovie {
        return SimilarMovie(
            id = MovieId(similar.tmdbId),
            tmdbId = TmdbId(similar.tmdbId),
            title = similar.title,
            year = similar.year,
            posterUrl = similar.posterUrl,
            rating = similar.rating
        )
    }
    
    // Reverse mapping helpers
    private fun mapCastMemberToData(cast: CastMember): Actor {
        return Actor(
            id = cast.person.id?.value,
            name = cast.person.name,
            character = cast.character,
            profilePath = cast.person.profileImageUrl
        )
    }
    
    private fun mapCollectionToData(collection: Collection): BelongsToCollection {
        return BelongsToCollection(
            id = collection.id.value,
            name = collection.name,
            poster_path = collection.posterUrl,
            backdrop_path = collection.backdropUrl
        )
    }
    
    private fun mapSimilarMovieToData(similar: SimilarMovie): SimilarContent {
        return SimilarContent(
            tmdbId = similar.tmdbId.value,
            title = similar.title,
            posterUrl = similar.posterUrl,
            backdropUrl = null, // Similar movies don't typically have backdrop
            rating = similar.rating,
            year = similar.year,
            mediaType = "movie"
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