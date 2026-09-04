package com.generative.midi.sequencer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.generative.midi.sequencer.midi.HarmonicDegree
import com.generative.midi.sequencer.midi.IntelligentChordMap
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Resuelve un colorIndex de grado armónico (capa de dominio, pura) a un Color
 * de Compose. Mantiene consistente la palette en toda la UI del wheel.
 */
object DegreeColors {
    val VALUES = listOf(
        Color(0xFF81C784), // 0 — I   green (primary/central)
        Color(0xFF90CAF9), // 1 — II  blue-light
        Color(0xFFFFAB91), // 2 — III warm peach
        Color(0xFFF48FB1), // 3 — IV  pink/magenta
        Color(0xFFFF8A65), // 4 — V   orange
        Color(0xFF80DEEA), // 5 — VI  cyan
        Color(0xFFB39DDB)  // 6 — VII violet
    )
    fun fromIndex(index: Int): Color = VALUES[index.coerceIn(0, VALUES.size - 1)]
}

/**
 * Harmonic Wheel radial dibujado en Canvas.
 * - Centro: acorde actual con anillo verde prominente.
 * - Alrededor: nodos de sugerencias conectados por líneas sutiles, cada uno con
 *   el color de su grado armónico y su numeral romano.
 * - Los tamaños se definen en dp y se convierten a px con la densidad real.
 */
