package com.example.weatherapp.Data.RepoImpl

import com.example.weatherapp.Data.remote.WeatherDto
import com.example.weatherapp.Data.services.WeatherApiServices
import com.example.weatherapp.Domain.Repository.weather.WeatherIRepo
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter





class WeatherRepoImpl(
    private val apiService: WeatherApiServices
): WeatherIRepo {

    private val apikey = com.example.weatherapp.BuildConfig.OPEN_WEATHER_API_KEY



    override suspend fun getweather(city:String): WeatherDto {
        return try{
            apiService.client.get("/data/2.5/weather"){
                parameter("q",city)
                parameter("appid",apikey)
                parameter("units","metric")
            }.body()
        }catch(e: Exception){
            e.printStackTrace()
            throw Exception(
                "failed to fetch weather data:${e.localizedMessage}"
            )
        }
    }
}