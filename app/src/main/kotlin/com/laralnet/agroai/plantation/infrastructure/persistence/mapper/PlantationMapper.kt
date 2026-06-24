package com.laralnet.agroai.plantation.infrastructure.persistence.mapper

import com.laralnet.agroai.plantation.domain.model.Location
import com.laralnet.agroai.plantation.domain.model.Plantation
import com.laralnet.agroai.plantation.domain.model.PlantType
import com.laralnet.agroai.plantation.infrastructure.persistence.entity.PlantationEntity
import com.laralnet.agroai.plantation.infrastructure.persistence.entity.PlantationWithPlants
import com.laralnet.agroai.plantation.infrastructure.persistence.entity.PlantTypeEntity
import java.time.Instant

fun PlantationWithPlants.toDomain(): Plantation = Plantation.reconstitute(
    id = plantation.id,
    name = plantation.name,
    type = plantation.type,
    location = Location(
        latitude = plantation.latitude,
        longitude = plantation.longitude,
        address = plantation.address,
        municipality = plantation.municipality,
        province = plantation.province,
        country = plantation.country,
        municipalityCode = plantation.municipalityCode
    ),
    areaSqMeters = plantation.areaSqMeters,
    plants = plants.map { it.toDomain() },
    notes = plantation.notes,
    googleAccountEmail = plantation.googleAccountEmail,
    createdAt = Instant.ofEpochMilli(plantation.createdAt),
    updatedAt = Instant.ofEpochMilli(plantation.updatedAt)
)

fun PlantTypeEntity.toDomain(): PlantType = PlantType(
    id = id,
    plantationId = plantationId,
    name = name,
    variety = variety,
    count = count,
    rowSpacingMeters = rowSpacingMeters,
    plantSpacingMeters = plantSpacingMeters,
    notes = notes
)

fun Plantation.toEntity(): PlantationEntity = PlantationEntity(
    id = id,
    name = name,
    type = type,
    latitude = location.latitude,
    longitude = location.longitude,
    address = location.address,
    municipality = location.municipality,
    province = location.province,
    country = location.country,
    municipalityCode = location.municipalityCode,
    areaSqMeters = areaSqMeters,
    notes = notes,
    googleAccountEmail = googleAccountEmail,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli()
)

fun PlantType.toEntity(): PlantTypeEntity = PlantTypeEntity(
    id = id,
    plantationId = plantationId,
    name = name,
    variety = variety,
    count = count,
    rowSpacingMeters = rowSpacingMeters,
    plantSpacingMeters = plantSpacingMeters,
    notes = notes
)
