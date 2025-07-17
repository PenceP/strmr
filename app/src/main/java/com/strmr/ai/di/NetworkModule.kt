package com.strmr.ai.di

import com.strmr.ai.data.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    @Provides
    @Singleton
    fun provideTraktApiService(): TraktApiService {
        return RetrofitInstance.trakt.create(TraktApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideTmdbApiService(): TmdbApiService {
        return RetrofitInstance.tmdb.create(TmdbApiService::class.java)
    }
    
    @Provides
    @Singleton
    fun provideTraktAuthService(): TraktAuthService {
        return RetrofitInstance.traktAuth
    }
    
    @Provides
    @Singleton
    fun provideOmdbApiService(): OmdbApiService {
        return RetrofitInstance.omdbApiService
    }
} 