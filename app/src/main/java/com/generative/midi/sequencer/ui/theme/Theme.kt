package com.generative.midi.sequencer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta oscura, minimalista y orientada a músicos.
private val DarkColors = darkColorScheme(
    primary = Color(0xFF4FC3F7),
    onPrimary = Color(0xFF001418),
    primaryContainer = Color(0xFF15202b),
    secondary = Color(0xFF9E9E9E),
    background = Color(0xFF0A0D10),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF12161B),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF1C232B),
    onSurfaceVariant = Color(0xFF9E9E9E),
    error = Color(0xFFCF6679)
)

/** Color de acento para el paso activo. */
val ActiveAccent = Color(0xFF4FC3F7)
val StepActiveBg = Color(0xFF2A3B4D)
val NoteBg = Color(0xFF1C232B)
val SilenceBg = Color(0xFF161B20)

@Composable
fun GenerativeMidiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content = content
    )
}
