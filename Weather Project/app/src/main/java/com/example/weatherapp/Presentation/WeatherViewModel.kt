package com.example.weatherapp.Presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherapp.Util.Result
import com.example.weatherapp.Data.remote.WeatherDto
import com.example.weatherapp.Domain.Repository.weather.WeatherIRepo
import kotlinx.coroutines.launch

class WeatherViewModel(
    private val weatherRepository: WeatherIRepo

): ViewModel() {
  var city by mutableStateOf("")
    private set

  var weatherState by mutableStateOf<Result<WeatherDto>>(Result.Idle)
    private set

  var Snackbarmsg by mutableStateOf<String?>(null)
    private set

    // update city
    fun updateCity(newCity: String) {
        city = newCity
    }

    fun searchcity(){
        if(city.isBlank()){
            Snackbarmsg="Please Enter a city"
            return
        }
        getweathercity(city)

    }

    private fun getweathercity(city:String){

        viewModelScope.launch {
            weatherState=Result.Loading
            try{
                val weatherData=weatherRepository.getweather(city)
                weatherState=Result.Success(weatherData)
            }catch(e:Exception){
                val errormsg=e.message?:"Unknown Error"
                weatherState=Result.Error(errormsg)
                Snackbarmsg=errormsg

            }
        }
    }

    fun clearSnackbarmsg(){
        Snackbarmsg=null
    }




  }