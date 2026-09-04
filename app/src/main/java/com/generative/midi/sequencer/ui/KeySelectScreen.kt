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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.generative.midi.sequencer.midi.ScaleManager
import com.generative.midi.sequencer.ui.theme.ActiveAccent
import com.generative.midi.sequencer.ui.theme.NoteBg

/**
 * Página KEY SELECT: selección de ROOT NOTE y SCALE con visualización de las
 * notas activas de la escala.
 */
@Composable
fun KeySelectScreen(viewModel: MainViewModel) {
    val root by viewModel.root.collectAsStateWithLifecycle()
    val scaleName by viewModel.scaleName.collectAsStateWithLifecycle()

    val scale = ScaleManager.Scale.fromDisplayName(scaleName)
    val rootMidi = ScaleManager.rootNameToMidi(root)
    val scaleClasses = ScaleManager.scalePitchClasses(scale, rootMidi).toSet()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("KEY SELECT", fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))

        SectionLabel("ROOT NOTE")
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ScaleManager.ROOT_NAMES.chunked(6).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { r ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (root == r) ActiveAccent else NoteBg)
                                .clickable { viewModel.setRoot(r) }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(r, fontSize = 14.sp, color = if (root == r) Color.White else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        SectionLabel("SCALE")
        ScaleLibraryGrid(selectedName = scaleName, onSelect = { viewModel.setScale(it) })
        Spacer(Modifier.height(16.dp))

        // ---- Información de la escala seleccionada (ROOT / SCALE / NOTES) ----
        CurrentScaleInfo(root = root, scaleName = scaleName)
        Spacer(Modifier.height(20.dp))

        // ---- Mini teclado con notas resaltadas ----
        SectionLabel("KEYBOARD")
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            ScaleManager.NOTE_NAMES.forEach { n ->
                val midiClass = ScaleManager.NOTE_NAMES.indexOf(n)
                val inScale = scaleClasses.contains(midiClass)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when {
                                inScale -> ActiveAccent
                                n.contains("#") -> Color(0xFF22272E)
                                else -> NoteBg
                            }
                        ),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Text(
                        n,
                        fontSize = 9.sp,
                        color = if (inScale) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
