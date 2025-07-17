package com.strmr.ai.di

import android.content.Context
import com.strmr.ai.data.database.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideStrmrDatabase(@ApplicationContext context: Context): StrmrDatabase {
        return StrmrDatabase.getDatabase(context)
    }
    
    @Provides
    fun provideMovieDao(database: StrmrDatabase): MovieDao = database.movieDao()
    
    @Provides
    fun provideTvShowDao(database: StrmrDatabase): TvShowDao = database.tvShowDao()
    
    @Provides
    fun provideCollectionDao(database: StrmrDatabase): CollectionDao = database.collectionDao()
    
    @Provides
    fun provideSeasonDao(database: StrmrDatabase): SeasonDao = database.seasonDao()
    
    @Provides
    fun provideEpisodeDao(database: StrmrDatabase): EpisodeDao = database.episodeDao()
    
    @Provides
    fun provideAccountDao(database: StrmrDatabase): AccountDao = database.accountDao()
    
    @Provides
    fun provideTraktUserProfileDao(database: StrmrDatabase): TraktUserProfileDao = database.traktUserProfileDao()
    
    @Provides
    fun provideTraktUserStatsDao(database: StrmrDatabase): TraktUserStatsDao = database.traktUserStatsDao()
    
    @Provides
    fun providePlaybackDao(database: StrmrDatabase): PlaybackDao = database.playbackDao()
    
    @Provides
    fun provideOmdbRatingsDao(database: StrmrDatabase): OmdbRatingsDao = database.omdbRatingsDao()

    @Provides
    fun provideTraktRatingsDao(database: StrmrDatabase): TraktRatingsDao = database.traktRatingsDao()
} 