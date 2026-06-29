package com.laralnet.agroai.core.infrastructure.di

import com.laralnet.agroai.aimodel.domain.repository.WorkerRunRepository
import com.laralnet.agroai.aimodel.infrastructure.repository.RoomWorkerRunRepository
import com.laralnet.agroai.calendar.domain.repository.CalendarRepository
import com.laralnet.agroai.calendar.infrastructure.google.GoogleCalendarRepository
import com.laralnet.agroai.plantation.domain.repository.PlantationRepository
import com.laralnet.agroai.plantation.infrastructure.repository.RoomPlantationRepository
import com.laralnet.agroai.treatment.domain.repository.TreatmentRepository
import com.laralnet.agroai.treatment.infrastructure.repository.RoomTreatmentRepository
import com.laralnet.agroai.weather.domain.repository.WeatherRepository
import com.laralnet.agroai.weather.infrastructure.api.OpenMeteoWeatherRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPlantationRepository(impl: RoomPlantationRepository): PlantationRepository

    @Binds
    @Singleton
    abstract fun bindTreatmentRepository(impl: RoomTreatmentRepository): TreatmentRepository

    @Binds
    @Singleton
    abstract fun bindCalendarRepository(impl: GoogleCalendarRepository): CalendarRepository

    @Binds
    @Singleton
    abstract fun bindWeatherRepository(impl: OpenMeteoWeatherRepository): WeatherRepository

    @Binds
    @Singleton
    abstract fun bindWorkerRunRepository(impl: RoomWorkerRunRepository): WorkerRunRepository
}
