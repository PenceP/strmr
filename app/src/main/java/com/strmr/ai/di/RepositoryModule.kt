package com.strmr.ai.di

import android.content.Context
import com.strmr.ai.data.*
import com.strmr.ai.data.database.*
import com.strmr.ai.data.database.TraktRatingsDao
import com.strmr.ai.domain.usecase.FetchLogoUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    
    @Provides
    @Singleton
    fun provideMovieRepository(
        movieDao: MovieDao,
        collectionDao: CollectionDao,
        traktApi: TraktApiService,
        tmdbApi: TmdbApiService,
        database: StrmrDatabase,
        traktRatingsDao: TraktRatingsDao,
        trailerService: TrailerService
    ): MovieRepository {
        return MovieRepository(movieDao, collectionDao, traktApi, tmdbApi, database, traktRatingsDao, trailerService)
    }
    
    @Provides
    @Singleton
    fun provideTvShowRepository(
        tvShowDao: TvShowDao,
        traktApiService: TraktApiService,
        tmdbApiService: TmdbApiService,
        seasonDao: SeasonDao,
        episodeDao: EpisodeDao,
        database: StrmrDatabase,
        traktRatingsDao: TraktRatingsDao,
        trailerService: TrailerService
    ): TvShowRepository {
        return TvShowRepository(tvShowDao, traktApiService, tmdbApiService, seasonDao, episodeDao, database, traktRatingsDao, trailerService)
    }
    
    @Provides
    @Singleton
    fun provideAccountRepository(
        accountDao: AccountDao,
        traktApiService: TraktApiService
    ): AccountRepository {
        return AccountRepository(accountDao, traktApiService)
    }
    
    @Provides
    @Singleton
    fun provideHomeRepository(
        @ApplicationContext context: Context,
        playbackDao: PlaybackDao,
        continueWatchingDao: ContinueWatchingDao,
        traktUserProfileDao: TraktUserProfileDao,
        traktUserStatsDao: TraktUserStatsDao
    ): HomeRepository {
        return HomeRepository(context, playbackDao, continueWatchingDao, traktUserProfileDao, traktUserStatsDao)
    }
    
    @Provides
    @Singleton
    fun provideOmdbRepository(
        omdbRatingsDao: OmdbRatingsDao,
        omdbApiService: OmdbApiService
    ): OmdbRepository {
        return OmdbRepository(omdbRatingsDao, omdbApiService)
    }
    
    @Provides
    @Singleton
    fun provideFetchLogoUseCase(
        tmdbApiService: TmdbApiService
    ): FetchLogoUseCase {
        return FetchLogoUseCase(tmdbApiService)
    }
    
    @Provides
    @Singleton
    fun provideSearchRepository(
        traktApiService: TraktApiService,
        tmdbApiService: TmdbApiService
    ): SearchRepository {
        return SearchRepository(traktApiService, tmdbApiService)
    }
    
    @Provides
    @Singleton
    fun provideGenericTraktRepository(
        database: StrmrDatabase,
        traktApiService: TraktApiService,
        tmdbApiService: TmdbApiService,
        fetchLogoUseCase: FetchLogoUseCase
    ): GenericTraktRepository {
        return GenericTraktRepository(database, traktApiService, tmdbApiService, fetchLogoUseCase)
    }
    
    @Provides
    @Singleton
    fun provideTrailerService(
        tmdbApiService: TmdbApiService
    ): TrailerService {
        return TrailerService(tmdbApiService)
    }
} 