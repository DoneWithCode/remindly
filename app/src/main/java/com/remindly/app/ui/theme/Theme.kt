package com.remindly.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.remindly.app.data.Category
import com.remindly.app.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = IndigoPrimary,
    onPrimary = Color.White,
    secondary = TealAccent,
    onSecondary = Color.White,
    background = BackgroundLight,
    surface = SurfaceLight,
    error = OverdueRed
)

private val DarkColors = darkColorScheme(
    primary = IndigoPrimaryDark,
    onPrimary = Color(0xFF1A237E),
    secondary = TealAccentDark,
    onSecondary = Color(0xFF00382F),
    background = BackgroundDark,
    surface = SurfaceDark,
    error = OverdueRedDark
)

@Composable
fun RemindlyTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = Typography(),
        content = content
    )
}

fun Category.color(): Color = when (this) {
    Category.GENERAL -> CatGeneral
    Category.WORK -> CatWork
    Category.PERSONAL -> CatPersonal
    Category.HEALTH -> CatHealth
    Category.SHOPPING -> CatShopping
}
