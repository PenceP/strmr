package com.strmr.ai.viewmodel

import com.strmr.ai.data.*
import com.strmr.ai.data.database.MovieEntity
import com.strmr.ai.data.database.TvShowEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.mockito.kotlin.*

@ExperimentalCoroutinesApi
class DetailsViewModelTest {
    
    private lateinit var movieRepository: MovieRepository
    private lateinit var tvShowRepository: TvShowRepository
    private lateinit var omdbRepository: OmdbRepository
    
    private lateinit var detailsViewModel: DetailsViewModel
    
    private val testDispatcher = UnconfinedTestDispatcher()
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        movieRepository = mock()
        tvShowRepository = mock()
        omdbRepository = mock()
        
        detailsViewModel = DetailsViewModel(
            movieRepository = movieRepository,
            tvShowRepository = tvShowRepository,
            omdbRepository = omdbRepository
        )
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun `init - has correct initial state`() = runTest {
        // Then - verify initial state
        assertNull(detailsViewModel.movie.value)
        assertNull(detailsViewModel.tvShow.value)
    }
    
    @Test
    fun `loadMovie - loads cached movie from repository`() = runTest {
        // Given
        val tmdbId = 123
        val cachedMovie = createTestMovieEntity(tmdbId, "Cached Movie")
        whenever(movieRepository.getMovieByTmdbId(tmdbId)).thenReturn(cachedMovie)
        
        // When
        detailsViewModel.loadMovie(tmdbId)
        
        // Then
        assertEquals(cachedMovie, detailsViewModel.movie.value)
        verify(movieRepository).getMovieByTmdbId(tmdbId)
        verify(movieRepository, never()).getOrFetchMovie(any())
    }
    
    @Test
    fun `loadMovie - fetches movie from API when not cached`() = runTest {
        // Given
        val tmdbId = 456
        val fetchedMovie = createTestMovieEntity(tmdbId, "Fetched Movie")
        whenever(movieRepository.getMovieByTmdbId(tmdbId)).thenReturn(null)
        whenever(movieRepository.getOrFetchMovie(tmdbId)).thenReturn(fetchedMovie)
        
        // When
        detailsViewModel.loadMovie(tmdbId)
        
        // Then
        assertEquals(fetchedMovie, detailsViewModel.movie.value)
        verify(movieRepository).getMovieByTmdbId(tmdbId)
        verify(movieRepository).getOrFetchMovie(tmdbId)
    }
    
    @Test
    fun `loadMovie - handles null result from repository`() = runTest {
        // Given
        val tmdbId = 789
        whenever(movieRepository.getMovieByTmdbId(tmdbId)).thenReturn(null)
        whenever(movieRepository.getOrFetchMovie(tmdbId)).thenReturn(null)
        
        // When
        detailsViewModel.loadMovie(tmdbId)
        
        // Then
        assertNull(detailsViewModel.movie.value)
        verify(movieRepository).getMovieByTmdbId(tmdbId)
        verify(movieRepository).getOrFetchMovie(tmdbId)
    }
    
    @Test
    fun `loadTvShow - loads cached tv show from repository`() = runTest {
        // Given
        val tmdbId = 123
        val cachedTvShow = createTestTvShowEntity(tmdbId, "Cached Show")
        whenever(tvShowRepository.getTvShowByTmdbId(tmdbId)).thenReturn(cachedTvShow)
        
        // When
        detailsViewModel.loadTvShow(tmdbId)
        
        // Then
        assertEquals(cachedTvShow, detailsViewModel.tvShow.value)
        verify(tvShowRepository).getTvShowByTmdbId(tmdbId)
        verify(tvShowRepository, never()).getOrFetchTvShow(any())
    }
    
    @Test
    fun `loadTvShow - fetches tv show from API when not cached`() = runTest {
        // Given
        val tmdbId = 456
        val fetchedTvShow = createTestTvShowEntity(tmdbId, "Fetched Show")
        whenever(tvShowRepository.getTvShowByTmdbId(tmdbId)).thenReturn(null)
        whenever(tvShowRepository.getOrFetchTvShow(tmdbId)).thenReturn(fetchedTvShow)
        
        // When
        detailsViewModel.loadTvShow(tmdbId)
        
        // Then
        assertEquals(fetchedTvShow, detailsViewModel.tvShow.value)
        verify(tvShowRepository).getTvShowByTmdbId(tmdbId)
        verify(tvShowRepository).getOrFetchTvShow(tmdbId)
    }
    