@Composable
fun HarmonicWheel(
    currentName: String,
    currentDegree: HarmonicDegree.DegreeInfo?,
    suggestions: List<IntelligentChordMap.Suggestion>,
    onSelect: (IntelligentChordMap.Suggestion) -> Unit
) {
    val canvasSize = 360.dp
    val nodeRadiusDp = 48f
    val centerRadiusDp = 50f
    val ringRadiusDp = 120f

    val density = LocalDensity.current.density

    val nodeRadius = nodeRadiusDp * density
    val centerRadius = centerRadiusDp * density
    val ringRadius = ringRadiusDp * density
    val widthPx = canvasSize.value * density
    val heightPx = canvasSize.value * density
    val cx = widthPx / 2f
    val cy = heightPx / 2f

    val textMeasurer = rememberTextMeasurer()
    // Pulsa el centro cada vez que cambia el acorde (selección desde el anillo).
    val selectPulse = remember(currentName) { Animatable(0f) }
    LaunchedEffect(currentName) {
        selectPulse.snapTo(0f)
        selectPulse.animateTo(1f, animationSpec = tween(durationMillis = 220))
    }
    val rotation = remember { mutableFloatStateOf(0f) }
    var lastAngle by remember { mutableFloatStateOf(0f) }
    var draggingFirst by remember { mutableStateOf(true) }

    BoxWithConstraints(modifier = Modifier.size(canvasSize), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .size(canvasSize)
                .pointerInput(suggestions) {
                    detectTapGestures { tap ->
                        val n = suggestions.size
                        for (i in suggestions.indices) {
                            val angle = angleFor(i, n, rotation.value)
                            val nx = cx + (cos(angle) * ringRadius).toFloat()
                            val ny = cy + (sin(angle) * ringRadius).toFloat()
                            if (hypot(tap.x - nx, tap.y - ny) <= nodeRadius * 1.35f) {
                                onSelect(suggestions[i])
                                break
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { ofs ->
                            lastAngle = atan2(ofs.y - cy, ofs.x - cx)
                            draggingFirst = true
                        },
                        onDrag = { change, _ ->
                            val pos = change.position
                            if (draggingFirst) {
                                lastAngle = atan2(pos.y - cy, pos.x - cx)
                                draggingFirst = false
                            } else {
                                val a = atan2(pos.y - cy, pos.x - cx)
                                rotation.value += (a - lastAngle)
                                lastAngle = a
                            }
                        }
                    )
                }
        ) {
            val n = suggestions.size
            val pulse = selectPulse.value

            // Líneas de conexión centro → sugerencia.
            suggestions.forEachIndexed { i, _ ->
                val angle = angleFor(i, n, rotation.value)
                drawLine(
                    color = Color.White.copy(alpha = 0.12f),
                    start = pt(cx, cy),
                    end = pt(cx + (cos(angle) * ringRadius).toFloat(), cy + (sin(angle) * ringRadius).toFloat()),
                    strokeWidth = 1.5f * density
                )
            }

            // Nodos de sugerencias.
            suggestions.forEachIndexed { i, s ->
                val angle = angleFor(i, n, rotation.value)
                val nx = cx + (cos(angle) * ringRadius).toFloat()
                val ny = cy + (sin(angle) * ringRadius).toFloat()
                val deg = s.degreeInfo
                val nodeColor = if (deg != null) DegreeColors.fromIndex(deg.colorIndex) else Color(0xFF455A64)
                val isTop = s.top

                drawCircle(color = nodeColor.copy(alpha = 0.25f), radius = nodeRadius, center = pt(nx, ny))
                drawCircle(
                    color = if (isTop) Color.White else nodeColor,
                    radius = nodeRadius,
                    center = pt(nx, ny),
                    style = Stroke(width = if (isTop) 2.5f * density else 1.5f * density)
                )

                val nameStyle = TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (deg != null) DegreeColors.fromIndex(deg.colorIndex).copy(alpha = 0.98f) else Color.White
                )
                val nameLayout = textMeasurer.measure(s.chord.name, nameStyle)
                drawText(nameLayout, topLeft = pt(nx - nameLayout.size.width / 2f, ny - nameLayout.size.height / 2f))

                val roman = deg?.romanNumeral ?: ""
                if (roman.isNotEmpty()) {
                    val romanStyle = TextStyle(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = (if (deg != null) DegreeColors.fromIndex(deg.colorIndex) else Color.White).copy(alpha = 0.85f)
                    )
                    val romanLayout = textMeasurer.measure(roman, romanStyle)
                    drawText(
                        romanLayout,
                        topLeft = pt(nx - romanLayout.size.width / 2f, ny - nameLayout.size.height / 2f + nodeRadius * 0.55f)
                    )
                }
            }

            // Centro.
            drawCircle(color = Color(0xFF1E2429), radius = centerRadius, center = pt(cx, cy))
            drawCircle(
                color = Color(0xFF4CAF50),
                radius = centerRadius + (ringRadius - centerRadius) * pulse,
                center = pt(cx, cy),
                style = Stroke(width = 2.5f * density * (1f - pulse * 0.5f))
            )
            drawCircle(
                color = Color(0xFF4CAF50).copy(alpha = 0.35f * (1f - pulse)),
                radius = centerRadius + (ringRadius - centerRadius) * pulse,
                center = pt(cx, cy),
                style = Stroke(width = 1f * density)
            )
            val centerStyle = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            val centerLayout = textMeasurer.measure(currentName, centerStyle)
            drawText(centerLayout, topLeft = pt(cx - centerLayout.size.width / 2f, cy - centerLayout.size.height / 2f))

            if (currentDegree != null) {
                val degStyle = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DegreeColors.fromIndex(currentDegree.colorIndex))
                val degLayout = textMeasurer.measure(currentDegree.romanNumeral, degStyle)
                drawText(degLayout, topLeft = pt(cx - degLayout.size.width / 2f, cy + centerRadius * 0.45f))
            }
        }
    }
}

/** Ángulo base de un nodo, desplazado por la rotación del anillo (radianes). */
private fun angleFor(index: Int, n: Int, rotation: Float = 0f): Double {
    return (index.toDouble() / n.coerceAtLeast(1)) * 2.0 * Math.PI - Math.PI / 2.0 + rotation
}

/** Construye un Offset desde floats, evitando la resolución al constructor internal. */
private fun pt(x: Float, y: Float) = androidx.compose.ui.geometry.Offset(x, y)

/**
 * Degree Wheel (Modo B): muestra los grados diatónicos de la escala en círculo.
 * Cada grado tiene su color propio y su acorde. El usuario toca un grado para
 * elegir el destino armónico.
 */
