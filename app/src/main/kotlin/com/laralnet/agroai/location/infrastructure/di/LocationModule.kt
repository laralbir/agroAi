package com.laralnet.agroai.location.infrastructure.di

import com.laralnet.agroai.location.domain.repository.LocationRepository
import com.laralnet.agroai.location.infrastructure.repository.NominatimLocationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {
    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: NominatimLocationRepository): LocationRepository
}
