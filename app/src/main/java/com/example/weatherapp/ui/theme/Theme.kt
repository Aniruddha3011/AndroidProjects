package com.example.weatherapp.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.neatroots.weatherapps.ui.theme.CloudWhite
import com.neatroots.weatherapps.ui.theme.DarkGray
import com.neatroots.weatherapps.ui.theme.DeepBlue
import com.neatroots.weatherapps.ui.theme.LightBlue
import com.neatroots.weatherapps.ui.theme.LightGray
import com.neatroots.weatherapps.ui.theme.MoonYellow
import com.neatroots.weatherapps.ui.theme.NightBlue
import com.neatroots.weatherapps.ui.theme.RainyBlue
import com.neatroots.weatherapps.ui.theme.SkyBlue
import com.neatroots.weatherapps.ui.theme.StormGray
import com.neatroots.weatherapps.ui.theme.SunnyYellow

private val DarkColorScheme = darkColorScheme(
    primary = DeepBlue,
    onPrimary = CloudWhite,
    primaryContainer = NightBlue,
    onPrimaryContainer = CloudWhite,
    secondary = MoonYellow,
    onSecondary = DarkGray,
    secondaryContainer = StormGray,
    onSecondaryContainer = MoonYellow,
    tertiary = RainyBlue,
    background = DarkGray,
    surface = DarkGray,
    surfaceVariant = StormGray
)

private val LightColorScheme = lightColorScheme(
    primary = SkyBlue,
    onPrimary = CloudWhite,
    primaryContainer = LightBlue,
    onPrimaryContainer = DeepBlue,
    secondary = SunnyYellow,
    onSecondary = DarkGray,
    secondaryContainer = LightGray,
    onSecondaryContainer = DarkGray,
    tertiary = RainyBlue,
    background = CloudWhite,
    surface = CloudWhite,
    surfaceVariant = LightGray
)

@Composable
fun WeatherAppsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}