    @Test
    fun `loadTvShow - handles null result from repository`() = runTest {
        // Given
        val tmdbId = 789
        whenever(tvShowRepository.getTvShowByTmdbId(tmdbId)).thenReturn(null)
        whenever(tvShowRepository.getOrFetchTvShow(tmdbId)).thenReturn(null)
        
        // When
        detailsViewModel.loadTvShow(tmdbId)
        
        // Then
        assertNull(detailsViewModel.tvShow.value)
        verify(tvShowRepository).getTvShowByTmdbId(tmdbId)
        verify(tvShowRepository).getOrFetchTvShow(tmdbId)
    }
    
    @Test
    fun `getOmdbRatings - delegates to omdb repository`() = runTest {
        // Given
        val imdbId = "tt1234567"
        val expectedRatings = createTestOmdbRatings()
        whenever(omdbRepository.getOmdbRatings(imdbId)).thenReturn(expectedRatings)
        
        // When
        val result = detailsViewModel.getOmdbRatings(imdbId)
        
        // Then
        assertEquals(expectedRatings, result)
        verify(omdbRepository).getOmdbRatings(imdbId)
    }
    
    @Test
    fun `getSimilarMovies - delegates to movie repository`() = runTest {
        // Given
        val tmdbId = 123
        val expectedSimilar = listOf(createTestSimilarContent())
        whenever(movieRepository.getOrFetchSimilarMovies(tmdbId)).thenReturn(expectedSimilar)
        
        // When
        val result = detailsViewModel.getSimilarMovies(tmdbId)
        
        // Then
        assertEquals(expectedSimilar, result)
        verify(movieRepository).getOrFetchSimilarMovies(tmdbId)
    }
    
    @Test
    fun `getSimilarTvShows - delegates to tv show repository`() = runTest {
        // Given
        val tmdbId = 456
        val expectedSimilar = listOf(createTestSimilarContent())
        whenever(tvShowRepository.getOrFetchSimilarTvShows(tmdbId)).thenReturn(expectedSimilar)
        
        // When
        val result = detailsViewModel.getSimilarTvShows(tmdbId)
        
        // Then
        assertEquals(expectedSimilar, result)
        verify(tvShowRepository).getOrFetchSimilarTvShows(tmdbId)
    }
    
    @Test
    fun `getCollection - delegates to movie repository`() = runTest {
        // Given
        val collectionId = 789
        val expectedCollection = createTestCollectionEntity()
        whenever(movieRepository.getOrFetchCollection(collectionId)).thenReturn(expectedCollection)
        
        // When
        val result = detailsViewModel.getCollection(collectionId)
        
        // Then
        assertEquals(expectedCollection, result)
        verify(movieRepository).getOrFetchCollection(collectionId)
    }
    
    @Test
    fun `getSeasons - delegates to tv show repository`() = runTest {
        // Given
        val tmdbId = 123
        val expectedSeasons = listOf(createTestSeasonEntity())
        whenever(tvShowRepository.getOrFetchSeasons(tmdbId)).thenReturn(expectedSeasons)
        
        // When
        val result = detailsViewModel.getSeasons(tmdbId)
        
        // Then
        assertEquals(expectedSeasons, result)
        verify(tvShowRepository).getOrFetchSeasons(tmdbId)
    }
    
    @Test
    fun `getEpisodes - delegates to tv show repository`() = runTest {
        // Given
        val tmdbId = 123
        val season = 1
        val expectedEpisodes = listOf(createTestEpisodeEntity())
        whenever(tvShowRepository.getOrFetchEpisodes(tmdbId, season)).thenReturn(expectedEpisodes)
        
        // When
        val result = detailsViewModel.getEpisodes(tmdbId, season)
        
        // Then
        assertEquals(expectedEpisodes, result)
        verify(tvShowRepository).getOrFetchEpisodes(tmdbId, season)
    }
    
    @Test
    fun `getMovieTrailer - delegates to movie repository`() = runTest {
        // Given
        val tmdbId = 123
        val expectedTrailer = "https://youtube.com/watch?v=trailer123"
        whenever(movieRepository.getMovieTrailer(tmdbId)).thenReturn(expectedTrailer)
        
        // When
        val result = detailsViewModel.getMovieTrailer(tmdbId)
        
        // Then
        assertEquals(expectedTrailer, result)
        verify(movieRepository).getMovieTrailer(tmdbId)
    }
    
    @Test
    fun `getTvShowTrailer - delegates to tv show repository`() = runTest {
        // Given
        val tmdbId = 456
        val expectedTrailer = "https://youtube.com/watch?v=trailer456"
        whenever(tvShowRepository.getTvShowTrailer(tmdbId)).thenReturn(expectedTrailer)
        
        // When
        val result = detailsViewModel.getTvShowTrailer(tmdbId)
        
        // Then
        assertEquals(expectedTrailer, result)
        verify(tvShowRepository).getTvShowTrailer(tmdbId)
    }
    
