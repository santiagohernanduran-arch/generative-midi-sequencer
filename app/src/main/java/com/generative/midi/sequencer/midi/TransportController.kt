package com.generative.midi.sequencer.midi

/**
 * Controlador de transporte del secuenciador (multitrack, 4 tracks).
 *
 * [TransportController] une el reloj ([ClockManager]) con los pasos de cada track y
 * la salida MIDI ([MidiOutput]):
 *  - avanza por los pasos de cada track habilitado de forma continua;
 *  - genera Note On / Note Off en el momento adecuado por track/canal;
 *  - emite MIDI Clock / Start / Stop cuando el reloj está activo;
 *  - garantiza la seguridad MIDI: evita notas atascadas (All Notes Off al parar).
 *
 * Soporta 4 tracks independientes, cada uno con su propio patrón, canal MIDI,
 * step length, escala, raíz y arpegiador. Un único reloj maestro sincroniza a todos.
 *
 * La API "mono" (pattern, channel, scale, etc.) delega en el track 0 para mantener
 * compatibilidad con la capa de UI existente.
 */
class TransportController(
    private val output: MidiOutput,
    private val clock: ClockManager
) {

    companion object {
        private const val TICKS = 96 // ticks por negra (coincide con ClockManager)
        const val TRACK_COUNT = 4
    }

    /** Estado por track (patrón, canal, params y estado de reproducción). */
    private class TrackData {
        var pattern: List<Step> = emptyList()
        var channel: Int = 1
        var stepLength: Int = PatternGenerator.MAX_STEPS
        var scale: ScaleManager.Scale = ScaleManager.Scale.RYUKYU_PENTATONIC
        var rootMidi: Int = 62
        var enabled: Boolean = true

        var arpOn: Boolean = false
        var arpMode: String = "UP"
        var arpOctaveRange: Int = 1
        var arpGate: Int = 80
        var arpPatternLength: Int = 8
        var arpDirty: Boolean = false
        var arpNotes: List<Int> = emptyList()
        var arpIndex: Int = 0

        var currentStepIndex: Int = -1
        val activeNotes = mutableMapOf<Int, Int>() // note -> count

        // Programación de gate por paso (Nota Off dentro del paso).
        var gateReleaseTick: Long = -1      // tick absoluto en que se libera la nota actual
        val gateNotes = mutableListOf<Int>() // notas del paso actual a liberar por gate
    }

    private val tracks = List(TRACK_COUNT) { i ->
        TrackData().apply {
            channel = i + 1       // track i -> canal i+1 por defecto
            enabled = (i == 0)    // solo el primero habilitado por defecto
        }
    }

    // ---- Parámetros globales (compartidos; los tracks pueden anular) ----

    /** Velocidad de reproducción en pasos por negra (global). */
    var stepsPerBeat: Int = 4

    /** Modo ambient global (afecta a los tracks que lo permitan). */
    var ambient: Boolean = false

    /** Si el MIDI Clock está activo. */
    var midiClockEnabled: Boolean = false

    /** Transposición global en semitonos (OPTIONS). Se aplica a todos los tracks. */
    var transpose: Int = 0

    // ---- API "mono" (delega al track 0) ----

    var pattern: List<Step>
        get() = tracks[0].pattern
        set(value) { tracks[0].pattern = value }

    var channel: Int
        get() = tracks[0].channel
        set(value) { tracks[0].channel = value.coerceIn(1, 16) }

    var stepLength: Int
        get() = tracks[0].stepLength
        set(value) {
            val v = value.coerceIn(1, PatternGenerator.MAX_STEPS)
            tracks[0].stepLength = v
            if (tracks[0].currentStepIndex >= v) {
                allNotesOff()
                tracks.forEach { it.currentStepIndex = 0 }
            }
        }

    var scale: ScaleManager.Scale
        get() = tracks[0].scale
        set(value) { tracks[0].scale = value }

    var rootMidi: Int
        get() = tracks[0].rootMidi
        set(value) { tracks[0].rootMidi = value }

    // ---- Arpegiador (track 0) ----

    var arpOn: Boolean
        get() = tracks[0].arpOn
        set(value) { tracks[0].arpOn = value; tracks[0].arpDirty = true }

    var arpMode: String
        get() = tracks[0].arpMode
        set(value) { tracks[0].arpMode = value; tracks[0].arpDirty = true }

    var arpOctaveRange: Int
        get() = tracks[0].arpOctaveRange
        set(value) { tracks[0].arpOctaveRange = value.coerceIn(1, 4); tracks[0].arpDirty = true }

    var arpGate: Int
        get() = tracks[0].arpGate
        set(value) { tracks[0].arpGate = value.coerceIn(0, 100) }

    var arpPatternLength: Int
        get() = tracks[0].arpPatternLength
        set(value) { tracks[0].arpPatternLength = value.coerceIn(1, 16); tracks[0].arpDirty = true }

    /** Indica que el estado de reproducción del track 1 cambió. */
    var onStepChanged: ((Int) -> Unit)? = null

    private var playing = false

    /** true si el transporte está reproduciendo. */
    val isPlaying: Boolean get() = playing

    // ================= API multitrack =================

    /** Número de tracks (siempre 4). */
    val trackCount: Int get() = TRACK_COUNT

    fun trackEnabled(index: Int): Boolean = tracks.getOrNull(index)?.enabled ?: false
    fun setTrackEnabled(index: Int, enabled: Boolean) {
        val t = tracks.getOrNull(index) ?: return
        if (t.enabled == enabled) return
        if (!enabled) {
            turnOffTrack(index)
        }
        t.enabled = enabled
        if (playing) t.arpDirty = true
    }

    fun trackChannel(index: Int): Int = tracks.getOrNull(index)?.channel ?: 1
    fun setTrackChannel(index: Int, channel: Int) {
        tracks.getOrNull(index)?.channel = channel.coerceIn(1, 16)
    }

    fun trackPattern(index: Int): List<Step> = tracks.getOrNull(index)?.pattern ?: emptyList()
    fun setTrackPattern(index: Int, pattern: List<Step>) {
        tracks.getOrNull(index)?.pattern = pattern
    }

    fun trackStepLength(index: Int): Int = tracks.getOrNull(index)?.stepLength ?: 64
    fun setTrackStepLength(index: Int, stepLength: Int) {
        val t = tracks.getOrNull(index) ?: return
        val v = stepLength.coerceIn(1, PatternGenerator.MAX_STEPS)
        t.stepLength = v
        if (playing && t.currentStepIndex >= v) {
            allNotesOff()
            tracks.forEach { it.currentStepIndex = 0 }
        }
    }

    fun trackScale(index: Int): ScaleManager.Scale = tracks.getOrNull(index)?.scale ?: ScaleManager.Scale.RYUKYU_PENTATONIC
    fun setTrackScale(index: Int, scale: ScaleManager.Scale) {
        tracks.getOrNull(index)?.scale = scale
    }

    fun trackRootMidi(index: Int): Int = tracks.getOrNull(index)?.rootMidi ?: 62
    fun setTrackRootMidi(index: Int, rootMidi: Int) {
        tracks.getOrNull(index)?.rootMidi = rootMidi
    }

    fun trackArpOn(index: Int): Boolean = tracks.getOrNull(index)?.arpOn ?: false
    fun setTrackArpOn(index: Int, on: Boolean) {
        val t = tracks.getOrNull(index) ?: return
        t.arpOn = on
        t.arpDirty = true
    }

    fun trackCurrentStep(index: Int): Int = tracks.getOrNull(index)?.currentStepIndex ?: -1

    // ================= Reproducción =================

    /** Inicia la reproducción de todos los tracks habilitados. */
    fun play() {
        if (playing) return
        playing = true
        tracks.forEach { it.currentStepIndex = 0 }

        if (midiClockEnabled) {
            output.sendSystemMessage(MidiOutput.STATUS_START)
        }
        clock.onTick = ::onTick
        clock.start()
        onStepChanged?.invoke(tracks[0].currentStepIndex)
    }

    /** Actualiza el arpegiador del track 0 (compatibilidad). */
    fun updateArp() {
        tracks.forEach { t -> if (t.arpOn) rebuildArpNotes(t) }
    }

    private fun rebuildArpNotes(t: TrackData) {
        t.arpDirty = false
        if (!t.arpOn) {
            t.arpNotes = emptyList()
            return
        }
        val classes = ScaleManager.scalePitchClasses(t.scale, t.rootMidi)
        if (classes.isEmpty()) {
            t.arpNotes = emptyList()
            return
        }
        val base = (t.rootMidi / 12) * 12
        val notes = mutableListOf<Int>()
        val range = t.arpOctaveRange.coerceIn(1, 4)
        for (o in 0 until range) {
            for (c in classes) {
                notes.add(base + o * 12 + c)
            }
        }
        t.arpNotes = when (t.arpMode.uppercase()) {
            "DOWN" -> notes.reversed()
            "UP+DOWN", "UP&DOWN" -> {
                if (notes.size <= 1) notes
                else notes + notes.drop(1).dropLast(1).reversed()
            }
            "ANYORDER" -> notes.shuffled()
            else -> notes
        }
    }

    /** Detiene la reproducción y limpia notas activas (All Notes Off). */
    fun stop() {
        if (!playing) return
        playing = false
        clock.stop()
        allNotesOff()
        if (midiClockEnabled) {
            output.sendSystemMessage(MidiOutput.STATUS_STOP)
        }
        tracks.forEach { it.currentStepIndex = -1 }
        onStepChanged?.invoke(-1)
    }

    /** Envía Note Off de todas las notas activas de todos los tracks (seguridad). */
    fun allNotesOff() {
        tracks.forEach { t ->
            for (note in t.activeNotes.keys) {
                output.sendChannelMessage(MidiOutput.STATUS_NOTE_OFF, t.channel, note, 0)
            }
            t.activeNotes.clear()
        }
    }

    /** Apaga solo las notas del track [index]. */
    private fun turnOffTrack(index: Int) {
        val t = tracks.getOrNull(index) ?: return
        for (note in t.activeNotes.keys) {
            output.sendChannelMessage(MidiOutput.STATUS_NOTE_OFF, t.channel, note, 0)
        }
        t.activeNotes.clear()
    }

    /** Pánico: All Notes Off + CC 123/120 en todos los canales habilitados. */
    fun panic() {
        allNotesOff()
        tracks.filter { it.enabled }.forEach {
            output.sendAllNotesOffCc(it.channel)
        }
    }

    /** Limpieza al desconectar: detener y enviar All Notes Off. */
    fun shutdown() {
        stop()
        allNotesOff()
        clock.stop()
    }

    private fun onTick(tick: Long) {
        if (!playing) return
        val ticksPerStep = (TICKS / stepsPerBeat.coerceAtLeast(1)).coerceAtLeast(1)
        // Liberar notas cuyo gate ya venció (antes de avanzar de paso) para que el
        // gate = 100% dure casi todo el paso y los menores se acorten a tiempo.
        releaseDueGates(tick)
        if (tick % ticksPerStep == 0L) {
            advanceStep(tick, ticksPerStep)
        }
        if (midiClockEnabled) {
            output.sendSystemMessage(MidiOutput.STATUS_CLOCK)
        }
    }

    /**
     * Libera (Note Off) las notas de cada track cuyo gate ya venció. Como la
     * nota se elimina de [TrackData.activeNotes], el apagado defensivo del
     * siguiente paso no reenvía un Note Off duplicado.
     */
    private fun releaseDueGates(tick: Long) {
        for (t in tracks) {
            if (!t.enabled) continue
            if (t.gateReleaseTick >= 0 && tick >= t.gateReleaseTick && t.gateNotes.isNotEmpty()) {
                for (note in t.gateNotes) {
                    if (t.activeNotes[note] != null) {
                        output.sendChannelMessage(MidiOutput.STATUS_NOTE_OFF, t.channel, note, 0)
                        t.activeNotes.remove(note)
                    }
                }
                t.gateNotes.clear()
                t.gateReleaseTick = -1
            }
        }
    }

    private fun advanceStep(tick: Long, ticksPerStep: Int) {
        var anyActive = false
        tracks.forEach { t ->
            if (!t.enabled) return@forEach
            if (t.pattern.isEmpty()) return@forEach
            anyActive = true
            val effective = t.stepLength.coerceIn(1, t.pattern.size)
            t.currentStepIndex = (t.currentStepIndex + 1) % effective
            playStep(t, tick, ticksPerStep)
        }
        onStepChanged?.invoke(tracks[0].currentStepIndex)
        if (!anyActive && playing) {
            // Sin tracks activos con notas: pánico defensivo.
            allNotesOff()
        }
    }

    private fun playStep(t: TrackData, tick: Long, ticksPerStep: Int) {
        val step = t.pattern.getOrNull(t.currentStepIndex) ?: return

        // REST (Active=false), silencio o gate 0: no emitir nota.
        if (step.isRest || step.isSilence || step.gate <= 0f) {
            turnOffTrackActive(t)
            t.gateNotes.clear()
            t.gateReleaseTick = -1
            if (t.arpOn) t.arpIndex = 0
        } else if (t.arpOn) {
            t.gateNotes.clear()
            t.gateReleaseTick = -1
            if (t.arpDirty) rebuildArpNotes(t)
            if (t.arpNotes.isEmpty()) {
                turnOffTrackActive(t)
            } else {
                turnOffTrackActive(t)
                val noteIndex = t.arpIndex % t.arpNotes.size
                val note = (t.arpNotes[noteIndex] + transpose).coerceIn(0, 127)
                output.sendChannelMessage(MidiOutput.STATUS_NOTE_ON, t.channel, note, step.velocity)
                t.activeNotes.merge(note, 1, Int::plus)
                t.arpIndex = (t.arpIndex + 1) % (t.arpPatternLength.coerceIn(1, 16))
            }
        } else {
            turnOffTrackActive(t)
            val oct = step.octave.coerceIn(-3, 3)
            t.gateNotes.clear()
            for (n in step.allNotes) {
                val note = (n + oct * 12 + transpose).coerceIn(0, 127)
                output.sendChannelMessage(MidiOutput.STATUS_NOTE_ON, t.channel, note, step.velocity)
                t.activeNotes.merge(note, 1, Int::plus)
                t.gateNotes.add(note)
            }
            // Programar el Note Off por gate dentro del paso (fracción de la duración).
            val gateTicks = (ticksPerStep.toFloat() * step.gate.coerceIn(0f, 1f)).toInt().coerceAtLeast(1)
            t.gateReleaseTick = tick + gateTicks
        }
    }

    /** Apaga las notas activas de un track concreto. */
    private fun turnOffTrackActive(t: TrackData) {
        for (note in t.activeNotes.keys) {
            output.sendChannelMessage(MidiOutput.STATUS_NOTE_OFF, t.channel, note, 0)
        }
        t.activeNotes.clear()
    }
}
