package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DeveloperColorScheme = darkColorScheme(
  primary = GitOrange,
  secondary = TechBlue,
  tertiary = SuccessGreen,
  background = DarkBackground,
  surface = CardSurface,
  onPrimary = Color.White,
  onSecondary = Color.Black,
  onBackground = TextLight,
  onSurface = TextLight,
  surfaceVariant = BorderSlate,
  onSurfaceVariant = TextMuted,
  error = WarningRed
)

private val DarkColorScheme =
  darkColorScheme(primary = Purple80, secondary = PurpleGrey80, tertiary = Pink80)

private val LightColorScheme =
  lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark developer theme for consistent terminal aesthetic
  dynamicColor: Boolean = false, // Disable dynamic colors to keep Git/developer brand coloring intact
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DeveloperColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
