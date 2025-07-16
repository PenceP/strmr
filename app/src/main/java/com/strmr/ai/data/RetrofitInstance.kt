package com.strmr.ai.data

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.logging.HttpLoggingInterceptor
import com.strmr.ai.data.OmdbApiService
import com.strmr.ai.BuildConfig
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object RetrofitInstance {

    private val traktClient: OkHttpClient by lazy {
        val logging = okhttp3.logging.HttpLoggingInterceptor().apply {
            level = okhttp3.logging.HttpLoggingInterceptor.Level.HEADERS
        }
        
        // Create a trust manager that accepts all certificates (for development)
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("trakt-api-key", BuildConfig.TRAKT_API_KEY)
                    .addHeader("trakt-api-version", "2")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()
    }

    val trakt: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.trakt.tv/")
            .client(traktClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val tmdbClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJlMTM1NjVlZGRlYmMwZDA0OGJhNGQ1YTc1ZGNmMjUxZiIsIm5iZiI6MTQ2MzY2ODkwMC45NzcsInN1YiI6IjU3M2RkMGE0YzNhMzY4Mjk5ZTAwMDA0YiIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.mvvESyrWSEEeK6yq-WTitSNaIc4U6nWn8BVohEgfubk")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    val tmdb: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .client(tmdbClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val omdb: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.omdbapi.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val omdbApiService: OmdbApiService by lazy {
        omdb.create(OmdbApiService::class.java)
    }

    // Trakt Auth Service
    val traktAuth: TraktAuthService by lazy {
        trakt.create(TraktAuthService::class.java)
    }

    // Trakt API Service
    val traktApiService: TraktApiService by lazy {
        trakt.create(TraktApiService::class.java)
    }

    // Trakt Auth Manager
    val traktAuthManager: TraktAuthManager by lazy {
        TraktAuthManager(traktAuth)
    }

    // Create authenticated Trakt API service with Bearer token
    fun createAuthenticatedTraktService(accessToken: String): TraktAuthenticatedApiService {
        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("Authorization", "Bearer $accessToken")
                .header("trakt-api-key", BuildConfig.TRAKT_API_KEY)
                .method(original.method, original.body)
                .build()
            chain.proceed(request)
        }

        // Create a trust manager that accepts all certificates (for development)
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .build()

        val authenticatedRetrofit = Retrofit.Builder()
            .baseUrl("https://api.trakt.tv/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return authenticatedRetrofit.create(TraktAuthenticatedApiService::class.java)
    }

    // Create authenticated Trakt API service with Bearer token and optional logging
    fun createAuthenticatedTraktServiceWithLogging(accessToken: String, logging: HttpLoggingInterceptor? = null): TraktAuthenticatedApiService {
        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("Authorization", "Bearer ${accessToken.trim()}")
                .header("trakt-api-key", BuildConfig.TRAKT_API_KEY)
                .header("trakt-api-version", "2")
                .method(original.method, original.body)
                .build()
            chain.proceed(request)
        }

        // Create a trust manager that accepts all certificates (for development)
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())

        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
        if (logging != null) {
            clientBuilder.addInterceptor(logging)
        }
        val client = clientBuilder.build()

        val authenticatedRetrofit = Retrofit.Builder()
            .baseUrl("https://api.trakt.tv/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return authenticatedRetrofit.create(TraktAuthenticatedApiService::class.java)
    }
} 