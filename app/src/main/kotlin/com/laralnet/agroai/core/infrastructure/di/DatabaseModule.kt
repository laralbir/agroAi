package com.laralnet.agroai.core.infrastructure.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE ai_models ADD COLUMN last_error TEXT")
    }
}

// Purge ai_models rows whose variant name no longer exists in the ModelVariant enum.
// Rows with stale enum values (e.g. GEMMA3_4B) crash Room's auto-generated converter.
private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "DELETE FROM ai_models WHERE variant NOT IN ('GEMMA3_1B', 'GEMMA4_2B')"
        )
    }
}

// GEMMA4_2B used a .web.task (WebAssembly) file incompatible with the Android MediaPipe SDK.
// Remove it from the DB so Room no longer tries to map it to a non-existent enum entry.
private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DELETE FROM ai_models WHERE variant = 'GEMMA4_2B'")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "agroai.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()

    @Provides
    fun providePlantationDao(db: AppDatabase): PlantationDao = db.plantationDao()

    @Provides
    fun provideTreatmentDao(db: AppDatabase): TreatmentDao = db.treatmentDao()

    @Provides
    fun provideAIModelDao(db: AppDatabase): AIModelDao = db.aiModelDao()
}
