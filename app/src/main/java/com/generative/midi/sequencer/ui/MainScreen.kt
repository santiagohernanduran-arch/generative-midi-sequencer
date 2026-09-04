package com.generative.midi.sequencer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.generative.midi.sequencer.midi.ScaleManager
import com.generative.midi.sequencer.midi.Step
import com.generative.midi.sequencer.ui.theme.ActiveAccent
import com.generative.midi.sequencer.ui.theme.NoteBg
import com.generative.midi.sequencer.ui.theme.SilenceBg
import com.generative.midi.sequencer.ui.theme.StepActiveBg

/**
 * Pantalla principal SEQUENCER.
 *
 * Organiza la UI en secciones: cabecera, controles (BPM, MIDI, ROOT, SCALE,
 * STEP LENGTH, generativos, etc.), el secuenciador (solo muestra los pasos
 * activos según STEP LENGTH) y los botones de acción.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, onDispose: () -> Unit) {

    val bpm by viewModel.bpm.collectAsStateWithLifecycle()
    val deviceName by viewModel.deviceName.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val channel by viewModel.channel.collectAsStateWithLifecycle()
    val root by viewModel.root.collectAsStateWithLifecycle()
    val scaleName by viewModel.scaleName.collectAsStateWithLifecycle()
    val density by viewModel.density.collectAsStateWithLifecycle()
    val randomness by viewModel.randomness.collectAsStateWithLifecycle()
    val variation by viewModel.variation.collectAsStateWithLifecycle()
    val octaveLow by viewModel.octaveLow.collectAsStateWithLifecycle()
    val octaveHigh by viewModel.octaveHigh.collectAsStateWithLifecycle()
    val ambient by viewModel.ambient.collectAsStateWithLifecycle()
    val midiClock by viewModel.midiClockEnabled.collectAsStateWithLifecycle()
    val noteLength by viewModel.noteLength.collectAsStateWithLifecycle()
    val pattern by viewModel.pattern.collectAsStateWithLifecycle()
    val currentStep by viewModel.currentStep.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val stepLength by viewModel.stepLength.collectAsStateWithLifecycle()
    val stepLengthCustom by viewModel.stepLengthCustom.collectAsStateWithLifecycle()
    val stepLengthValue by viewModel.stepLengthValue.collectAsStateWithLifecycle()
    val selectedStep by viewModel.selectedStep.collectAsStateWithLifecycle()

    // Android Back: cerrar el Step Editor antes de salir de la pantalla.
    BackHandler(enabled = selectedStep in pattern.indices) {
        viewModel.deselectStep()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(
            text = "GENERATIVE MIDI",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = FontFamily.Monospace
        )
        Spacer(Modifier.height(8.dp))

        // ---- BPM ----
        SectionLabel("BPM")
        Slider(
            value = bpm.toFloat(),
            onValueChange = { viewModel.setBpm(it.toInt()) },
            valueRange = 30f..180f,
            steps = 149
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("$bpm BPM", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE0E0E0))
            Text("30 - 180", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))

        // ---- MIDI Device ----
        SectionLabel("MIDI DEVICE")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(deviceName, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                status,
                color = if (status == "CONNECTED") Color(0xFF66BB6A) else MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { viewModel.refreshMidi() }) {
                Text("REFRESH MIDI")
            }
            Spacer(Modifier.width(8.dp))
            Text("CH", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
            ChannelSelector(channel = channel, onSelect = { viewModel.setChannel(it) })
        }
        Spacer(Modifier.height(8.dp))

        // ---- Root & Scale ----
        SectionLabel("ROOT NOTE")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RootSelector(current = root, onSelect = { viewModel.setRoot(it) })
        }
        Spacer(Modifier.height(8.dp))
        SectionLabel("SCALE")
        ScaleLibraryGrid(selectedName = scaleName, onSelect = { viewModel.setScale(it) })
        Spacer(Modifier.height(8.dp))
        CurrentScaleInfo(root = root, scaleName = scaleName)
        Spacer(Modifier.height(8.dp))

        // ---- STEP LENGTH (pertenece a la sección del secuenciador) ----
        SectionLabel("STEP LENGTH")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(16, 32, 48, 64).forEach { opt ->
                StepLengthToggle("$opt", selected = !stepLengthCustom && stepLength == opt) {
                    viewModel.setStepLength(opt)
                }
            }
            StepLengthToggle("CUSTOM", selected = stepLengthCustom) {
                viewModel.setStepLengthCustom(stepLengthValue)
            }
        }
        if (stepLengthCustom) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                OutlinedButton(onClick = { viewModel.setStepLengthCustom(stepLengthValue - 1) }) { Text("-") }
                Spacer(Modifier.width(12.dp))
                Text("$stepLengthValue", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE0E0E0))
                Spacer(Modifier.width(12.dp))
                OutlinedButton(onClick = { viewModel.setStepLengthCustom(stepLengthValue + 1) }) { Text("+") }
            }
        }
        Spacer(Modifier.height(8.dp))

        // ---- Sequencer ----
        SectionLabel("$stepLength STEP SEQUENCER")
        Spacer(Modifier.height(8.dp))
        StepGrid(
            pattern = pattern,
            currentStep = currentStep,
            selectedStep = selectedStep,
            stepLength = stepLength,
            onTap = viewModel::selectStep,
            onLongPress = viewModel::deleteStep
        )
        Spacer(Modifier.height(16.dp))

        // ---- Step Editor (V2.2) ----
        if (selectedStep in pattern.indices) {
            StepEditor(
                stepIndex = selectedStep,
                step = pattern[selectedStep],
                onClose = viewModel::deselectStep,
                onCycleNote = viewModel::cycleSelectedStepNote,
                onVelocityChange = { v -> viewModel.editStepVelocity(selectedStep, v) },
                onGateChange = { p -> viewModel.editStepGatePercent(selectedStep, p) },
                onOctaveChange = { o -> viewModel.editStepOctave(selectedStep, o) },
                onToggleActive = { viewModel.toggleStepActive(selectedStep) },
                onCopy = { viewModel.copyStep(selectedStep) },
                onPaste = { viewModel.pasteStep(selectedStep) },
                onReset = { viewModel.resetStep(selectedStep) }
            )
            Spacer(Modifier.height(16.dp))
        }

        // ---- Generative controls ----
        SliderControl("DENSITY", density, 0..100) { viewModel.setDensity(it) }
        SliderControl("RANDOMNESS", randomness, 0..100) { viewModel.setRandomness(it) }
        SliderControl("VARIATION", variation, 0..100) { viewModel.setVariation(it) }

        // OCTAVE RANGE
        SliderControl("OCTAVE", octaveLow, -2..2) { v ->
            viewModel.setOctave(v, octaveHigh)
        }
        Text("RANGE $octaveLow / +$octaveHigh", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))

        // NOTE LENGTH
        SectionLabel("NOTE LENGTH")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("1/8", "1/4", "1/2", "1 BAR").forEach { opt ->
                SmallToggle(text = opt, selected = noteLength == opt) { viewModel.setNoteLength(opt) }
            }
        }
        Spacer(Modifier.height(8.dp))

        // AMBIENT & MIDI CLOCK switches
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("AMBIENT MODE", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Switch(checked = ambient, onCheckedChange = { viewModel.setAmbient(it) })
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("MIDI CLOCK", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.weight(1f))
            Switch(checked = midiClock, onCheckedChange = { viewModel.setMidiClock(it) })
        }
        Spacer(Modifier.height(16.dp))

        // ---- Buttons ----
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { viewModel.generate() }, modifier = Modifier.weight(1f)) {
                Text("GENERATE")
            }
            OutlinedButton(onClick = { viewModel.mutate() }, modifier = Modifier.weight(1f)) {
                Text("MUTATE")
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { viewModel.play() },
                modifier = Modifier.weight(1f),
                enabled = pattern.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = if (isPlaying) Color(0xFF00796B) else Color(0xFF2E7D32))
            ) {
                Text(if (isPlaying) "PLAYING" else "PLAY")
            }
            OutlinedButton(onClick = { viewModel.stop() }, modifier = Modifier.weight(1f)) {
                Text("STOP")
            }
            OutlinedButton(onClick = { viewModel.panic() }, modifier = Modifier.weight(1f)) {
                Text("PANIC")
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp
    )
    Spacer(Modifier.height(4.dp))
}

@Composable
fun SliderControl(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Slider(
        value = value.toFloat(),
        onValueChange = { onChange(it.toInt()) },
        valueRange = range.first.toFloat()..range.last.toFloat()
    )
    Text("$value", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(4.dp))
}

@Composable
fun ChannelSelector(channel: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(1, 2, 3, 4, 16).forEach { c ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (channel == c) ActiveAccent else NoteBg)
                    .clickable { onSelect(c) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("$c", fontSize = 12.sp, color = if (channel == c) Color.White else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

private val ROOTS = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

@Composable
fun RootSelector(current: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        ROOTS.forEach { r ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (current == r) ActiveAccent else NoteBg)
                    .clickable { onSelect(r) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(r, fontSize = 12.sp, color = if (current == r) Color.White else MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun SmallToggle(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) ActiveAccent else NoteBg)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 13.sp, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun StepLengthToggle(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) ActiveAccent else NoteBg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * Cuadrícula de pasos. Solo muestra los pasos activos según [stepLength]
 * (en filas de 8). Cada paso muestra la nota (o "-" si silencio / REST).
 * El paso que suena se resalta con [currentStep]; el paso seleccionado se
 * resalta con [selectedStep]. Tocar = seleccionar; mantener = REST.
 */