@Composable
fun DegreeWheel(
    degrees: List<Pair<HarmonicDegree.DegreeInfo, com.generative.midi.sequencer.midi.Harmony.Chord>>,
    onSelect: (com.generative.midi.sequencer.midi.Harmony.Chord) -> Unit
) {
    val canvasSize = 360.dp
    val nodeRadiusDp = 44f
    val ringRadiusDp = 128f
    val centerRadiusDp = 40f

    val density = LocalDensity.current.density
    val nodeRadius = nodeRadiusDp * density
    val ringRadius = ringRadiusDp * density
    val widthPx = canvasSize.value * density
    val heightPx = canvasSize.value * density
    val cx = widthPx / 2f
    val cy = heightPx / 2f
    val textMeasurer = rememberTextMeasurer()

    val rotation = remember { mutableFloatStateOf(0f) }
    var lastAngle by remember { mutableFloatStateOf(0f) }
    var draggingFirst by remember { mutableStateOf(true) }

    BoxWithConstraints(modifier = Modifier.size(canvasSize), contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .size(canvasSize)
                .pointerInput(degrees) {
                    detectTapGestures { tap ->
                        val n = degrees.size
                        for (i in degrees.indices) {
                            val angle = angleFor(i, n, rotation.value)
                            val nx = cx + (cos(angle) * ringRadius).toFloat()
                            val ny = cy + (sin(angle) * ringRadius).toFloat()
                            if (hypot(tap.x - nx, tap.y - ny) <= nodeRadius * 1.35f) {
                                onSelect(degrees[i].second)
                                break
                            }
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { ofs ->
                            lastAngle = atan2(ofs.y - cy, ofs.x - cx)
                            draggingFirst = true
                        },
                        onDrag = { change, _ ->
                            val pos = change.position
                            if (draggingFirst) {
                                lastAngle = atan2(pos.y - cy, pos.x - cx)
                                draggingFirst = false
                            } else {
                                val a = atan2(pos.y - cy, pos.x - cx)
                                rotation.value += (a - lastAngle)
                                lastAngle = a
                            }
                        }
                    )
                }
        ) {
            val n = degrees.size
            // Centro neutro.
            drawCircle(color = Color(0xFF1E2429), radius = centerRadiusDp * density, center = pt(cx, cy))
            drawCircle(
                color = Color(0xFF455A64),
                radius = centerRadiusDp * density,
                center = pt(cx, cy),
                style = Stroke(width = 1.5f * density)
            )

            degrees.forEachIndexed { i, (info, chord) ->
                val angle = angleFor(i, n, rotation.value)
                val nx = cx + (cos(angle) * ringRadius).toFloat()
                val ny = cy + (sin(angle) * ringRadius).toFloat()

                drawLine(
                    color = DegreeColors.fromIndex(info.colorIndex).copy(alpha = 0.15f),
                    start = pt(cx, cy),
                    end = pt(nx, ny),
                    strokeWidth = 1.5f * density
                )
                drawCircle(color = DegreeColors.fromIndex(info.colorIndex).copy(alpha = 0.25f), radius = nodeRadius, center = pt(nx, ny))
                drawCircle(color = DegreeColors.fromIndex(info.colorIndex), radius = nodeRadius, center = pt(nx, ny), style = Stroke(width = 1.8f * density))

                val romanStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DegreeColors.fromIndex(info.colorIndex).copy(alpha = 0.98f))
                val romanLayout = textMeasurer.measure(info.romanNumeral, romanStyle)
                drawText(romanLayout, topLeft = pt(nx - romanLayout.size.width / 2f, ny - romanLayout.size.height / 2f - nodeRadius * 0.2f))

                val chordStyle = TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.8f))
                val chordLayout = textMeasurer.measure(chord.name, chordStyle)
                drawText(chordLayout, topLeft = pt(nx - chordLayout.size.width / 2f, ny - romanLayout.size.height / 2f + nodeRadius * 0.4f))
            }
        }
    }
}





