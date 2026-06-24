package com.laralnet.agroai.plantation.infrastructure.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.laralnet.agroai.plantation.infrastructure.persistence.entity.PlantationEntity
import com.laralnet.agroai.plantation.infrastructure.persistence.entity.PlantTypeEntity
import com.laralnet.agroai.plantation.infrastructure.persistence.entity.PlantationWithPlants
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantationDao {

    @Transaction
    @Query("SELECT * FROM plantations ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PlantationWithPlants>>

    @Transaction
    @Query("SELECT * FROM plantations WHERE id = :id")
    suspend fun findById(id: String): PlantationWithPlants?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlantation(plantation: PlantationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlants(plants: List<PlantTypeEntity>)

    @Query("DELETE FROM plant_types WHERE plantationId = :plantationId")
    suspend fun deletePlantsFor(plantationId: String)

    @Query("DELETE FROM plantations WHERE id = :id")
    suspend fun delete(id: String)

    @Transaction
    suspend fun upsert(plantation: PlantationEntity, plants: List<PlantTypeEntity>) {
        insertPlantation(plantation)
        deletePlantsFor(plantation.id)
        insertPlants(plants)
    }
}
