package com.generative.midi.sequencer.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import com.generative.midi.sequencer.midi.Harmony
import com.generative.midi.sequencer.midi.IntelligentChordMap
import com.generative.midi.sequencer.midi.MidiManager
import com.generative.midi.sequencer.midi.NoteGenerator
import com.generative.midi.sequencer.midi.PatternGenerator
import com.generative.midi.sequencer.midi.ScaleManager
import com.generative.midi.sequencer.midi.Step
import com.generative.midi.sequencer.midi.TransportController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * ViewModel principal de la aplicación (V2).
 *
 * Mantiene el estado de la UI y conecta todos los componentes del dominio
 * ([MidiManager], [PatternGenerator], [ScaleManager], [Harmony]) con la interfaz.
 *
 * Amplía la V1 con: STEP LENGTH configurable, arpegiador, progresiones, acordes,
 * mapa de acordes, opciones (transpose, All Notes Off, save/load).
 */
class MainViewModel(
    private val context: Context,
    private val midiManager: MidiManager
) : ViewModel() {

    // ---- Estado expuesto a la UI ----

    /** Nombre del dispositivo MIDI conectado. */
    private val _deviceName = MutableStateFlow("No device")
    val deviceName: StateFlow<String> = _deviceName.asStateFlow()

    /** Estado de conexión. */
    private val _status = MutableStateFlow("NOT CONNECTED")
    val status: StateFlow<String> = _status.asStateFlow()

    /** BPM actual. */
    private val _bpm = MutableStateFlow(62)
    val bpm: StateFlow<Int> = _bpm.asStateFlow()

    /** Canal MIDI (1..16). */
    private val _channel = MutableStateFlow(1)
    val channel: StateFlow<Int> = _channel.asStateFlow()

    /** Nota raíz. */
    private val _root = MutableStateFlow("D")
    val root: StateFlow<String> = _root.asStateFlow()

    /** Escala seleccionada (displayName). */
    private val _scale = MutableStateFlow("Ryukyu")
    val scaleName: StateFlow<String> = _scale.asStateFlow()

    /** Densidad 0..100. */
    private val _density = MutableStateFlow(65)
    val density: StateFlow<Int> = _density.asStateFlow()

    /** Randomness 0..100. */
    private val _randomness = MutableStateFlow(25)
    val randomness: StateFlow<Int> = _randomness.asStateFlow()

    /** Variación 0..100. */
    private val _variation = MutableStateFlow(35)
    val variation: StateFlow<Int> = _variation.asStateFlow()

    /** Octava inferior. */
    private val _octaveLow = MutableStateFlow(-1)
    val octaveLow: StateFlow<Int> = _octaveLow.asStateFlow()

    /** Octava superior. */
    private val _octaveHigh = MutableStateFlow(1)
    val octaveHigh: StateFlow<Int> = _octaveHigh.asStateFlow()

    /** Modo ambient. */
    private val _ambient = MutableStateFlow(false)
    val ambient: StateFlow<Boolean> = _ambient.asStateFlow()

    /** MIDI Clock activado/desactivado. */
    private val _midiClock = MutableStateFlow(false)
    val midiClockEnabled: StateFlow<Boolean> = _midiClock.asStateFlow()

    /** Longitud de nota (steps per beat label). */
    private val _noteLength = MutableStateFlow("1/8")
    val noteLength: StateFlow<String> = _noteLength.asStateFlow()

    /** STEP LENGTH: 16, 32, 48, 64 o CUSTOM. */
    private val _stepLength = MutableStateFlow(64)
    val stepLength: StateFlow<Int> = _stepLength.asStateFlow()

    /** ¿Step length en modo custom? */
    private val _stepLengthCustom = MutableStateFlow(false)
    val stepLengthCustom: StateFlow<Boolean> = _stepLengthCustom.asStateFlow()

    /** Valor custom del step length (1..64). */
    private val _stepLengthValue = MutableStateFlow(64)
    val stepLengthValue: StateFlow<Int> = _stepLengthValue.asStateFlow()

    /** Transpose en semitonos (OPTIONS). */
    private val _transpose = MutableStateFlow(0)
    val transpose: StateFlow<Int> = _transpose.asStateFlow()

    // ---- Arpegiador ----
    private val _arpOn = MutableStateFlow(false)
    val arpOn: StateFlow<Boolean> = _arpOn.asStateFlow()
    private val _arpMode = MutableStateFlow("UP")
    val arpMode: StateFlow<String> = _arpMode.asStateFlow()
    private val _arpRate = MutableStateFlow("1/16")
    val arpRate: StateFlow<String> = _arpRate.asStateFlow()
    private val _arpOctaveRange = MutableStateFlow(1)
    val arpOctaveRange: StateFlow<Int> = _arpOctaveRange.asStateFlow()
    private val _arpGate = MutableStateFlow(80)
    val arpGate: StateFlow<Int> = _arpGate.asStateFlow()
    private val _arpPatternLength = MutableStateFlow(8)
    val arpPatternLength: StateFlow<Int> = _arpPatternLength.asStateFlow()

    // ---- Progresión ----
    private val _progRoot = MutableStateFlow("D")
    val progRoot: StateFlow<String> = _progRoot.asStateFlow()
    private val _progScale = MutableStateFlow("Ryukyu")
    val progScale: StateFlow<String> = _progScale.asStateFlow()
    private val _progLength = MutableStateFlow(4)
    val progLength: StateFlow<Int> = _progLength.asStateFlow()
    private val _progression = MutableStateFlow<List<Harmony.Chord>>(emptyList())
    val progression: StateFlow<List<Harmony.Chord>> = _progression.asStateFlow()

    // ---- Chord Sequencer ----
    private val _chordSeqLength = MutableStateFlow(4)
    val chordSeqLength: StateFlow<Int> = _chordSeqLength.asStateFlow()
    private val _chordSeq = MutableStateFlow<List<Harmony.Chord?>>(List(4) { null })
    val chordSeq: StateFlow<List<Harmony.Chord?>> = _chordSeq.asStateFlow()

    // ---- Chord Map ----
    private val _currentChord = MutableStateFlow<Harmony.Chord?>(null)
    val currentChord: StateFlow<Harmony.Chord?> = _currentChord.asStateFlow()
    private val _chordSuggestions = MutableStateFlow<List<Harmony.Chord>>(emptyList())
    val chordSuggestions: StateFlow<List<Harmony.Chord>> = _chordSuggestions.asStateFlow()

    // ---- Intelligent Chord Map ----
    /** Historial reciente de acordes (máx. 4). La más reciente está al final. */
    private val _chordHistory = MutableStateFlow<List<Harmony.Chord>>(emptyList())
    val chordHistory: StateFlow<List<Harmony.Chord>> = _chordHistory.asStateFlow()

    /** Sugerencias inteligentes (con peso relativo y top). */
    private val _mapSuggestions = MutableStateFlow<List<IntelligentChordMap.Suggestion>>(emptyList())
    val mapSuggestions: StateFlow<List<IntelligentChordMap.Suggestion>> = _mapSuggestions.asStateFlow()

    /** true si se muestran sugerencias "MORE" (menor peso). */
    private val _mapMore = MutableStateFlow(false)
    val mapMore: StateFlow<Boolean> = _mapMore.asStateFlow()

    /** true si el modo es DEGREE MAP (exploración de grados) en vez del Intelligent Map. */
    private val _mapDegreeMode = MutableStateFlow(false)
    val mapDegreeMode: StateFlow<Boolean> = _mapDegreeMode.asStateFlow()

    /** Info de degree del acorde actual (para la UI). */
    val currentChordDegree: com.generative.midi.sequencer.midi.HarmonicDegree.DegreeInfo?
        get() = _currentChord.value?.let {
            com.generative.midi.sequencer.midi.HarmonicDegree.chordDegree(it, _root.value, ScaleManager.Scale.fromDisplayName(_scale.value))
        }

    /** Grados diatónicos disponibles (modo DEGREE MAP). */
    val mapScaleDegrees: List<Pair<com.generative.midi.sequencer.midi.HarmonicDegree.DegreeInfo, Harmony.Chord>>
        get() = com.generative.midi.sequencer.midi.HarmonicDegree.scaleDegrees(_root.value, ScaleManager.Scale.fromDisplayName(_scale.value))

    /** Patrón actual de pasos. */
    private val _pattern = MutableStateFlow<List<Step>>(emptyList())
    val pattern: StateFlow<List<Step>> = _pattern.asStateFlow()

    /** Paso actual resaltado. */
    private val _currentStep = MutableStateFlow(-1)
    val currentStep: StateFlow<Int> = _currentStep.asStateFlow()

    /** Paso seleccionado para edición en el Step Editor (-1 = ninguno). */
    private val _selectedStep = MutableStateFlow(-1)
    val selectedStep: StateFlow<Int> = _selectedStep.asStateFlow()

    /** Parámetros copiados de un paso (para COPY/PASTE). */
    private var copiedStep: com.generative.midi.sequencer.midi.Step? = null

    /** Si está reproduciendo. */
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // ---- 4 Tracks ----
    private val _trackEnabled = List(TransportController.TRACK_COUNT) { i -> MutableStateFlow(i == 0) }
    val trackEnabled: List<StateFlow<Boolean>> get() = _trackEnabled

    private val _trackChannel = List(TransportController.TRACK_COUNT) { i -> MutableStateFlow(i + 1) }
    val trackChannel: List<StateFlow<Int>> get() = _trackChannel

    private val _trackPattern = List(TransportController.TRACK_COUNT) { MutableStateFlow<List<Step>>(emptyList()) }
    val trackPattern: List<StateFlow<List<Step>>> get() = _trackPattern

    private val _trackCurrentStep = List(TransportController.TRACK_COUNT) { MutableStateFlow(-1) }
    val trackCurrentStep: List<StateFlow<Int>> get() = _trackCurrentStep

    /** Nombre de track seleccionado para edición en la UI de tracks. */
    private val _activeTrack = MutableStateFlow(0)
    val activeTrack: StateFlow<Int> = _activeTrack.asStateFlow()

    init {
        midiManager.transport.onStepChanged = { step -> _currentStep.value = step }
        midiManager.onConnectionChanged = { connected, name ->
            _deviceName.value = if (name.isNullOrEmpty()) "No device" else name
            _status.value = if (connected) "CONNECTED" else "NOT CONNECTED"
        }

        refreshPattern()
        midiManager.connectToFirstAvailable { _ -> }
    }

    // ---- Acciones base ----

    fun refreshMidi() {
        midiManager.connectToFirstAvailable { _ -> }
    }

    fun setBpm(value: Int) {
        _bpm.value = value.coerceIn(30, 180)
        midiManager.clock.bpm = _bpm.value
    }

    fun setChannel(value: Int) {
        _channel.value = value.coerceIn(1, 16)
        midiManager.transport.channel = _channel.value
    }

    fun setRoot(value: String) {
        _root.value = value
        _chordHistory.value = emptyList()
        _currentChord.value = null
        _mapMore.value = false
        refreshChordSuggestions()
    }

    fun setScale(value: String) {
        _scale.value = value
        _chordHistory.value = emptyList()
        _currentChord.value = null
        _mapMore.value = false
        refreshChordSuggestions()
        midiManager.transport.scale = ScaleManager.Scale.fromDisplayName(value)
    }

    fun setDensity(value: Int) { _density.value = value.coerceIn(0, 100) }
    fun setRandomness(value: Int) { _randomness.value = value.coerceIn(0, 100) }
    fun setVariation(value: Int) { _variation.value = value.coerceIn(0, 100) }

    fun setOctave(low: Int, high: Int) {
        _octaveLow.value = low.coerceIn(-2, 2)
        _octaveHigh.value = high.coerceIn(-2, 2)
        if (_octaveHigh.value < _octaveLow.value) _octaveHigh.value = _octaveLow.value
    }

    fun setAmbient(value: Boolean) {
        _ambient.value = value
        midiManager.transport.ambient = value
    }

    fun setMidiClock(value: Boolean) {
        _midiClock.value = value
        midiManager.transport.midiClockEnabled = value
    }

    fun setNoteLength(value: String) {
        _noteLength.value = value
    }

    // ---- STEP LENGTH ----

    fun setStepLength(value: Int) {
        val v = value.coerceIn(1, 64)
        _stepLength.value = v
        _stepLengthValue.value = v
        _stepLengthCustom.value = v != 16 && v != 32 && v != 48 && v != 64
        applyStepLengthToTransport()
        // Ajustar patrón a la nueva longitud (rellenar con silencios si hace falta).
        extendPatternIfNeeded()
    }

    fun setStepLengthCustom(value: Int) {
        val v = value.coerceIn(1, 64)
        _stepLengthValue.value = v
        _stepLength.value = v
        _stepLengthCustom.value = true
        applyStepLengthToTransport()
        extendPatternIfNeeded()
    }

    private fun applyStepLengthToTransport() {
        midiManager.transport.stepLength = _stepLength.value
    }

    private fun extendPatternIfNeeded() {
        val current = _pattern.value.toMutableList()
        val target = _stepLength.value
        if (current.size < target) {
            while (current.size < target) {
                current.add(Step.silence(current.size))
            }
            _pattern.value = current
        }
    }

    // ---- Transpose (OPTIONS) ----

    fun setTranspose(semitones: Int) {
        _transpose.value = semitones.coerceIn(-24, 24)
    }

    fun applyTranspose() {
        val t = _transpose.value
        if (t == 0) return
        _pattern.value = _pattern.value.map { step ->
            if (step.isSilence) step
            else step.copy(midiNote = (step.midiNote + t).coerceIn(0, 127))
        }
    }

    // ---- Arpegiador ----

    fun setArpOn(value: Boolean) { _arpOn.value = value; pushArpToTransport() }
    fun setArpMode(value: String) { _arpMode.value = value; pushArpToTransport() }
    fun setArpRate(value: String) { _arpRate.value = value; pushArpToTransport() }
    fun setArpOctaveRange(value: Int) { _arpOctaveRange.value = value.coerceIn(1, 4); pushArpToTransport() }
    fun setArpGate(value: Int) { _arpGate.value = value.coerceIn(0, 100); pushArpToTransport() }
    fun setArpPatternLength(value: Int) { _arpPatternLength.value = value.coerceIn(1, 16); pushArpToTransport() }

    /** Sincroniza los parámetros del arpegiador con el transporte (si está activo). */
    private fun pushArpToTransport() {
        val t = midiManager.transport
        if (!t.isPlaying) return
        t.arpOn = _arpOn.value
        t.arpMode = _arpMode.value
        t.arpOctaveRange = _arpOctaveRange.value
        t.arpGate = _arpGate.value
        t.arpPatternLength = _arpPatternLength.value
        t.updateArp()
    }

    // ---- Progresión ----

    fun setProgRoot(value: String) { _progRoot.value = value }
    fun setProgScale(value: String) { _progScale.value = value }
    fun setProgLength(value: Int) { _progLength.value = value.coerceIn(4, 16) }

    fun generateProgression() {
        val scale = ScaleManager.Scale.fromDisplayName(_progScale.value)
        _progression.value = Harmony.generateProgression(scale, _progRoot.value, _progLength.value)
        // Aplicar como sugerencia inicial del chord map.
        _currentChord.value = _progression.value.firstOrNull()
        refreshChordSuggestions()
    }

    fun sendProgressionToSequencer() {
        // Convierte la progresión en pasos de ACORDE reales (Step.chord): cada
        // cambio de acorde (i % 4 == 0) reproduce todas las notas del acorde como
        // un bloque, en lugar de una sola fundamental.
        val prog = _progression.value
        if (prog.isEmpty()) return
        val len = _stepLength.value
        val pattern = mutableListOf<Step>()
        for (i in 0 until len) {
            val chord = prog[i % prog.size]
            if (i % 4 == 0) {
                pattern.add(chordStep(i, chord))
            } else {
                pattern.add(Step.silence(i))
            }
        }
        // Reemplazar solo la porción correspondiente.
        val result = _pattern.value.toMutableList()
        for (i in pattern.indices) {
            if (i < result.size) result[i] = pattern[i] else result.add(pattern[i])
        }
        _pattern.value = result
    }

    /** Aplica la secuencia de acordes del CHORD SEQUENCER como pasos de acorde reales. */
    fun sendChordSeqToPattern() {
        val seq = _chordSeq.value
        if (seq.none { it != null }) {
            sendProgressionToSequencer()
            return
        }
        val len = _stepLength.value
        val pattern = mutableListOf<Step>()
        for (i in 0 until len) {
            val chord = seq[i % seq.size]
            if (chord != null) {
                pattern.add(chordStep(i, chord))
            } else {
                pattern.add(Step.silence(i))
            }
        }
        val result = _pattern.value.toMutableList()
        for (i in pattern.indices) {
            if (i < result.size) result[i] = pattern[i] else result.add(pattern[i])
        }
        _pattern.value = result
    }

    /** Crea un paso de acorde real con las notas MIDI del acorde en su octava base. */
    private fun chordStep(index: Int, chord: Harmony.Chord): Step {
        val notes = chord.notes().filter { it in 0..127 }
        val base = chord.baseMidi.coerceIn(0, 127)
        return Step.chord(index, base, notes, velocity = 90, gate = 0.9f)
    }

    // ---- Chord Sequencer ----

    fun setChordSeqLength(value: Int) {
        val v = value.coerceIn(4, 16)
        _chordSeqLength.value = v
        val current = _chordSeq.value.toMutableList()
        if (current.size < v) {
            while (current.size < v) current.add(null)
        } else {
            current.subList(v, current.size).clear() // solo referencia para UI; crear nueva lista
            val trimmed = current.subList(0, v).toMutableList()
            _chordSeq.value = trimmed
            return
        }
        _chordSeq.value = current
    }

    fun setChordAt(index: Int, chord: Harmony.Chord?) {
        val current = _chordSeq.value.toMutableList()
        if (index in current.indices) {
            current[index] = chord
            _chordSeq.value = current
        }
    }

    fun duplicateChordAt(index: Int) {
        val current = _chordSeq.value.toMutableList()
        if (index in current.indices && index + 1 < current.size) {
            current[index + 1] = current[index]
            _chordSeq.value = current
        }
    }

    fun clearChords() {
        _chordSeq.value = List(_chordSeqLength.value) { null }
    }

    fun randomizeChords() {
        val scale = ScaleManager.Scale.fromDisplayName(_scale.value)
        val rootName = _root.value
        val diatonic = Harmony.diatonicChords(scale, rootName)
        if (diatonic.isEmpty()) return
        _chordSeq.value = List(_chordSeqLength.value) {
            diatonic[kotlin.random.Random.nextInt(diatonic.size)]
        }
    }

    // ---- Chord Map (Intelligent) ----

    private fun chordMapContext(): IntelligentChordMap.Context =
        IntelligentChordMap.Context(
            rootName = _root.value,
            scale = ScaleManager.Scale.fromDisplayName(_scale.value),
            history = _chordHistory.value,
            ambient = _ambient.value
        )

    private fun refreshChordSuggestions() {
        val context = chordMapContext()
        _mapSuggestions.value = if (_mapMore.value) {
            IntelligentChordMap.suggestMore(context, 5)
        } else {
            IntelligentChordMap.suggest(context, 5)
        }
        // Shimm de compatibilidad para otras pantallas (Chord Sequencer).
        _chordSuggestions.value = _mapSuggestions.value.map { it.chord }
    }

    /**
     * Selecciona una sugerencia: la vuelve CURRENT, la agrega al historial
     * (máx. 4) y recalcula las siguientes sugerencias inmediatamente.
     */
    fun selectSuggestion(chord: Harmony.Chord) {
        _currentChord.value = chord
        _chordHistory.value = (_chordHistory.value + chord).takeLast(4)
        _mapMore.value = false
        refreshChordSuggestions()
    }

    /** Mantiene compatibilidad con la API anterior del mapa. */
    fun setCurrentChord(chord: Harmony.Chord) = selectSuggestion(chord)

    /** Alterna entre las sugerencias TOP y las "MORE" (de menor peso). */
    fun toggleChordMore() {
        _mapMore.value = !_mapMore.value
        refreshChordSuggestions()
    }

    /** Alterna entre el Intelligent Map y el Degree Map. */
    fun toggleMapDegreeMode() {
        _mapDegreeMode.value = !_mapDegreeMode.value
        _mapMore.value = false
        refreshChordSuggestions()
    }

    /** Selecciona un grado del DEGREE MAP como acorde. */
    fun selectDegreeChord(chord: Harmony.Chord) = selectSuggestion(chord)

    /** Limpia el historial, el acorde actual y recalea las sugerencias. */
    fun clearChordHistory() {
        _chordHistory.value = emptyList()
        _currentChord.value = null
        _mapMore.value = false
        refreshChordSuggestions()
    }

    /** Agrega el acorde actual a la progresión (sin duplicar adyacentes). */
    fun addCurrentToProgression() {
        val chord = _currentChord.value ?: return
        val current = _progression.value.toMutableList()
        if (current.isEmpty() || current.last().name != chord.name) {
            current.add(chord)
        }
        _progression.value = current
    }

    /** Envía la progresión actual al CHORD SEQUENCER (como secuencia de acordes). */
    fun sendMapToChordSequencer() {
        val prog = _progression.value
        if (prog.isEmpty()) return
        val len = _chordSeqLength.value
        val target = List(len) { i -> if (i < prog.size) prog[i] else null }
        _chordSeq.value = target
    }

    // ---- GENERATE / MUTATE ----

    fun generate() {
        stopPlaybackInternal()
        val step = generateInternal()
        _pattern.value = step
    }

    fun mutate() {
        val current = _pattern.value
        if (current.isEmpty()) {
            generate()
            return
        }
        val scale = ScaleManager.Scale.fromDisplayName(_scale.value)
        val rootMidi = ScaleManager.rootNameToMidi(_root.value)
        val mutated = PatternGenerator.mutate(
            pattern = current,
            scale = scale,
            rootMidi = rootMidi,
            variation = _variation.value,
            octaveLow = _octaveLow.value,
            octaveHigh = _octaveHigh.value,
            stepLength = _stepLength.value
        )
        _pattern.value = mutated
    }

    /** PLAY. Sincroniza los 4 tracks en el transporte y reproduce. */
    fun play() {
        if (_pattern.value.isEmpty()) return
        syncTracksToTransport()
        setTransportParams()
        midiManager.transport.play()
        _isPlaying.value = true
    }

    /** Vuelca los patrones/canales/state de cada track del ViewModel al transporte. */
    private fun syncTracksToTransport() {
        val t = midiManager.transport
        for (i in 0 until TransportController.TRACK_COUNT) {
            t.setTrackEnabled(i, _trackEnabled[i].value)
            t.setTrackChannel(i, _trackChannel[i].value)
            val p = if (i == 0) _pattern.value else _trackPattern[i].value
            t.setTrackPattern(i, p)
        }
    }

    /** STOP con All Notes Off. */
    fun stop() {
        midiManager.transport.stop()
        _isPlaying.value = false
    }

    /** ALL NOTES OFF inmediato (pánico). */
    fun allNotesOff() {
        midiManager.transport.allNotesOff()
    }

    /** Pánico completo: notas activas + CC 123. */
    fun panic() {
        midiManager.transport.panic()
    }

    // ---- Acciones por track ----

    fun setActiveTrack(index: Int) {
        if (index in 0 until TransportController.TRACK_COUNT) _activeTrack.value = index
    }

    fun setTrackEnabled(index: Int, enabled: Boolean) {
        if (index !in 0 until TransportController.TRACK_COUNT) return
        _trackEnabled[index].value = enabled
        midiManager.transport.setTrackEnabled(index, enabled)
    }

    fun setTrackChannel(index: Int, channel: Int) {
        if (index !in 0 until TransportController.TRACK_COUNT) return
        _trackChannel[index].value = channel.coerceIn(1, 16)
        midiManager.transport.setTrackChannel(index, channel)
    }

    /** Genera un patrón nuevo para el track [index] con sus parámetros. */
    fun generateTrack(index: Int) {
        if (index !in 0 until TransportController.TRACK_COUNT) return
        val step = generatePatternInternal()
        if (index == 0) {
            _pattern.value = step
        } else {
            _trackPattern[index].value = step
        }
        midiManager.transport.setTrackPattern(index, step)
    }

    /** Mutar el track [index]. */
    fun mutateTrack(index: Int) {
        if (index !in 0 until TransportController.TRACK_COUNT) return
        val current = if (index == 0) _pattern.value else _trackPattern[index].value
        if (current.isEmpty()) {
            generateTrack(index)
            return
        }
        val scale = ScaleManager.Scale.fromDisplayName(_scale.value)
        val rootMidi = ScaleManager.rootNameToMidi(_root.value)
        val mutated = PatternGenerator.mutate(
            pattern = current,
            scale = scale,
            rootMidi = rootMidi,
            variation = _variation.value,
            octaveLow = _octaveLow.value,
            octaveHigh = _octaveHigh.value,
            stepLength = _stepLength.value
        )
        if (index == 0) _pattern.value = mutated else _trackPattern[index].value = mutated
        midiManager.transport.setTrackPattern(index, mutated)
    }

    private fun generatePatternInternal(): List<Step> {
        val scale = ScaleManager.Scale.fromDisplayName(_scale.value)
        val rootMidi = ScaleManager.rootNameToMidi(_root.value)
        return PatternGenerator.generate(
            stepLength = _stepLength.value,
            scale = scale,
            rootMidi = rootMidi,
            density = _density.value,
            randomness = _randomness.value,
            octaveLow = _octaveLow.value,
            octaveHigh = _octaveHigh.value,
            ambient = _ambient.value
        )
    }

    // ---- Edición manual de steps (V2.2) ----
    //
    // El toque sobre un paso ahora lo SELECCIONA (no sobrescribe su nota). Los
    // parámetros se editan desde el Step Editor. Todos los cambios preservan la
    // nota/velocidad/gate/octava y el estado ACTIVE/REST de cada paso.

    /** Selecciona un paso para el Step Editor (-1 para deseleccionar). */
    fun selectStep(index: Int) {
        _selectedStep.value = if (index in _pattern.value.indices) index else -1
    }

    fun deselectStep() {
        _selectedStep.value = -1
    }

    /** La nota del paso seleccionado se cicla por la escala activa (nota siguiente). */
    fun cycleSelectedStepNote() {
        val idx = _selectedStep.value
        if (idx !in _pattern.value.indices) return
        val pattern = _pattern.value.toMutableList()
        val step = pattern[idx]
        if (step.isSilence || step.isRest) {
            pattern[idx] = Step.note(idx, firstScaleNote(), 90, 0.8f)
            _pattern.value = pattern
            pushPatternToTransport()
            return
        }
        val next = nextScaleNote(step.midiNote)
        pattern[idx] = Step.note(idx, next, step.velocity, step.gate, step.octave, step.active)
        _pattern.value = pattern
        pushPatternToTransport()
    }

    private fun firstScaleNote(): Int {
        val scale = ScaleManager.Scale.fromDisplayName(_scale.value)
        val rootMidi = ScaleManager.rootNameToMidi(_root.value)
        return NoteGenerator.scaleNotesFromRange(scale, rootMidi, _octaveLow.value, _octaveHigh.value)
            .firstOrNull() ?: (rootMidi + _octaveLow.value * 12)
    }

    private fun nextScaleNote(current: Int): Int {
        val scale = ScaleManager.Scale.fromDisplayName(_scale.value)
        val rootMidi = ScaleManager.rootNameToMidi(_root.value)
        val pool = NoteGenerator.scaleNotesFromRange(scale, rootMidi, _octaveLow.value, _octaveHigh.value)
        if (pool.isEmpty()) return current
        val idx = pool.indexOf(current)
        return if (idx < 0) pool.first() else pool[(idx + 1) % pool.size]
    }

    /** Long press: eliminar la nota del paso (REST). */
    fun deleteStep(index: Int) {
        val pattern = _pattern.value.toMutableList()
        if (pattern.isEmpty()) return
        if (index in pattern.indices) {
            pattern[index] = Step.silence(index)
            _pattern.value = pattern
            pushPatternToTransport()
        }
        deselectStep()
    }

    /** Edita la nota del paso conservando el resto de parámetros. */
    fun editStepNote(index: Int, midiNote: Int) {
        val pattern = _pattern.value.toMutableList()
        if (index !in pattern.indices) return
        val step = pattern[index]
        pattern[index] = step.copy(midiNote = midiNote.coerceIn(0, 127))
        applyPatternEdit(pattern)
    }

    /** Edita la velocidad (1..127) del paso. */
    fun editStepVelocity(index: Int, velocity: Int) {
        val pattern = _pattern.value.toMutableList()
        if (index !in pattern.indices) return
        val step = pattern[index]
        pattern[index] = step.copy(velocity = velocity.coerceIn(1, 127))
        applyPatternEdit(pattern)
    }

    /** Edita el gate (0..100%) del paso. Se guarda como fracción 0.0..1.0. */
    fun editStepGatePercent(index: Int, gatePercent: Int) {
        val pattern = _pattern.value.toMutableList()
        if (index !in pattern.indices) return
        val step = pattern[index]
        val frac = (gatePercent.coerceIn(0, 100)) / 100f
        val actual = if (step.isSilence) step else step.copy(gate = frac)
        pattern[index] = actual
        applyPatternEdit(pattern)
    }

    /** Edita la octava relativa (-2..2) del paso. */
    fun editStepOctave(index: Int, octave: Int) {
        val pattern = _pattern.value.toMutableList()
        if (index !in pattern.indices) return
        val step = pattern[index]
        pattern[index] = step.copy(octave = octave.coerceIn(-2, 2))
        applyPatternEdit(pattern)
    }

    /** Alterna ACTIVE / REST del paso (conserva nota/velocidad/gate). */
    fun toggleStepActive(index: Int) {
        val pattern = _pattern.value.toMutableList()
        if (index !in pattern.indices) return
        val step = pattern[index]
        pattern[index] = step.copy(active = !step.active)
        applyPatternEdit(pattern)
    }

    /** Copia los parámetros del paso (nota/velocidad/gate/octava/active). */
    fun copyStep(index: Int) {
        val step = _pattern.value.getOrNull(index) ?: return
        copiedStep = step
    }

    /** Pega los parámetros copiados en el paso [index]. */
    fun pasteStep(index: Int) {
        val src = copiedStep ?: return
        val pattern = _pattern.value.toMutableList()
        if (index !in pattern.indices) return
        pattern[index] = Step.note(
            index,
            src.midiNote,
            src.velocity,
            src.gate,
            src.octave,
            src.active
        ).copy(notes = src.notes)
        applyPatternEdit(pattern)
    }

    /** Restaura el paso seleccionado a sus valores por defecto sensatos. */
    fun resetStep(index: Int) {
        val pattern = _pattern.value.toMutableList()
        if (index !in pattern.indices) return
        pattern[index] = Step.note(index, firstScaleNote(), 90, 0.8f)
        applyPatternEdit(pattern)
    }

    /** Aplica una edición: actualiza el patrón y lo sincroniza con el transporte. */
    private fun applyPatternEdit(pattern: MutableList<Step>) {
        _pattern.value = pattern
        pushPatternToTransport()
    }

    /**
     * Sincroniza el patrón del track 0 con el transporte para que los cambios
     * realizados durante la reproducción se apliquen de forma segura en el
     * siguiente borde de paso (sin notas atascadas).
     */
    private fun pushPatternToTransport() {
        if (midiManager.transport.isPlaying) {
            midiManager.transport.setTrackPattern(0, _pattern.value)
        }
    }

    // ---- Save / Load ----

    fun savePattern(prefs: android.content.SharedPreferences?, name: String) {
        try {
            val json = JSONObject()
            json.put("bpm", _bpm.value)
            json.put("root", _root.value)
            json.put("scale", _scale.value)
            json.put("stepLength", _stepLength.value)
            json.put("density", _density.value)
            json.put("randomness", _randomness.value)
            json.put("variation", _variation.value)
            json.put("ambient", _ambient.value)
            json.put("channel", _channel.value)
            json.put("arpOn", _arpOn.value)
            json.put("arpMode", _arpMode.value)
            json.put("arpRate", _arpRate.value)
            json.put("arpOctaveRange", _arpOctaveRange.value)
            json.put("arpGate", _arpGate.value)
            json.put("arpPatternLength", _arpPatternLength.value)
            json.put("noteLength", _noteLength.value)

            val steps = org.json.JSONArray()
            for (s in _pattern.value) {
                val o = JSONObject()
                o.put("i", s.stepIndex)
                o.put("n", s.midiNote)
                o.put("v", s.velocity)
                o.put("g", s.gate.toDouble())
                o.put("o", s.octave)
                o.put("a", s.active)
                steps.put(o)
            }
            json.put("steps", steps)
            prefs?.edit()?.putString("pattern_$name", json.toString())?.apply()
        } catch (e: Exception) {
            // ignorar
        }
    }

    fun loadPattern(prefs: android.content.SharedPreferences?, name: String) {
        try {
            val raw = prefs?.getString("pattern_$name", null) ?: return
            val json = JSONObject(raw)
            _bpm.value = json.optInt("bpm", 62)
            midiManager.clock.bpm = _bpm.value
            _root.value = json.optString("root", "D")
            _scale.value = json.optString("scale", "Ryukyu")
            setStepLength(json.optInt("stepLength", 64))
            _density.value = json.optInt("density", 65)
            _randomness.value = json.optInt("randomness", 25)
            _variation.value = json.optInt("variation", 35)
            _ambient.value = json.optBoolean("ambient", false)
            setChannel(json.optInt("channel", 1))
            _arpOn.value = json.optBoolean("arpOn", false)
            _arpMode.value = json.optString("arpMode", "UP")
            _arpRate.value = json.optString("arpRate", "1/16")
            _arpOctaveRange.value = json.optInt("arpOctaveRange", 1)
            _arpGate.value = json.optInt("arpGate", 80)
            _arpPatternLength.value = json.optInt("arpPatternLength", 8)
            _noteLength.value = json.optString("noteLength", "1/8")

            val steps = json.optJSONArray("steps")
            val list = mutableListOf<Step>()
            if (steps != null) {
                for (i in 0 until steps.length()) {
                    val o = steps.getJSONObject(i)
                    val isRest = !o.optBoolean("a", true)
                    list.add(Step.note(
                        o.optInt("i"),
                        o.optInt("n", -1),
                        o.optInt("v", 100),
                        o.optDouble("g", 0.9).toFloat(),
                        o.optInt("o", 0),
                        !isRest
                    ))
                }
            }
            _pattern.value = list
            midiManager.transport.stepLength = _stepLength.value
        } catch (e: Exception) {
            // ignorar
        }
    }

    /** Exporta el patrón actual como texto JSON comprimido (para compartir). */
    fun exportPattern(): String {
        val json = JSONObject()
        json.put("root", _root.value)
        json.put("scale", _scale.value)
        json.put("stepLength", _stepLength.value)
        json.put("bpm", _bpm.value)
        val steps = org.json.JSONArray()
        for (s in _pattern.value) {
            val o = JSONObject()
            o.put("i", s.stepIndex)
            o.put("n", s.midiNote)
            o.put("v", s.velocity)
            o.put("g", s.gate.toDouble())
            o.put("o", s.octave)
            o.put("a", s.active)
            steps.put(o)
        }
        json.put("steps", steps)
        return json.toString()
    }

    /** Importa un patrón desde texto JSON producido por [exportPattern]. */
    fun importPattern(text: String): Boolean {
        return try {
            val json = JSONObject(text)
            _root.value = json.optString("root", "D")
            _scale.value = json.optString("scale", "Ryukyu")
            val steps = json.optJSONArray("steps")
            val list = mutableListOf<Step>()
            if (steps != null) {
                for (i in 0 until steps.length()) {
                    val o = steps.getJSONObject(i)
                    list.add(Step.note(
                        o.optInt("i"),
                        o.optInt("n", -1),
                        o.optInt("v", 100),
                        o.optDouble("g", 0.9).toFloat(),
                        o.optInt("o", 0),
                        !o.optBoolean("a", true)
                    ))
                }
            }
            _pattern.value = list
            setStepLength(json.optInt("stepLength", list.size.coerceIn(1, 64)))
            true
        } catch (e: Exception) {
            false
        }
    }

    // ---- Helpers ----

    private fun generateInternal(): List<Step> {
        val scale = ScaleManager.Scale.fromDisplayName(_scale.value)
        val rootMidi = ScaleManager.rootNameToMidi(_root.value)
        val len = _stepLength.value
        val base = PatternGenerator.generate(
            stepLength = len,
            scale = scale,
            rootMidi = rootMidi,
            density = _density.value,
            randomness = _randomness.value,
            octaveLow = _octaveLow.value,
            octaveHigh = _octaveHigh.value,
            ambient = _ambient.value
        )
        // Si el patrón anterior era más largo, preservar la longitud total del
        // patrón en memoria pero solo reproducir los activos (transport ya respeta stepLength).
        return base
    }

    /** Aplica los parámetros actuales al transporte antes de reproducir. */
    private fun setTransportParams() {
        val t = midiManager.transport
        t.channel = _channel.value
        t.pattern = _pattern.value
        t.ambient = _ambient.value
        t.midiClockEnabled = _midiClock.value
        t.stepLength = _stepLength.value
        t.scale = ScaleManager.Scale.fromDisplayName(_scale.value)
        t.rootMidi = ScaleManager.rootNameToMidi(_root.value)
        t.transpose = _transpose.value
        t.arpOn = _arpOn.value
        t.arpMode = _arpMode.value
        t.arpOctaveRange = _arpOctaveRange.value
        t.arpGate = _arpGate.value
        t.arpPatternLength = _arpPatternLength.value
        t.updateArp()
        t.stepsPerBeat = stepsPerBeatFor(_noteLength.value)
        midiManager.clock.bpm = _bpm.value
    }

    private fun stepsPerBeatFor(length: String): Int = when (length) {
        "1/4" -> 4
        "1/2" -> 2
        "1 BAR" -> 1
        else -> 2 // 1/8 por defecto
    }

    private fun stopPlaybackInternal() {
        midiManager.transport.stop()
    }

    private fun refreshPattern() {
        _pattern.value = generateInternal()
    }

    override fun onCleared() {
        midiManager.transport.shutdown()
        super.onCleared()
    }
}