@Composable
private fun StepGrid(
    pattern: List<Step>,
    currentStep: Int,
    selectedStep: Int,
    stepLength: Int,
    onTap: (Int) -> Unit,
    onLongPress: (Int) -> Unit
) {
    Column {
        val rows = ((stepLength + 7) / 8).coerceAtLeast(1)
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                for (col in 0 until 8) {
                    val index = row * 8 + col
                    if (index >= stepLength) break
                    StepCell(
                        index = index,
                        pattern = pattern,
                        currentStep = currentStep,
                        selectedStep = selectedStep,
                        onTap = onTap,
                        onLongPress = onLongPress
                    )
                }
            }
        }
    }
}

@Composable
private fun StepCell(
    index: Int,
    pattern: List<Step>,
    currentStep: Int,
    selectedStep: Int,
    onTap: (Int) -> Unit,
    onLongPress: (Int) -> Unit
) {
    val step = pattern.getOrNull(index)
    val isSilence = step == null || step.isSilence
    val isRest = step?.isRest == true
    val isSelected = index == selectedStep
    val isActive = index == currentStep
    val noteText = when {
        isSilence -> "-"
        isRest -> "--"
        else -> ScaleManager.midiToName(step.midiNote)
    }

    Box(
        modifier = Modifier
            .padding(3.dp)
            .size(40.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(
                when {
                    isSelected -> Color(0xFF3D5AFE)      // foco selección (azul)
                    isActive -> StepActiveBg
                    isSilence || isRest -> SilenceBg
                    else -> NoteBg
                }
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = when {
                    isSelected -> Color.White
                    isActive -> ActiveAccent
                    isSilence || isRest -> Color(0xFF22272E)
                    else -> Color(0xFF37474F)
                },
                shape = RoundedCornerShape(6.dp)
            )
            .pointerInput(index) {
                detectTapGestures(
                    onTap = { onTap(index) },
                    onLongPress = { onLongPress(index) }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = noteText,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = when {
                isActive || isSelected -> Color.White
                isSilence || isRest -> Color(0xFF555B63)
                else -> MaterialTheme.colorScheme.onSurface
            },
            fontWeight = if (isActive || isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/**
 * Step Editor (V2.2): edita los parámetros independientes del paso seleccionado.
 * Diseñado compacto para pantallas portrait. Todos los cambios se aplican al
 * paso en vivo y de forma segura.
 */
@Composable
private fun StepEditor(
    stepIndex: Int,
    step: Step,
    onClose: () -> Unit,
    onCycleNote: () -> Unit,
    onVelocityChange: (Int) -> Unit,
    onGateChange: (Int) -> Unit,
    onOctaveChange: (Int) -> Unit,
    onToggleActive: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onReset: () -> Unit
) {
    val gatePercent = (step.gate.coerceIn(0f, 1f) * 100).toInt()
    val isActiveStep = step.active && step.midiNote >= 0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF14181D))
            .border(1.dp, Color(0xFF2A333C), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "STEP ${stepIndex + 1}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier.size(44.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("✕", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(10.dp))

            // NOTE
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("NOTE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(16.dp))
                OutlinedButton(onClick = onCycleNote, modifier = Modifier.weight(1f)) {
                    Text(
                        if (isActiveStep) ScaleManager.midiToName(step.midiNote) else "--",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            // VELOCITY
            Text("VELOCITY  ${step.velocity}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = step.velocity.toFloat(),
                onValueChange = { onVelocityChange(it.toInt()) },
                valueRange = 1f..127f
            )
            Spacer(Modifier.height(4.dp))

            // GATE
            Text("GATE  $gatePercent%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(
                value = gatePercent.toFloat(),
                onValueChange = { onGateChange(it.toInt()) },
                valueRange = 0f..100f
            )
            Spacer(Modifier.height(4.dp))

            // OCTAVE
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("OCTAVE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = { onOctaveChange(step.octave - 1) }) { Text("−") }
                Text("${step.octave}", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 14.dp))
                OutlinedButton(onClick = { onOctaveChange(step.octave + 1) }) { Text("+") }
            }
            Spacer(Modifier.height(8.dp))

            // ACTIVE / REST
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("ACTIVE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Text(
                    if (step.active) "ON" else "REST",
                    color = if (step.active) Color(0xFF66BB6A) else Color(0xFFCF6679),
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(10.dp))
                Switch(checked = step.active, onCheckedChange = { onToggleActive() })
            }
            Spacer(Modifier.height(10.dp))

            // COPY / PASTE / RESET
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onCopy, modifier = Modifier.weight(1f)) { Text("COPY") }
                OutlinedButton(onClick = onPaste, modifier = Modifier.weight(1f)) { Text("PASTE") }
                OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) { Text("RESET") }
            }
        }
    }
}
