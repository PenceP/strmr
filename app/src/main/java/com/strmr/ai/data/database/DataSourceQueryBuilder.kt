package com.strmr.ai.data.database

/**
 * Query builder for dynamic data source queries
 */
object DataSourceQueryBuilder {
    /**
     * Build a query for getting items from a specific data source
     */
    fun buildDataSourceQuery(
        tableName: String,
        dataSourceId: String,
        orderBy: String = "ASC",
    ): String {
        val fieldName = getDataSourceField(dataSourceId)
        return """
            SELECT * FROM $tableName 
            WHERE $fieldName IS NOT NULL 
            ORDER BY $fieldName $orderBy
            """.trimIndent()
    }

    /**
     * Build a query for clearing a specific data source
     */
    fun buildClearDataSourceQuery(
        tableName: String,
        dataSourceId: String,
    ): String {
        val fieldName = getDataSourceField(dataSourceId)
        return """
            UPDATE $tableName 
            SET $fieldName = NULL 
            WHERE $fieldName IS NOT NULL
            """.trimIndent()
    }

    /**
     * Map data source config to database field name
     */
    fun getDataSourceField(dataSourceId: String): String {
        return when (dataSourceId) {
            "trending" -> "trendingOrder"
            "popular" -> "popularOrder"
            "now_playing" -> "nowPlayingOrder"
            "upcoming" -> "upcomingOrder"
            "top_rated" -> "topRatedOrder"
            "top_movies_week" -> "topMoviesWeekOrder"
            "recommended_movies" -> "recommendedMoviesOrder"
            "airing_today" -> "airingTodayOrder"
            "on_the_air" -> "onTheAirOrder"
            else -> throw IllegalArgumentException("Unknown data source: $dataSourceId")
        }
    }
}
