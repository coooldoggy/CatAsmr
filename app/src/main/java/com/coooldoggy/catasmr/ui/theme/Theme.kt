package com.coooldoggy.catasmr.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = Amber30,
    onPrimary = Color.White,
    primaryContainer = Amber90,
    onPrimaryContainer = Amber10,

    secondary = Cream30,
    onSecondary = Color.White,
    secondaryContainer = Cream90,
    onSecondaryContainer = Cream10,

    tertiary = Emerald30,
    onTertiary = Color.White,
    tertiaryContainer = Emerald90,
    onTertiaryContainer = Emerald10,

    error = Error,
    onError = Color.White,
    errorContainer = Error90,
    onErrorContainer = ErrorDark,

    background = Neutral90,
    onBackground = Neutral10,

    surface = Color.White,
    onSurface = Neutral10,
    surfaceVariant = Neutral80,
    onSurfaceVariant = Neutral40,

    outline = Neutral50
)

private val DarkColorScheme = darkColorScheme(
    primary = Amber50,
    onPrimary = Amber10,
    primaryContainer = Amber30,
    onPrimaryContainer = Amber90,

    secondary = Cream50,
    onSecondary = Cream10,
    secondaryContainer = Cream30,
    onSecondaryContainer = Cream90,

    tertiary = Emerald50,
    onTertiary = Emerald10,
    tertiaryContainer = Emerald30,
    onTertiaryContainer = Emerald90,

    error = Color(0xFFFF7B7B),
    onError = ErrorDark,
    errorContainer = Error,
    onErrorContainer = Color(0xFFFFCDD2),

    background = Neutral10,
    onBackground = Neutral90,

    surface = Neutral20,
    onSurface = Neutral90,
    surfaceVariant = Neutral30,
    onSurfaceVariant = Neutral80,

    outline = Neutral60
)

@Composable
fun CatAsmrTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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