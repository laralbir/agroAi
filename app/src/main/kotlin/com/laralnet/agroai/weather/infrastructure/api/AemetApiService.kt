package com.laralnet.agroai.weather.infrastructure.api

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface AemetApiService {

    @GET("prediccion/especifica/municipio/diaria/{codMunicipio}")
    suspend fun getDailyForecast(
        @Path("codMunicipio") municipalityCode: String,
        @Header("api_key") apiKey: String
    ): AemetForecastResponse

    @GET("prediccion/especifica/municipio/horaria/{codMunicipio}")
    suspend fun getHourlyForecast(
        @Path("codMunicipio") municipalityCode: String,
        @Header("api_key") apiKey: String
    ): AemetForecastResponse
}

data class AemetForecastResponse(
    val estado: Int = 0,
    val datos: String = "",
    val metadatos: String = "",
    val descripcion: String = ""
)

data class AemetForecastData(
    val nombre: String = "",
    val provincia: String = "",
    val prediccion: AemetPrediccion = AemetPrediccion()
)

data class AemetPrediccion(
    val dia: List<AemetDia> = emptyList()
)

data class AemetDia(
    val fecha: String = "",
    val probPrecipitacion: List<AemetPeriodo> = emptyList(),
    val cotaNieveProv: List<AemetPeriodo> = emptyList(),
    val estadoCielo: List<AemetEstadoCielo> = emptyList(),
    val viento: List<AemetViento> = emptyList(),
    val temperatura: AemetTemperatura = AemetTemperatura(),
    val humedadRelativa: AemetTemperatura = AemetTemperatura(),
    val uvMax: Int = 0
)

data class AemetPeriodo(val periodo: String = "", val value: String = "")
data class AemetEstadoCielo(val periodo: String = "", val value: String = "", val descripcion: String = "")
data class AemetViento(val periodo: String = "", val direccion: String = "", val velocidad: Int = 0)
data class AemetTemperatura(val maxima: Int = 0, val minima: Int = 0)
