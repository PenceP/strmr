package com.strmr.ai.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.strmr.ai.data.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
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

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor =
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient()
            .create()
    }
}
