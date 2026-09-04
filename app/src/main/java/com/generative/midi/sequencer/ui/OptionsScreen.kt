package com.generative.midi.sequencer.ui

import android.content.Context
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.generative.midi.sequencer.ui.theme.ActiveAccent
import com.generative.midi.sequencer.ui.theme.NoteBg

private const val PREFS = "generative_presets"

/**
 * Página OPTIONS: All Notes Off / panic, transpose, presets (save/load) y
 * export/import del patrón.
 */
@Composable
fun OptionsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val transpose by viewModel.transpose.collectAsStateWithLifecycle()
    var message by remember { mutableStateOf("") }

    val prefs = remember(context) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("OPTIONS", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(12.dp))

        // ---- All Notes Off / Panic ----
        SectionLabel("MIDI CONTROL")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = {
                viewModel.allNotesOff()
                message = "All Notes Off enviado"
            }, modifier = Modifier.weight(1f)) {
                Text("ALL NOTES OFF")
            }
            OutlinedButton(onClick = {
                viewModel.panic()
                message = "Panic: All Notes Off + CC 123/120"
            }, modifier = Modifier.weight(1f)) {
                Text("PANIC")
            }
        }
        Spacer(Modifier.height(16.dp))

        // ---- Transpose ----
        SectionLabel("TRANSPOSE (SEMITONES)")
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(NoteBg)
                    .clickable { viewModel.setTranspose(transpose - 1) }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) { Text("−", fontSize = 18.sp) }
            Spacer(Modifier.weight(0.1f))
            Text("$transpose", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE0E0E0), modifier = Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.weight(0.1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(NoteBg)
                    .clickable { viewModel.setTranspose(transpose + 1) }
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) { Text("+", fontSize = 18.sp) }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { viewModel.applyTranspose(); message = "Transposición aplicada al patrón" },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("APPLY TRANSPOSE")
        }
        Spacer(Modifier.height(16.dp))

        // ---- Presets ----
        SectionLabel("PRESETS (SAVE / LOAD)")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = {
                viewModel.savePattern(prefs, "A")
                message = "Guardado en preset A"
            }, modifier = Modifier.weight(1f)) {
                Text("SAVE A")
            }
            OutlinedButton(onClick = {
                viewModel.savePattern(prefs, "B")
                message = "Guardado en preset B"
            }, modifier = Modifier.weight(1f)) {
                Text("SAVE B")
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = {
                viewModel.loadPattern(prefs, "A")
                message = "Cargado preset A"
            }, modifier = Modifier.weight(1f)) {
                Text("LOAD A")
            }
            OutlinedButton(onClick = {
                viewModel.loadPattern(prefs, "B")
                message = "Cargado preset B"
            }, modifier = Modifier.weight(1f)) {
                Text("LOAD B")
            }
        }
        Spacer(Modifier.height(16.dp))

        // ---- Export / Import ----
        SectionLabel("EXPORT / IMPORT")
        OutlinedButton(
            onClick = {
                val data = viewModel.exportPattern()
                message = "Exportado: $data"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("EXPORT PATTERN")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                val ok = viewModel.importPattern(demoPattern())
                message = if (ok) "Importado (demo)" else "Importación fallida"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("IMPORT PATTERN (DEMO)")
        }

        if (message.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                message,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(NoteBg)
                    .padding(10.dp)
            )
        }

        Spacer(Modifier.height(16.dp))
        SectionLabel("ABOUT")
        Text(
            "Generative MIDI Sequencer V2\n" +
                "Instrumento MIDI host que genera patrones y acordes, respeando la " +
                "escala y raíz seleccionadas, para controlar sintetizadores por USB MIDI.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))
    }
}

/** JSON mínimo de ejemplo para probar la importación. */
private fun demoPattern(): String {
    val sb = StringBuilder()
    sb.append("{")
    sb.append("\"root\":\"D\",\"scale\":\"Ryukyu\",\"stepLength\":16,\"bpm\":80,")
    sb.append("\"steps\":[")
    sb.append("{\"i\":0,\"n\":62,\"v\":90,\"g\":0.8},")
    sb.append("{\"i\":4,\"n\":64,\"v\":85,\"g\":0.8},")
    sb.append("{\"i\":8,\"n\":66,\"v\":88,\"g\":0.9},")
    sb.append("{\"i\":12,\"n\":69,\"v\":92,\"g\":0.8}")
    sb.append("]}")
    return sb.toString()
}
