package com.laralnet.agroai.core.infrastructure.di

import android.content.Context
import androidx.room.Room
import com.laralnet.agroai.aimodel.infrastructure.persistence.dao.AIModelDao
import com.laralnet.agroai.database.AppDatabase
import com.laralnet.agroai.plantation.infrastructure.persistence.dao.PlantationDao
import com.laralnet.agroai.treatment.infrastructure.persistence.dao.TreatmentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "agroai.db")
            .build()

    @Provides
    fun providePlantationDao(db: AppDatabase): PlantationDao = db.plantationDao()

    @Provides
    fun provideTreatmentDao(db: AppDatabase): TreatmentDao = db.treatmentDao()

    @Provides
    fun provideAIModelDao(db: AppDatabase): AIModelDao = db.aiModelDao()
}
