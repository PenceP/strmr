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
        dataSourceField: String,
        orderBy: String = "ASC"
    ): String {
        return """
            SELECT * FROM $tableName 
            WHERE $dataSourceField IS NOT NULL 
            ORDER BY $dataSourceField $orderBy
        """.trimIndent()
    }
    
    /**
     * Build a query for clearing a specific data source
     */
    fun buildClearDataSourceQuery(
        tableName: String,
        dataSourceField: String
    ): String {
        return """
            UPDATE $tableName 
            SET $dataSourceField = NULL 
            WHERE $dataSourceField IS NOT NULL
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
            "airing_today" -> "airingTodayOrder"
            "on_the_air" -> "onTheAirOrder"
            else -> throw IllegalArgumentException("Unknown data source: $dataSourceId")
        }
    }
}