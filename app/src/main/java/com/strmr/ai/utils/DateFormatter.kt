package com.strmr.ai.utils

import java.text.SimpleDateFormat
import java.util.*

object DateFormatter {
    
    /**
     * Format movie release date as "MM/dd/yyyy"
     * Example: "08/30/2005"
     */
    fun formatMovieDate(releaseDate: String?): String? {
        if (releaseDate.isNullOrBlank()) return null
        
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val outputFormat = SimpleDateFormat("MM/dd/yyyy", Locale.US)
            val date = inputFormat.parse(releaseDate)
            date?.let { outputFormat.format(it) }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Format TV show date range as "yyyy-yyyy" or "yyyy -" if still running
     * Example: "2005-2012" or "2005 -"
     */
    fun formatTvShowDateRange(firstAirDate: String?, lastAirDate: String? = null): String? {
        if (firstAirDate.isNullOrBlank()) return null
        
        val startYear = try {
            firstAirDate.substring(0, 4).toIntOrNull()
        } catch (e: Exception) {
            null
        } ?: return null
        
        val endYear = if (!lastAirDate.isNullOrBlank()) {
            try {
                lastAirDate.substring(0, 4).toIntOrNull()
            } catch (e: Exception) {
                null
            }
        } else null
        
        return if (endYear != null && endYear != startYear) {
            "$startYear-$endYear"
        } else {
            "$startYear -"
        }
    }
    
    /**
     * Format episode air date as "MM/dd/yy"
     * Example: "08/30/05"
     */
    fun formatEpisodeDate(airDate: String?): String? {
        if (airDate.isNullOrBlank()) return null
        
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val outputFormat = SimpleDateFormat("MM/dd/yy", Locale.US)
            val date = inputFormat.parse(airDate)
            date?.let { outputFormat.format(it) }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extract year from date string
     */
    fun extractYear(dateString: String?): Int? {
        if (dateString.isNullOrBlank()) return null
        
        return try {
            dateString.substring(0, 4).toIntOrNull()
        } catch (e: Exception) {
            null
        }
    }
} 