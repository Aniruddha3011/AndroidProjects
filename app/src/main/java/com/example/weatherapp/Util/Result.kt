package com.example.weatherapp.Util

sealed class Result<out T>{
    data object Loading:Result<Nothing>()

    data object Idle:Result<Nothing>()

    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
}