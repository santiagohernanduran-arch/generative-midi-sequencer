package com.generative.midi.sequencer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import com.generative.midi.sequencer.ui.theme.ActiveAccent
import com.generative.midi.sequencer.ui.theme.NoteBg

private val ARP_MODES = listOf("UP", "DOWN", "UP+DOWN", "ANYORDER", "NOTEORDER")

/**
 * Página ARPEGGIATOR: configuración del arpegiador. El motor usará la escala y
 * raíz del transporte. En esta versión la ejecución se deja preparada.
 */
@Composable
fun ArpeggiatorScreen(viewModel: MainViewModel) {
    val arpOn by viewModel.arpOn.collectAsStateWithLifecycle()
    val arpMode by viewModel.arpMode.collectAsStateWithLifecycle()
    val arpRate by viewModel.arpRate.collectAsStateWithLifecycle()
    val arpOctaveRange by viewModel.arpOctaveRange.collectAsStateWithLifecycle()
    val arpGate by viewModel.arpGate.collectAsStateWithLifecycle()
    val arpPatternLength by viewModel.arpPatternLength.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("ARPEGGIATOR", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ENABLE ARPEGGIATOR", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Switch(checked = arpOn, onCheckedChange = { viewModel.setArpOn(it) })
        }
        Spacer(Modifier.height(16.dp))

        SectionLabel("MODE")
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            ARP_MODES.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { m ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (arpMode == m) ActiveAccent else NoteBg)
                                .clickable { viewModel.setArpMode(m) }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(m, fontSize = 11.sp, color = if (arpMode == m) Color.White else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        SectionLabel("RATE")
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf("1/16", "1/8", "1/4", "1/2").forEach { r ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (arpRate == r) ActiveAccent else NoteBg)
                        .clickable { viewModel.setArpRate(r) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(r, fontSize = 12.sp, color = if (arpRate == r) Color.White else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        SectionLabel("OCTAVE RANGE: $arpOctaveRange")
        SliderControl("OCTAVE RANGE", arpOctaveRange, 1..4) { viewModel.setArpOctaveRange(it) }

        SectionLabel("GATE: $arpGate")
        SliderControl("GATE", arpGate, 0..100) { viewModel.setArpGate(it) }

        SectionLabel("PATTERN LENGTH: $arpPatternLength")
        SliderControl("PATTERN LENGTH", arpPatternLength, 1..16) { viewModel.setArpPatternLength(it) }

        Spacer(Modifier.height(16.dp))
        Text(
            "El arpegiador se aplicará sobre los pasos activos del secuenciador usando la escala y raíz del transporte. Su ejecución está preparada para una próxima versión sin comprometer la estabilidad MIDI.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
    }
}
