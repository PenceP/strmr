package com.strmr.ai.domain.model

/**
 * Domain model for movie collections (e.g., "The Matrix Collection")
 */
data class Collection(
    val id: CollectionId,
    val name: String,
    val posterUrl: String? = null,
    val backdropUrl: String? = null
)

@JvmInline
value class CollectionId(val value: Int)