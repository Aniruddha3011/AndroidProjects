package com.example.weatherapp.Domain.Repository.weather

import com.example.weatherapp.Data.remote.WeatherDto

interface WeatherIRepo {
    suspend fun getweather(city:String): WeatherDto
}