package com.laralnet.agroai.plantation.domain.model

enum class PlantationType(
    val labelResKey: String,
    val defaultIconEmoji: String
) {
    HUERTA("type_huerta", "🥦"),
    SECANO("type_secano", "🌾"),
    REGADIO("type_regadio", "💧"),
    VIÑEDO("type_vinedo", "🍇"),
    OLIVAR("type_olivar", "🫒"),
    FRUTAL("type_frutal", "🍎"),
    CITRICOS("type_citricos", "🍊"),
    INVERNADERO("type_invernadero", "🏡"),
    AROMATICAS("type_aromaticas", "🌿"),
    MEDICINALES("type_medicinales", "🌱"),
    CEREAL("type_cereal", "🌾"),
    LEGUMINOSAS("type_leguminosas", "🫘"),
    TUBERCULOS("type_tuberculos", "🥔"),
    FLORICULTURA("type_floricultura", "🌸"),
    VIVERO("type_vivero", "🪴"),
    BOSQUE("type_bosque", "🌲"),
    PRADERA("type_pradera", "🍀"),
    MONTANA("type_montana", "⛰️"),
    AGUACATE("type_aguacate", "🥑"),
    ARROZ("type_arroz", "🍚"),
    PLATANO("type_platano", "🍌"),
    OTRO("type_otro", "🌱")
}
