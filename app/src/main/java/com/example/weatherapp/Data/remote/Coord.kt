package com.example.weatherapp.Data.remote

import kotlinx.serialization.Serializable

@Serializable
data class Coord(
    val lat: Double,
    val lon: Double
)