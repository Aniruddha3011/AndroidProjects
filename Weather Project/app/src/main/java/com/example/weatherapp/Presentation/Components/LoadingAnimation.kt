package com.example.weatherapp.Presentation.Components

import android.R
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun LoadingAnimation(
    modifier: Modifier = Modifier,
    Circlecolor: Color = MaterialTheme.colorScheme.primary,
    circleSize: Dp = 12.dp,
    animationDelay: Int = 400,
    initialAlpha: Float = 0.3f
   ) {
    // 3 dot or cicles

    val circles=listOf(
        remember { Animatable(initialValue = initialAlpha) },
        remember { Animatable(initialValue = initialAlpha) },
        remember { Animatable(initialValue = initialAlpha) }
    )

    circles.forEachIndexed { index,animatable ->
        LaunchedEffect(key1=animatable) {
            delay(timeMillis = (animationDelay / circles.size).toLong() * index)

            animatable.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation= keyframes {
                        durationMillis = animationDelay
                        initialAlpha at 0 with LinearEasing
                        1f at animationDelay / 2 with LinearEasing
                        initialAlpha at animationDelay with LinearEasing
                    },
                    repeatMode = RepeatMode.Restart
                )
            )
        }
    }

    // container for circles

    Row(modifier= Modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically){

        //adding eacj circlrs

        circles.forEachIndexed { index, animatable ->
            val circleAlpha by animatable.asState()

            Box(
              modifier= Modifier
                  .size(circleSize)
                  .clip(CircleShape)
                  .background(Circlecolor.copy(alpha = circleAlpha))

            )
            if (index!=circles.lastIndex){
                Spacer(modifier=Modifier.width(6.dp))
            }
        }
    }

}