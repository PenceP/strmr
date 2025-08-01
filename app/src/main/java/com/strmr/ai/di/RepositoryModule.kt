package com.strmr.ai.di

import android.content.Context
import com.strmr.ai.data.*
import com.strmr.ai.data.database.*
import com.strmr.ai.data.database.TraktRatingsDao
import com.strmr.ai.data.mapper.AccountMapper
import com.strmr.ai.data.mapper.MovieMapper
import com.strmr.ai.data.mapper.TvShowMapper
import com.strmr.ai.data.repository.AccountRepositoryImpl
import com.strmr.ai.data.repository.MovieRepositoryImpl
import com.strmr.ai.data.repository.TvShowRepositoryImpl
import com.strmr.ai.domain.usecase.FetchLogoUseCase
import com.strmr.ai.utils.ImageUtils
import com.strmr.ai.utils.RemoteResourceLoader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    // Legacy repository providers (will be gradually phased out)
    @Provides
    @Singleton
    fun provideMovieRepository(
        movieDao: MovieDao,
        collectionDao: CollectionDao,
        traktApi: TraktApiService,
        tmdbApi: TmdbApiService,
        database: StrmrDatabase,
        traktRatingsDao: TraktRatingsDao,
        trailerService: TrailerService,
        tmdbEnrichmentService: TmdbEnrichmentService,
    ): MovieRepository {
        return MovieRepository(movieDao, collectionDao, traktApi, tmdbApi, database, traktRatingsDao, trailerService, tmdbEnrichmentService)
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
        trailerService: TrailerService,
        tmdbEnrichmentService: TmdbEnrichmentService,
    ): TvShowRepository {
        return TvShowRepository(tvShowDao, traktApiService, tmdbApiService, seasonDao, episodeDao, database, traktRatingsDao, trailerService, tmdbEnrichmentService)
    }

    @Provides
    @Singleton
    fun provideAccountRepository(
        accountDao: AccountDao,
        traktApiService: TraktApiService,
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
        traktUserStatsDao: TraktUserStatsDao,
    ): HomeRepository {
        return HomeRepository(context, playbackDao, continueWatchingDao, traktUserProfileDao, traktUserStatsDao)
    }

    @Provides
    @Singleton
    fun provideOmdbRepository(
        omdbRatingsDao: OmdbRatingsDao,
        omdbApiService: OmdbApiService,
    ): OmdbRepository {
        return OmdbRepository(omdbRatingsDao, omdbApiService)
    }

    @Provides
    @Singleton
    fun provideFetchLogoUseCase(tmdbApiService: TmdbApiService): FetchLogoUseCase {
        return FetchLogoUseCase(tmdbApiService)
    }

    @Provides
    @Singleton
    fun provideSearchRepository(
        traktApiService: TraktApiService,
        tmdbApiService: TmdbApiService,
    ): SearchRepository {
        return SearchRepository(traktApiService, tmdbApiService)
    }

    @Provides
    @Singleton
    fun provideDataSourceService(database: StrmrDatabase): DataSourceService {
        return DataSourceService(database)
    }

    @Provides
    @Singleton
    fun provideTmdbEnrichmentService(
        tmdbApiService: TmdbApiService,
        database: StrmrDatabase,
    ): TmdbEnrichmentService {
        return TmdbEnrichmentService(tmdbApiService, database)
    }

    @Provides
    @Singleton
    fun provideGenericTraktRepository(
        database: StrmrDatabase,
        traktApiService: TraktApiService,
        dataSourceService: DataSourceService,
        tmdbEnrichmentService: TmdbEnrichmentService,
        fetchLogoUseCase: FetchLogoUseCase,
    ): GenericTraktRepository {
        return GenericTraktRepository(database, traktApiService, dataSourceService, tmdbEnrichmentService, fetchLogoUseCase)
    }

    @Provides
    @Singleton
    fun provideTrailerService(tmdbApiService: TmdbApiService): TrailerService {
        return TrailerService(tmdbApiService)
    }

    @Provides
    @Singleton
    fun provideOnboardingService(
        @ApplicationContext context: Context,
        database: StrmrDatabase,
        genericRepository: GenericTraktRepository,
    ): OnboardingService {
        return OnboardingService(context, database, genericRepository)
    }

    // =================
    // CLEAN ARCHITECTURE DOMAIN REPOSITORIES
    // =================

    @Provides
    @Singleton
    fun provideDomainMovieRepository(
        legacyRepository: MovieRepository,
        movieMapper: MovieMapper,
    ): com.strmr.ai.domain.repository.MovieRepository {
        return MovieRepositoryImpl(legacyRepository, movieMapper)
    }

    @Provides
    @Singleton
    fun provideDomainTvShowRepository(
        legacyRepository: TvShowRepository,
        tvShowMapper: TvShowMapper,
    ): com.strmr.ai.domain.repository.TvShowRepository {
        return TvShowRepositoryImpl(legacyRepository, tvShowMapper)
    }

    @Provides
    @Singleton
    fun provideDomainAccountRepository(
        legacyRepository: AccountRepository,
        accountMapper: AccountMapper,
    ): com.strmr.ai.domain.repository.AccountRepository {
        return AccountRepositoryImpl(legacyRepository, accountMapper)
    }

    // =================
    // REMOTE RESOURCE LOADING (APK SIZE OPTIMIZATION)
    // =================

    @Provides
    @Singleton
    fun provideRemoteResourceLoader(
        @ApplicationContext context: Context,
    ): RemoteResourceLoader {
        return RemoteResourceLoader(context)
    }

    @Provides
    @Singleton
    fun provideImageUtils(
        @ApplicationContext context: Context,
        remoteResourceLoader: RemoteResourceLoader,
    ): ImageUtils {
        return ImageUtils(context, remoteResourceLoader)
    }
}
