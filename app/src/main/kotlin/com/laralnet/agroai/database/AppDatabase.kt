package com.laralnet.agroai.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.laralnet.agroai.action.infrastructure.persistence.dao.PlantationActionDao
import com.laralnet.agroai.action.infrastructure.persistence.entity.PlantationActionEntity
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
        AnalysisRecordEntity::class,
        PlantationActionEntity::class
    ],
    version = 8,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun plantationDao(): PlantationDao
    abstract fun treatmentDao(): TreatmentDao
    abstract fun aiModelDao(): AIModelDao
    abstract fun weatherDao(): WeatherDao
    abstract fun analysisRecordDao(): AnalysisRecordDao
    abstract fun plantationActionDao(): PlantationActionDao

    companion object {
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS plantation_actions (
                        id TEXT NOT NULL PRIMARY KEY,
                        plantationId TEXT NOT NULL,
                        plantTypeId TEXT,
                        actionType TEXT NOT NULL,
                        title TEXT NOT NULL,
                        notes TEXT NOT NULL,
                        scheduledAt INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        calendarEventId INTEGER,
                        calendarAccountEmail TEXT,
                        source TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
