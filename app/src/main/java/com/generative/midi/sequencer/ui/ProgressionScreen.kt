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
 * Página PROGRESSION: genera progresiones de acordes diatónicas según la
 * escala/raíz del transporte y permite seleccionarlas para inspección.
 */
@Composable
fun ProgressionScreen(viewModel: MainViewModel) {
    val root by viewModel.root.collectAsStateWithLifecycle()
    val scaleName by viewModel.scaleName.collectAsStateWithLifecycle()
    val progression by viewModel.progression.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("PROGRESSION", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        Text(
            "ROOT: $root   SCALE: $scaleName",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        OutlinedButton(onClick = { viewModel.generateProgression() }) {
            Text("GENERATE PROGRESSION")
        }
        Spacer(Modifier.height(16.dp))

        if (progression.isEmpty()) {
            Text(
                "Pulsa GENERATE PROGRESSION para crear una progresión diatónica.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            SectionLabel("CURRENT PROGRESSION")
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                progression.forEach { chord ->
                    Text(
                        chord.name,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(NoteBg)
                            .padding(10.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
