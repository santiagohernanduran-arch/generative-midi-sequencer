package com.generative.midi.sequencer.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.generative.midi.sequencer.ui.theme.NoteBg

/**
 * Página CHORD MAP: Harmonic Wheel radial como interfaz primaria.
 *
 * Modo A (Intelligent): el centro es el acorde actual; alrededor, sugerencias
 * inteligentes con el color de su grado armónico y conexión por líneas.
 * Modo B (Degree): explora los grados diatónicos de la escala en círculo.
 */
@Composable
fun ChordMapScreen(viewModel: MainViewModel) {
    val root by viewModel.root.collectAsStateWithLifecycle()
    val scaleName by viewModel.scaleName.collectAsStateWithLifecycle()
    val currentChord by viewModel.currentChord.collectAsStateWithLifecycle()
    val history by viewModel.chordHistory.collectAsStateWithLifecycle()
    val suggestions by viewModel.mapSuggestions.collectAsStateWithLifecycle()
    val mapMore by viewModel.mapMore.collectAsStateWithLifecycle()
    val progression by viewModel.progression.collectAsStateWithLifecycle()
    val degreeMode by viewModel.mapDegreeMode.collectAsStateWithLifecycle()
    val currentDegree = viewModel.currentChordDegree
    val scaleDegrees = viewModel.mapScaleDegrees

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("CHORD MAP", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = { viewModel.toggleMapDegreeMode() }) {
                Text(if (degreeMode) "INTELLIGENT" else "DEGREE")
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "KEY: $root   SCALE: $scaleName   ${if (degreeMode) "· DEGREE MAP" else "· INTELLIGENT MAP"}",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))

        // Rueda principal (Modo A: Intelligent / Modo B: Degree) con transición.
        AnimatedContent(
            targetState = degreeMode,
            transitionSpec = {
                fadeIn(tween(160)) togetherWith fadeOut(tween(160))
            },
            label = "mapMode"
        ) { isDegree ->
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (isDegree) {
                    DegreeWheel(degrees = scaleDegrees, onSelect = { viewModel.selectDegreeChord(it) })
                } else {
                    HarmonicWheel(
                        currentName = currentChord?.name ?: "—",
                        currentDegree = currentDegree,
                        suggestions = suggestions,
                        onSelect = { viewModel.selectSuggestion(it.chord) }
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))

        // Controles.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            if (!degreeMode) {
                OutlinedButton(onClick = { viewModel.toggleChordMore() }, modifier = Modifier.weight(1f)) {
                    Text(if (mapMore) "TOP 5" else "MORE")
                }
            }
            OutlinedButton(onClick = { viewModel.clearChordHistory() }, modifier = Modifier.weight(1f)) {
                Text("CLEAR")
            }
        }
        Spacer(Modifier.height(10.dp))

        // Historial.
        if (history.isNotEmpty()) {
            Text("HISTORY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                history.joinToString(" → ") { it.name },
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(NoteBg)
                    .padding(10.dp)
            )
            Spacer(Modifier.height(10.dp))
        }

        // Acciones de progresión.
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { viewModel.addCurrentToProgression() }, modifier = Modifier.weight(1f)) {
                Text("ADD")
            }
            OutlinedButton(onClick = { viewModel.sendMapToChordSequencer() }, modifier = Modifier.weight(1f)) {
                Text("SEND")
            }
        }
        if (progression.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Progresión: ${progression.joinToString(" → ") { it.name }}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}
