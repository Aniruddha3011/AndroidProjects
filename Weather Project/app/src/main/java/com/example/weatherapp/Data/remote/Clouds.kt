package com.example.weatherapp.Data.remote

import kotlinx.serialization.Serializable

@Serializable
data class Clouds(
    val all: Int
)