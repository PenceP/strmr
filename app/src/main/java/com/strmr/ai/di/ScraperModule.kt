package com.strmr.ai.di

import android.content.Context
import com.strmr.ai.data.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ScraperModule {
    @Provides
    @Singleton
    @Named("TorrentioRetrofit")
    fun provideTorrentioRetrofit(
        @Named("ScraperOkHttpClient") okHttpClient: OkHttpClient,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(TorrentioApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("CometRetrofit")
    fun provideCometRetrofit(
        @Named("ScraperOkHttpClient") okHttpClient: OkHttpClient,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(CometApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("PremiumizeRetrofit")
    fun providePremiumizeRetrofit(
        @Named("ScraperOkHttpClient") okHttpClient: OkHttpClient,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(PremiumizeApiService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("PremiumizeAuthRetrofit")
    fun providePremiumizeAuthRetrofit(
        @Named("ScraperOkHttpClient") okHttpClient: OkHttpClient,
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(PremiumizeAuthService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("ScraperOkHttpClient")
    fun provideScraperOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request =
                    chain.request().newBuilder()
                        .addHeader("User-Agent", "Strmr/1.0")
                        .build()
                chain.proceed(request)
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideTorrentioApiService(
        @Named("TorrentioRetrofit") retrofit: Retrofit,
    ): TorrentioApiService {
        return retrofit.create(TorrentioApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideCometApiService(
        @Named("CometRetrofit") retrofit: Retrofit,
    ): CometApiService {
        return retrofit.create(CometApiService::class.java)
    }

    @Provides
    @Singleton
    fun providePremiumizeApiService(
        @Named("PremiumizeRetrofit") retrofit: Retrofit,
    ): PremiumizeApiService {
        return retrofit.create(PremiumizeApiService::class.java)
    }

    @Provides
    @Singleton
    fun providePremiumizeAuthService(
        @Named("PremiumizeAuthRetrofit") retrofit: Retrofit,
    ): PremiumizeAuthService {
        return retrofit.create(PremiumizeAuthService::class.java)
    }

    @Provides
    @Singleton
    fun provideScraperRepository(
        @ApplicationContext context: Context,
        torrentioApi: TorrentioApiService,
        cometApi: CometApiService,
        premiumizeApi: PremiumizeApiService,
    ): ScraperRepository {
        return ScraperRepository(context, torrentioApi, cometApi, premiumizeApi)
    }
}
