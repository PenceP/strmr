package com.strmr.ai.data.mapper

import com.strmr.ai.data.database.AccountEntity
import com.strmr.ai.data.database.ContinueWatchingEntity
import com.strmr.ai.domain.repository.UserAccount
import com.strmr.ai.domain.repository.ContinueWatchingItem
import javax.inject.Inject

/**
 * Mapper for converting between account entities and domain models
 */
class AccountMapper @Inject constructor() {
    
    /**
     * Convert AccountEntity to UserAccount domain model
     */
    fun mapAccountToDomain(entity: AccountEntity): UserAccount {
        return UserAccount(
            accountType = entity.accountType,
            username = entity.username ?: "",
            displayName = entity.username, // Use username as display name if needed
            isActive = true, // Always active if entity exists
            lastSyncTime = entity.lastSyncTimestamp
        )
    }
    
    /**
     * Convert UserAccount domain model to AccountEntity
     */
    fun mapAccountToEntity(domain: UserAccount, accessToken: String = "", refreshToken: String = "", expiresAt: Long = 0L): AccountEntity {
        return AccountEntity(
            accountType = domain.accountType,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAt = expiresAt,
            username = domain.username,
            lastSyncTimestamp = domain.lastSyncTime
        )
    }
    
    /**
     * Convert ContinueWatchingEntity to domain model
     */
    fun mapContinueWatchingToDomain(entity: ContinueWatchingEntity): ContinueWatchingItem {
        val mediaId = when (entity.type) {
            "movie" -> entity.movieTmdbId ?: 0
            "episode" -> entity.showTmdbId ?: 0
            else -> 0
        }
        
        val title = when (entity.type) {
            "movie" -> entity.movieTitle ?: "Unknown Movie"
            "episode" -> "${entity.showTitle ?: "Unknown Show"} - S${entity.episodeSeason ?: 0}E${entity.episodeNumber ?: 0}"
            else -> "Unknown"
        }
        
        val mediaType = when (entity.type) {
            "movie" -> "movie"
            "episode" -> "tv"
            else -> "unknown"
        }
        
        return ContinueWatchingItem(
            mediaId = mediaId,
            mediaType = mediaType,
            title = title,
            posterUrl = null, // Entity doesn't store poster URL
            progress = entity.progress ?: 0f,
            lastWatched = parseTimestamp(entity.lastWatchedAt)
        )
    }
    
    /**
     * Convert domain model to ContinueWatchingEntity (simplified for movies)
     */
    fun mapContinueWatchingToEntity(domain: ContinueWatchingItem, id: String = ""): ContinueWatchingEntity {
        return ContinueWatchingEntity(
            id = id.ifEmpty { "${domain.mediaType}_${domain.mediaId}_${System.currentTimeMillis()}" },
            type = if (domain.mediaType == "movie") "movie" else "episode",
            lastWatchedAt = formatTimestamp(domain.lastWatched),
            progress = if (domain.progress > 0f) domain.progress else null,
            movieTitle = if (domain.mediaType == "movie") domain.title else null,
            movieTmdbId = if (domain.mediaType == "movie") domain.mediaId else null,
            showTitle = if (domain.mediaType == "tv") domain.title else null,
            showTmdbId = if (domain.mediaType == "tv") domain.mediaId else null,
            isInProgress = domain.progress > 0f && domain.progress < 1f,
            isNextEpisode = domain.progress == 0f
        )
    }
    
    private fun parseTimestamp(timestampString: String): Long {
        return try {
            timestampString.toLongOrNull() ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        return timestamp.toString()
    }
}