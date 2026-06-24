package com.laralnet.agroai.plantation.infrastructure.persistence.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.laralnet.agroai.plantation.domain.model.PlantationType

@Entity(tableName = "plantations")
data class PlantationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: PlantationType,
    val latitude: Double?,
    val longitude: Double?,
    val address: String,
    val municipality: String,
    val province: String,
    val country: String,
    val municipalityCode: String,
    val areaSqMeters: Double,
    val notes: String,
    val googleAccountEmail: String?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(tableName = "plant_types", foreignKeys = [
    androidx.room.ForeignKey(
        entity = PlantationEntity::class,
        parentColumns = ["id"],
        childColumns = ["plantationId"],
        onDelete = androidx.room.ForeignKey.CASCADE
    )
])
data class PlantTypeEntity(
    @PrimaryKey val id: String,
    val plantationId: String,
    val name: String,
    val variety: String,
    val count: Int,
    val rowSpacingMeters: Double?,
    val plantSpacingMeters: Double?,
    val notes: String
)

data class PlantationWithPlants(
    @Embedded val plantation: PlantationEntity,
    @Relation(parentColumn = "id", entityColumn = "plantationId")
    val plants: List<PlantTypeEntity>
)
