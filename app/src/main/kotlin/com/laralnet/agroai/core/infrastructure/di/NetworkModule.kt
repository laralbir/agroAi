package com.laralnet.agroai.core.infrastructure.di

import com.laralnet.agroai.location.infrastructure.nominatim.NominatimApiService
import com.laralnet.agroai.weather.infrastructure.api.AemetApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
        .build()

    // --- AEMET ---

    @Provides
    @Singleton
    @Named("aemet")
    fun provideAemetRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://opendata.aemet.es/openapi/api/")
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideAemetApi(@Named("aemet") retrofit: Retrofit): AemetApiService =
        retrofit.create(AemetApiService::class.java)

    @Provides
    @Named("aemet_api_key")
    fun provideAemetApiKey(): String = ""

    // --- Nominatim (OpenStreetMap geocoding — no API key needed) ---
    // Usage policy: User-Agent required, max 1 req/s (debounce handled in VM)

    @Provides
    @Singleton
    @Named("nominatim")
    fun provideNominatimRetrofit(okHttpClient: OkHttpClient): Retrofit {
        val client = okHttpClient.newBuilder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", "AgroAI/0.1.0 (laralbir@gmail.com)")
                    .header("Accept-Language", "es,en")
                    .build()
                chain.proceed(req)
            }
            .build()
        return Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideNominatimApi(@Named("nominatim") retrofit: Retrofit): NominatimApiService =
        retrofit.create(NominatimApiService::class.java)
}
