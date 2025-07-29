package com.strmr.ai.domain.model

/**
 * Domain model for media genres
 */
data class Genre(
    val name: String
) {
    companion object {
        // Common genres for validation/consistency
        val ACTION = Genre("Action")
        val ADVENTURE = Genre("Adventure")
        val ANIMATION = Genre("Animation")
        val COMEDY = Genre("Comedy")
        val CRIME = Genre("Crime")
        val DOCUMENTARY = Genre("Documentary")
        val DRAMA = Genre("Drama")
        val FAMILY = Genre("Family")
        val FANTASY = Genre("Fantasy")
        val HISTORY = Genre("History")
        val HORROR = Genre("Horror")
        val MUSIC = Genre("Music")
        val MYSTERY = Genre("Mystery")
        val ROMANCE = Genre("Romance")
        val SCIENCE_FICTION = Genre("Science Fiction")
        val THRILLER = Genre("Thriller")
        val WAR = Genre("War")
        val WESTERN = Genre("Western")
    }
}