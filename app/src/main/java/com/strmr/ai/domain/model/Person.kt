package com.strmr.ai.domain.model

/**
 * Domain model for people (actors, directors, etc.)
 */
data class Person(
    val id: PersonId?,
    val name: String,
    val profileImageUrl: String? = null
)

/**
 * Domain model for cast member with their role
 */
data class CastMember(
    val person: Person,
    val character: String?
)

@JvmInline
value class PersonId(val value: Int)