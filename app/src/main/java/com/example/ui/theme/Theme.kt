package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.preferences.AccentColor
import com.example.data.preferences.AppTheme

@Composable
fun CalcProTheme(
    appTheme: AppTheme = AppTheme.DARK,
    accentColorEnum: AccentColor = AccentColor.BLUE,
    content: @Composable () -> Unit
) {
    val darkTheme = when (appTheme) {
        AppTheme.DARK -> true
        AppTheme.LIGHT -> false
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val primaryAccent = when (accentColorEnum) {
        AccentColor.BLUE -> ElectricBlue
        AccentColor.CYAN -> CyanAccent
        AccentColor.PURPLE -> PurpleAccent
        AccentColor.GREEN -> GreenAccent
        AccentColor.ORANGE -> OrangeAccent
    }

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = primaryAccent,
            onPrimary = Color(0xFF0A0C10),
            primaryContainer = DarkSurfaceVariant,
            onPrimaryContainer = primaryAccent,
            secondary = CyanAccent,
            onSecondary = Color(0xFF0A0C10),
            secondaryContainer = DarkSurfaceVariant,
            onSecondaryContainer = CyanAccent,
            tertiary = PurpleAccent,
            background = DarkBackground,
            onBackground = TextPrimaryDark,
            surface = DarkSurface,
            onSurface = TextPrimaryDark,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = TextSecondaryDark,
            errorContainer = DarkSurfaceVariant,
            onErrorContainer = ClearKeyRed
        )
    } else {
        lightColorScheme(
            primary = primaryAccent,
            onPrimary = Color.White,
            secondary = CyanAccent,
            onSecondary = Color.White,
            tertiary = PurpleAccent,
            background = LightBackground,
            onBackground = TextPrimaryLight,
            surface = LightSurface,
            onSurface = TextPrimaryLight,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = TextSecondaryLight
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
