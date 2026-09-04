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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
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
import com.generative.midi.sequencer.midi.TransportController
import com.generative.midi.sequencer.ui.theme.ActiveAccent
import com.generative.midi.sequencer.ui.theme.NoteBg

/**
 * Página TRACKS: gestión de los 4 tracks independientes. Cada track tiene su
 * propio canal MIDI y su propio patrón generado por separado. Permite habilitar/
 * deshabilitar, cambiar canal y generar/mutar cada uno.
 */
@Composable
fun TracksScreen(viewModel: MainViewModel) {
    val enabledList = viewModel.trackEnabled
    val channelList = viewModel.trackChannel
    val patternList = viewModel.trackPattern
    val pattern by viewModel.pattern.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("TRACKS", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(
            "4 tracks independientes con canal propio. Activa/desactiva y genera cada uno.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        for (i in 0 until TransportController.TRACK_COUNT) {
            val enabled by enabledList[i].collectAsStateWithLifecycle()
            val ch by channelList[i].collectAsStateWithLifecycle()
            val tp by patternList[i].collectAsStateWithLifecycle()
            val effectivePattern = if (i == 0) pattern else tp

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (enabled) NoteBg else NoteBg.copy(alpha = 0.4f))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "TRACK ${i + 1}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.weight(1f))
                    Text("ENABLED", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Switch(checked = enabled, onCheckedChange = { viewModel.setTrackEnabled(i, it) })
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("CH", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(1, 2, 3, 4, 5, 6).forEach { c ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (ch == c) ActiveAccent else MaterialTheme.colorScheme.surface)
                                    .clickable { viewModel.setTrackChannel(i, c) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("$c", fontSize = 12.sp, color = if (ch == c) Color.White else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { viewModel.generateTrack(i) }, modifier = Modifier.weight(1f), enabled = enabled) {
                        Text("GENERATE")
                    }
                    OutlinedButton(onClick = { viewModel.mutateTrack(i) }, modifier = Modifier.weight(1f), enabled = enabled) {
                        Text("MUTATE")
                    }
                }
                Spacer(Modifier.height(6.dp))
                val activeCount = effectivePattern.count { !it.isSilence }
                Text(
                    "${effectivePattern.size} pasos · $activeCount activos",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        Text(
            "Los tracks habilitados suenan simultáneamente, cada uno por su canal MIDI.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
    }
}
