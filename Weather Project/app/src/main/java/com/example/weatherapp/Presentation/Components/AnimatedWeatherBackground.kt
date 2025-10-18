package com.example.weatherapp.Presentation.Components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.neatroots.weatherapps.ui.theme.CloudWhite
import com.neatroots.weatherapps.ui.theme.LightBlue
import com.neatroots.weatherapps.ui.theme.SkyBlue

@Composable
fun AnimatedWeatherBackground(
    modifier: Modifier = Modifier,
    content: @Composable () ->Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label="Weather_background_transition")

    val animationvalue by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(10000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label="color_animation"
    )

//    Box(
//      modifier= Modifier
//          .fillMaxSize()
//          .background(
//              brush = Brush.radialGradient(
//                  colors=listOf(
//                      SkyBlue,
//                      LightBlue,
//                      CloudWhite
//                  ),
//                  center=androidx.compose.ui.geometry.Offset(0.5f,0.5f),
//                  radius = 300f+(animationvalue+1500f)
//              )
//          )
//    ){
//        content()
//    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        SkyBlue,
                        LightBlue,
                        CloudWhite
                    ),
                    center = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                    radius = 300f + (animationvalue + 1500f)
                )
            )
    ) {
        content()
    }





}