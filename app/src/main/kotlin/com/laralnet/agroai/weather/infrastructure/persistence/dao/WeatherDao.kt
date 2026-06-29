package com.laralnet.agroai.weather.infrastructure.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.laralnet.agroai.weather.infrastructure.persistence.entity.WeatherEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {

    @Query("SELECT * FROM weather_cache WHERE id = :id")
    fun observe(id: String): Flow<WeatherEntity?>

    @Query("SELECT * FROM weather_cache WHERE id = :id")
    suspend fun getOnce(id: String): WeatherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WeatherEntity)

    @Query("DELETE FROM weather_cache WHERE fetchedAt < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long)
}
