package com.laralnet.agroai.core.infrastructure.di

import android.content.Context
import androidx.work.WorkManager
import com.laralnet.agroai.aimodel.application.port.ModelDownloader
import com.laralnet.agroai.aimodel.domain.repository.AIModelRepository
import com.laralnet.agroai.aimodel.infrastructure.download.WorkManagerModelDownloader
import com.laralnet.agroai.aimodel.infrastructure.repository.RoomAIModelRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AIModelModule {

    @Binds
    @Singleton
    abstract fun bindAIModelRepository(impl: RoomAIModelRepository): AIModelRepository

    @Binds
    @Singleton
    abstract fun bindModelDownloader(impl: WorkManagerModelDownloader): ModelDownloader

    companion object {
        @Provides
        @Singleton
        fun provideWorkManager(@ApplicationContext context: Context): WorkManager =
            WorkManager.getInstance(context)
    }
}