    // Helper functions
    private fun createTestMovieEntity(tmdbId: Int = 123, title: String = "Test Movie"): MovieEntity {
        return MovieEntity(
            tmdbId = tmdbId,
            imdbId = "tt$tmdbId",
            title = title,
            posterUrl = "poster.jpg",
            backdropUrl = "backdrop.jpg",
            overview = "Test overview",
            rating = 8.5f,
            logoUrl = "logo.jpg",
            traktRating = null,
            traktVotes = null,
            year = 2024,
            releaseDate = "2024-01-01",
            runtime = 120,
            genres = listOf("Action", "Adventure"),
            cast = emptyList(),
            similar = emptyList(),
            belongsToCollection = null,
            trendingOrder = null,
            popularOrder = null,
            nowPlayingOrder = null,
            upcomingOrder = null,
            topRatedOrder = null,
            topMoviesWeekOrder = null,
            dataSourceOrders = emptyMap(),
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    private fun createTestTvShowEntity(tmdbId: Int = 456, title: String = "Test Show"): TvShowEntity {
        return TvShowEntity(
            tmdbId = tmdbId,
            imdbId = "tt$tmdbId",
            title = title,
            posterUrl = "poster.jpg",
            backdropUrl = "backdrop.jpg",
            overview = "Test overview",
            rating = 8.5f,
            logoUrl = "logo.jpg",
            traktRating = null,
            traktVotes = null,
            year = 2024,
            firstAirDate = "2024-01-01",
            lastAirDate = "2024-12-31",
            runtime = 45,
            genres = listOf("Drama", "Thriller"),
            cast = emptyList(),
            similar = emptyList(),
            trendingOrder = null,
            popularOrder = null,
            topRatedOrder = null,
            airingTodayOrder = null,
            onTheAirOrder = null,
            dataSourceOrders = emptyMap(),
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    private fun createTestOmdbRatings(): OmdbResponse? {
        return OmdbResponse(
            Title = "Test Movie",
            Year = "2024",
            Rated = "PG-13",
            Released = "01 Jan 2024",
            Runtime = "120 min",
            Genre = "Action, Adventure",
            Director = "Test Director",
            Writer = "Test Writer",
            Actors = "Test Actor 1, Test Actor 2",
            Plot = "Test plot",
            Language = "English",
            Country = "USA",
            Awards = "Test Awards",
            Poster = "poster.jpg",
            Ratings = emptyList(),
            Metascore = "80",
            imdbRating = "8.5",
            imdbVotes = "1,000,000",
            imdbID = "tt1234567",
            Type = "movie",
            DVD = "N/A",
            BoxOffice = "$100,000,000",
            Production = "Test Production",
            Website = "N/A",
            Response = "True"
        )
    }
    
    private fun createTestSimilarContent(): SimilarContent {
        return SimilarContent(
            tmdbId = 999,
            title = "Similar Content",
            posterUrl = "similar_poster.jpg",
            backdropUrl = "similar_backdrop.jpg",
            rating = 7.5f,
            year = 2023,
            mediaType = "movie"
        )
    }
    
    private fun createTestCollectionEntity(): com.strmr.ai.data.database.CollectionEntity {
        return com.strmr.ai.data.database.CollectionEntity(
            id = 789,
            name = "Test Collection",
            overview = "Test collection overview",
            posterPath = "collection_poster.jpg",
            backdropPath = "collection_backdrop.jpg",
            parts = emptyList(),
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    private fun createTestSeasonEntity(): com.strmr.ai.data.database.SeasonEntity {
        return com.strmr.ai.data.database.SeasonEntity(
            id = 0L,
            showTmdbId = 456,
            seasonNumber = 1,
            name = "Season 1",
            overview = "Test season overview",
            posterUrl = "season_poster.jpg",
            episodeCount = 10,
            airDate = "2024-01-01",
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    private fun createTestEpisodeEntity(): com.strmr.ai.data.database.EpisodeEntity {
        return com.strmr.ai.data.database.EpisodeEntity(
            id = 0L,
            showTmdbId = 456,
            seasonNumber = 1,
            episodeNumber = 1,
            name = "Episode 1",
            overview = "Test episode overview",
            stillUrl = "episode_still.jpg",
            airDate = "2024-01-01",
            runtime = 45,
            rating = 8.0f,
            lastUpdated = System.currentTimeMillis()
        )
    }
}