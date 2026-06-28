package com.laralnet.agroai.photoanalysis.infrastructure.di

import com.laralnet.agroai.photoanalysis.domain.repository.AnalysisRepository
import com.laralnet.agroai.photoanalysis.infrastructure.repository.RoomAnalysisRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalysisDiModule {

    @Binds
    @Singleton
    abstract fun bindAnalysisRepository(impl: RoomAnalysisRepository): AnalysisRepository
}
