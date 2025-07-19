package com.strmr.ai.data

/**
 * Generic data source configuration that maps JSON config to API endpoints
 */
data class DataSourceConfig(
    val id: String,
    val title: String,
    val endpoint: String,
    val mediaType: MediaType,
    val cacheKey: String,
    val enabled: Boolean = true,
    val order: Int = 0
)

enum class MediaType {
    MOVIE, TV_SHOW
}

/**
 * Registry of all available data sources
 */
object DataSourceRegistry {
    
    private val movieDataSources = mapOf(
        "trending" to DataSourceConfig(
            id = "trending",
            title = "Trending",
            endpoint = "movies/trending",
            mediaType = MediaType.MOVIE,
            cacheKey = "trending_movies"
        ),
        "popular" to DataSourceConfig(
            id = "popular", 
            title = "Popular",
            endpoint = "movies/popular",
            mediaType = MediaType.MOVIE,
            cacheKey = "popular_movies"
        ),
        "now_playing" to DataSourceConfig(
            id = "now_playing",
            title = "Now Playing", 
            endpoint = "movies/now_playing",
            mediaType = MediaType.MOVIE,
            cacheKey = "now_playing_movies"
        ),
        "upcoming" to DataSourceConfig(
            id = "upcoming",
            title = "Upcoming",
            endpoint = "movies/upcoming", 
            mediaType = MediaType.MOVIE,
            cacheKey = "upcoming_movies"
        ),
        "top_rated" to DataSourceConfig(
            id = "top_rated",
            title = "Top Rated",
            endpoint = "movies/top_rated",
            mediaType = MediaType.MOVIE, 
            cacheKey = "top_rated_movies"
        ),
        "top_movies_week" to DataSourceConfig(
            id = "top_movies_week",
            title = "Top Movies of the Week",
            endpoint = "users/garycrawfordgc/lists/top-movies-of-the-week/items",
            mediaType = MediaType.MOVIE,
            cacheKey = "top_movies_week"
        )
    )
    
    private val tvDataSources = mapOf(
        "trending" to DataSourceConfig(
            id = "trending",
            title = "Trending", 
            endpoint = "shows/trending",
            mediaType = MediaType.TV_SHOW,
            cacheKey = "trending_tv_shows"
        ),
        "popular" to DataSourceConfig(
            id = "popular",
            title = "Popular",
            endpoint = "shows/popular", 
            mediaType = MediaType.TV_SHOW,
            cacheKey = "popular_tv_shows"
        ),
        "top_rated" to DataSourceConfig(
            id = "top_rated", 
            title = "Top Rated",
            endpoint = "shows/top_rated",
            mediaType = MediaType.TV_SHOW,
            cacheKey = "top_rated_tv_shows"
        ),
        "airing_today" to DataSourceConfig(
            id = "airing_today",
            title = "Airing Today",
            endpoint = "shows/airing_today",
            mediaType = MediaType.TV_SHOW,
            cacheKey = "airing_today_tv_shows" 
        ),
        "on_the_air" to DataSourceConfig(
            id = "on_the_air",
            title = "On The Air", 
            endpoint = "shows/on_the_air",
            mediaType = MediaType.TV_SHOW,
            cacheKey = "on_the_air_tv_shows"
        )
    )
    
    fun getMovieDataSource(id: String): DataSourceConfig? = movieDataSources[id]
    fun getTvDataSource(id: String): DataSourceConfig? = tvDataSources[id]
    
    fun getAllMovieDataSources(): List<DataSourceConfig> = movieDataSources.values.toList()
    fun getAllTvDataSources(): List<DataSourceConfig> = tvDataSources.values.toList()
}