package com.laralnet.agroai.core.infrastructure.di

import com.laralnet.agroai.aimodel.domain.repository.HuggingFaceAuthRepository
import com.laralnet.agroai.aimodel.infrastructure.oauth.HuggingFaceOAuthConfig
import com.laralnet.agroai.aimodel.infrastructure.oauth.HuggingFaceTokenService
import com.laralnet.agroai.aimodel.infrastructure.repository.DataStoreHuggingFaceAuthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HuggingFaceOAuthModule {

    @Binds
    @Singleton
    abstract fun bindHuggingFaceAuthRepository(
        impl: DataStoreHuggingFaceAuthRepository
    ): HuggingFaceAuthRepository

    companion object {

        @Provides
        @Singleton
        @Named("applicationScope")
        fun provideApplicationScope(): CoroutineScope =
            CoroutineScope(SupervisorJob() + Dispatchers.Default)

        @Provides
        @Singleton
        @Named("huggingface")
        fun provideHuggingFaceRetrofit(okHttpClient: OkHttpClient): Retrofit =
            Retrofit.Builder()
                .baseUrl(HuggingFaceOAuthConfig.BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

        @Provides
        @Singleton
        fun provideHuggingFaceTokenService(
            @Named("huggingface") retrofit: Retrofit
        ): HuggingFaceTokenService = retrofit.create(HuggingFaceTokenService::class.java)
    }
}
