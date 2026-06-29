package com.laralnet.agroai.action.infrastructure.di

import com.laralnet.agroai.action.domain.repository.PlantationActionRepository
import com.laralnet.agroai.action.infrastructure.repository.RoomPlantationActionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlantationActionModule {

    @Binds
    @Singleton
    abstract fun bindPlantationActionRepository(
        impl: RoomPlantationActionRepository
    ): PlantationActionRepository
}
