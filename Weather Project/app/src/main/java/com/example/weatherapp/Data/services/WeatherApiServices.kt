package com.example.weatherapp.Data.services

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class WeatherApiServices() {
    // Ktor setup
    val client = HttpClient(CIO) {
        install(ContentNegotiation){
            json(Json{
                ignoreUnknownKeys = true
                isLenient = true

            })
        }

        install(HttpTimeout){
            requestTimeoutMillis=15000 // 15 sec
            connectTimeoutMillis=15000
            socketTimeoutMillis=15000
        }

        defaultRequest {
            url{
                protocol= URLProtocol.HTTPS
                host="api.openweathermap.org"
            }
        }

    }
}