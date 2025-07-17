package com.strmr.ai.di

import android.content.Context
import com.strmr.ai.data.*
import com.strmr.ai.data.database.*
import com.strmr.ai.data.database.TraktRatingsDao
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
        traktRatingsDao: TraktRatingsDao
    ): MovieRepository {
        return MovieRepository(movieDao, collectionDao, traktApi, tmdbApi, database, traktRatingsDao)
    }
    
    @Provides
    @Singleton
    fun provideTvShowRepository(
        tvShowDao: TvShowDao,
        traktApiService: TraktApiService,
        tmdbApiService: TmdbApiService,
        seasonDao: SeasonDao,
        episodeDao: EpisodeDao,
        database: StrmrDatabase
    ): TvShowRepository {
        return TvShowRepository(tvShowDao, traktApiService, tmdbApiService, seasonDao, episodeDao, database)
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
        traktUserProfileDao: TraktUserProfileDao,
        traktUserStatsDao: TraktUserStatsDao
    ): HomeRepository {
        return HomeRepository(context, playbackDao, traktUserProfileDao, traktUserStatsDao)
    }
    
    @Provides
    @Singleton
    fun provideOmdbRepository(
        omdbRatingsDao: OmdbRatingsDao,
        omdbApiService: OmdbApiService
    ): OmdbRepository {
        return OmdbRepository(omdbRatingsDao, omdbApiService)
    }
} 