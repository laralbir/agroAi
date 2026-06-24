package com.laralnet.agroai.plantation.domain.model

import java.util.UUID

data class PlantType(
    val id: String = UUID.randomUUID().toString(),
    val plantationId: String,
    val name: String,
    val variety: String = "",
    val count: Int = 0,
    val rowSpacingMeters: Double? = null,
    val plantSpacingMeters: Double? = null,
    val notes: String = ""
)
