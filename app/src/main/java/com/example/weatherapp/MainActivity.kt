package com.example.weatherapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.weatherapp.Data.RepoImpl.WeatherRepoImpl
import com.example.weatherapp.Data.services.WeatherApiServices
import com.example.weatherapp.Domain.Repository.weather.WeatherIRepo
import com.example.weatherapp.Presentation.WeatherScreen
import com.example.weatherapp.Presentation.WeatherViewModel
import com.example.weatherapp.ui.theme.WeatherAppsTheme


class MainActivity : ComponentActivity() {
    private val weatherApiService by lazy {
        WeatherApiServices()
    }

    private val weatherRepository: WeatherIRepo by  lazy {
        WeatherRepoImpl(weatherApiService)
    }

    private val viewModel: WeatherViewModel by lazy {
        WeatherViewModel(weatherRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeatherAppsTheme {
                WeatherScreen(viewModel=viewModel)
            }
        }
    }
}

