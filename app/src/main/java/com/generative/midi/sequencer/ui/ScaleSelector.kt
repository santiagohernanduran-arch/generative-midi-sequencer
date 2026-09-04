package com.generative.midi.sequencer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.generative.midi.sequencer.midi.ScaleManager
import com.generative.midi.sequencer.ui.theme.ActiveAccent
import com.generative.midi.sequencer.ui.theme.NoteBg

/**
 * Entrada de la biblioteca de escalas: la escala del modelo de datos (con su
 * nombre detallado interno) y una etiqueta UI concisa.
 */
data class ScaleEntry(val scale: ScaleManager.Scale, val label: String)

/**
 * Biblioteca unificada de escalas. Todas las escalas forman parte de una única
 * colección musical (sin categorías geográficas), en el orden deseado.
 */
val SCALE_LIBRARY: List<ScaleEntry> = listOf(
    ScaleEntry(ScaleManager.Scale.MAJOR, "Major"),
    ScaleEntry(ScaleManager.Scale.MINOR, "Minor"),
    ScaleEntry(ScaleManager.Scale.DORIAN, "Dorian"),
    ScaleEntry(ScaleManager.Scale.PHRYGIAN, "Phrygian"),
    ScaleEntry(ScaleManager.Scale.LYDIAN, "Lydian"),
    ScaleEntry(ScaleManager.Scale.MIXOLYDIAN, "Mixolydian"),
    ScaleEntry(ScaleManager.Scale.LOCRIAN, "Locrian"),
    ScaleEntry(ScaleManager.Scale.MAJOR_PENTATONIC, "Maj Pent"),
    ScaleEntry(ScaleManager.Scale.MINOR_PENTATONIC, "Min Pent"),
    ScaleEntry(ScaleManager.Scale.WHOLE_TONE, "Whole Tone"),
    ScaleEntry(ScaleManager.Scale.HARMONIC_MINOR, "Harm Minor"),
    ScaleEntry(ScaleManager.Scale.MELODIC_MINOR, "Mel Minor"),
    ScaleEntry(ScaleManager.Scale.HUNGARIAN_MINOR, "Hung Minor"),
    ScaleEntry(ScaleManager.Scale.DOUBLE_HARMONIC, "Dbl Harmonic"),
    ScaleEntry(ScaleManager.Scale.PHRYGIAN_DOMINANT, "Phryg Dom"),
    ScaleEntry(ScaleManager.Scale.RYUKYU_PENTATONIC, "Ryukyu"),
    ScaleEntry(ScaleManager.Scale.HIRAJOSHI, "Hirajoshi"),
    ScaleEntry(ScaleManager.Scale.INSEN, "Insen"),
    ScaleEntry(ScaleManager.Scale.KUMOI, "Kumoi"),
    ScaleEntry(ScaleManager.Scale.YO, "Yo")
)

/**
 * Cuadrícula responsiva de escalas. Usa 2 columnas en pantallas de teléfono
 * portátil y 3 en anchos mayores. Cada celda usa el mismo ancho/alto, sin
 * desplazamiento horizontal ni filas desiguales.
 */
@Composable
fun ScaleLibraryGrid(
    selectedName: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columns = if (maxWidth >= 520.dp) 3 else 2
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SCALE_LIBRARY.chunked(columns).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    row.forEach { entry ->
                        val selected = selectedName.equals(entry.scale.displayName, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) ActiveAccent else NoteBg)
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) ActiveAccent else Color(0xFF2C343D),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { onSelect(entry.scale.displayName) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = entry.label,
                                color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta con la información de la escala seleccionada: ROOT, SCALE y las NOTAS
 * de la escala. Se actualiza automáticamente al cambiar de escala.
 */
@Composable
fun CurrentScaleInfo(
    root: String,
    scaleName: String,
    modifier: Modifier = Modifier
) {
    val scale = ScaleManager.Scale.fromDisplayName(scaleName)
    val rootMidi = ScaleManager.rootNameToMidi(root)
    val notes = ScaleManager.scaleNoteNames(scale, rootMidi)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(NoteBg)
            .border(1.dp, Color(0xFF2C343D), RoundedCornerShape(10.dp))
            .height(64.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        InfoColumn("ROOT", root, Modifier.weight(1f))
        InfoColumn("SCALE", scaleName.uppercase(), Modifier.weight(1f))
        Column(
            modifier = Modifier.weight(1f).height(64.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text("NOTES", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            Text(
                notes.joinToString(" – "),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun InfoColumn(label: String, value: String, modifier: Modifier) {
    Column(
        modifier = modifier.height(64.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        Text(value, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
    }
}
