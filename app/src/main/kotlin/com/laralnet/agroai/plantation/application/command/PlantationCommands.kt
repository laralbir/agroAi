package com.laralnet.agroai.plantation.application.command

import com.laralnet.agroai.plantation.domain.model.Location
import com.laralnet.agroai.plantation.domain.model.PlantType
import com.laralnet.agroai.plantation.domain.model.PlantationType

data class CreatePlantationCommand(
    val name: String,
    val type: PlantationType,
    val location: Location,
    val areaSqMeters: Double,
    val plants: List<PlantType> = emptyList(),
    val notes: String = "",
    val googleAccountEmail: String? = null
)

data class UpdatePlantationCommand(
    val id: String,
    val name: String,
    val type: PlantationType,
    val location: Location,
    val areaSqMeters: Double,
    val plants: List<PlantType>,
    val notes: String,
    val googleAccountEmail: String?
)

data class DeletePlantationCommand(val id: String)
