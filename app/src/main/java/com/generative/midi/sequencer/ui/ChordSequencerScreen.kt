package com.generative.midi.sequencer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.generative.midi.sequencer.ui.theme.ActiveAccent
import com.generative.midi.sequencer.ui.theme.NoteBg

/**
 * Página CHORD SEQUENCER: permite definir una secuencia de acordes por paso.
 * Tocar una celda la rellena con un acorde sugerido; mantener limpia aún no se
 * implementa en esta versión (evitar complejidad no estable).
 */
@Composable
fun ChordSequencerScreen(viewModel: MainViewModel) {
    val chordSeqLength by viewModel.chordSeqLength.collectAsStateWithLifecycle()
    val chordSeq by viewModel.chordSeq.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("CHORD SEQUENCER", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))

        SectionLabel("LENGTH: $chordSeqLength")
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(4, 8, 16).forEach { l ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (chordSeqLength == l) ActiveAccent else NoteBg)
                        .clickable { viewModel.setChordSeqLength(l) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$l", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (chordSeqLength == l) Color.White else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        SectionLabel("SEQUENCE")
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            chordSeq.forEachIndexed { index, chord ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(NoteBg)
                            .clickable { viewModel.setChordAt(index, suggestedFor(viewModel, index)) }
                            .padding(10.dp)
                    ) {
                        Text(
                            chord?.name ?: "— tocar para asignar —",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (chord != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(ActiveAccent.copy(alpha = 0.6f))
                            .clickable { viewModel.duplicateChordAt(index) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text("DUP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { viewModel.randomizeChords() }, modifier = Modifier.weight(1f)) {
                Text("RANDOMIZE")
            }
            OutlinedButton(onClick = { viewModel.clearChords() }, modifier = Modifier.weight(1f)) {
                Text("CLEAR")
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { viewModel.sendChordSeqToPattern() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("SEND TO SEQUENCER (ACORDE REAL)")
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** Obtiene una sugerencia de acorde para una posición de la secuencia. */
private fun suggestedFor(viewModel: MainViewModel, index: Int): com.generative.midi.sequencer.midi.Harmony.Chord? {
    val suggestions = viewModel.chordSuggestions.value
    if (suggestions.isEmpty()) return null
    return suggestions[index % suggestions.size]
}
