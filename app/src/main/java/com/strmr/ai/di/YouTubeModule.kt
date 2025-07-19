package com.strmr.ai.di

import com.strmr.ai.data.YouTubeExtractor
import com.strmr.ai.data.youtube.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object YouTubeModule {
    
    @Provides
    @Singleton
    fun provideYouTubeInnerTubeClient(
        playerConfig: YouTubePlayerConfig
    ): YouTubeInnerTubeClient {
        return YouTubeInnerTubeClient(playerConfig)
    }
    
    @Provides
    @Singleton
    fun provideYouTubeSignatureDecryptor(): YouTubeSignatureDecryptor {
        return YouTubeSignatureDecryptor()
    }
    
    @Provides
    @Singleton
    fun provideYouTubeFormatSelector(): YouTubeFormatSelector {
        return YouTubeFormatSelector()
    }
    
    @Provides
    @Singleton
    fun provideYouTubeNParamTransformer(): YouTubeNParamTransformer {
        return YouTubeNParamTransformer()
    }
    
    @Provides
    @Singleton
    fun provideYouTubePlayerConfig(): YouTubePlayerConfig {
        return YouTubePlayerConfig()
    }
    
    @Provides
    @Singleton
    fun provideYouTubeStreamUrlResolver(
        signatureDecryptor: YouTubeSignatureDecryptor,
        nParamTransformer: YouTubeNParamTransformer
    ): YouTubeStreamUrlResolver {
        return YouTubeStreamUrlResolver(signatureDecryptor, nParamTransformer)
    }
    
    @Provides
    @Singleton
    fun provideYouTubeProxyExtractor(): YouTubeProxyExtractor {
        return YouTubeProxyExtractor()
    }
    
    @Provides
    @Singleton
    fun provideYouTubeExtractor(
        innerTubeClient: YouTubeInnerTubeClient,
        formatSelector: YouTubeFormatSelector,
        streamUrlResolver: YouTubeStreamUrlResolver,
        proxyExtractor: YouTubeProxyExtractor
    ): YouTubeExtractor {
        return YouTubeExtractor(innerTubeClient, formatSelector, streamUrlResolver, proxyExtractor)
    }
}