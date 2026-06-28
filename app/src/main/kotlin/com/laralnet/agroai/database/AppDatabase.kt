package com.laralnet.agroai.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.laralnet.agroai.aimodel.infrastructure.persistence.AIModelEntity
import com.laralnet.agroai.aimodel.infrastructure.persistence.PromptTemplateEntity
import com.laralnet.agroai.aimodel.infrastructure.persistence.dao.AIModelDao
import com.laralnet.agroai.photoanalysis.infrastructure.persistence.dao.AnalysisRecordDao
import com.laralnet.agroai.photoanalysis.infrastructure.persistence.entity.AnalysisRecordEntity
import com.laralnet.agroai.plantation.infrastructure.persistence.dao.PlantationDao
import com.laralnet.agroai.plantation.infrastructure.persistence.entity.PlantationEntity
import com.laralnet.agroai.plantation.infrastructure.persistence.entity.PlantTypeEntity
import com.laralnet.agroai.treatment.infrastructure.persistence.dao.TreatmentDao
import com.laralnet.agroai.treatment.infrastructure.persistence.entity.TreatmentEntity
import com.laralnet.agroai.treatment.infrastructure.persistence.entity.TreatmentRecordEntity
import com.laralnet.agroai.weather.infrastructure.persistence.dao.WeatherDao
import com.laralnet.agroai.weather.infrastructure.persistence.entity.WeatherEntity

@Database(
    entities = [
        PlantationEntity::class,
        PlantTypeEntity::class,
        TreatmentEntity::class,
        TreatmentRecordEntity::class,
        AIModelEntity::class,
        PromptTemplateEntity::class,
        WeatherEntity::class,
        AnalysisRecordEntity::class
    ],
    version = 7,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun plantationDao(): PlantationDao
    abstract fun treatmentDao(): TreatmentDao
    abstract fun aiModelDao(): AIModelDao
    abstract fun weatherDao(): WeatherDao
    abstract fun analysisRecordDao(): AnalysisRecordDao
}
