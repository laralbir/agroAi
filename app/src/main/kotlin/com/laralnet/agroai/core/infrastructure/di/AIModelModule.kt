package com.laralnet.agroai.core.infrastructure.di

import com.laralnet.agroai.aimodel.domain.repository.AIModelRepository
import com.laralnet.agroai.aimodel.infrastructure.repository.RoomAIModelRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AIModelModule {

    @Binds
    @Singleton
    abstract fun bindAIModelRepository(impl: RoomAIModelRepository): AIModelRepository
}
