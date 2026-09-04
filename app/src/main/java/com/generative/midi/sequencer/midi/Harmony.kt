package com.generative.midi.sequencer.midi

import kotlin.random.Random

/**
 * Lógica musical armónica: acordes, progresiones y mapa de acordes.
 *
 * Módulo de dominio puro, sin dependencias de la UI ni de MIDI I/O.
 * Utiliza reglas musicales determinísticas (armonía diatónica simple) para
 * generar sugerencias de acordes y progresiones que respeten la tonalidad.
 */
object Harmony {

    /** Cualidad de un acorde. */
    enum class Quality(val symbol: String, val intervals: List<Int>) {
        MAJOR("", ScaleManager.Chords.MAJOR),
        MINOR("m", ScaleManager.Chords.MINOR),
        DOMINANT7("7", ScaleManager.Chords.DOMINANT7),
        MINOR7("m7", ScaleManager.Chords.MINOR7),
        MAJOR7("maj7", ScaleManager.Chords.MAJOR7),
        DIMINISHED("dim", ScaleManager.Chords.DIM),
        SUS2("sus2", ScaleManager.Chords.SUS2),
        SUS4("sus4", ScaleManager.Chords.SUS4)
    }

    /** Un acorde: fundamental (nombre), cualidad y nota MIDI base. */
    data class Chord(
        val rootName: String,
        val quality: Quality,
        val baseMidi: Int
    ) {
        /** Nombre completo, p. ej. "Dm". */
        val name: String get() = rootName + quality.symbol

        /** Notas MIDI del acorde (fundamental + intervalos). */
        fun notes(): List<Int> = quality.intervals.map { baseMidi + it }

        companion object {
            fun fromName(name: String, rootMidi: Int = 60): Chord? {
                val trimmed = name.trim()
                if (trimmed.isEmpty()) return null
                // Buscar cualidad coincide en el sufijo.
                val qualities = Quality.entries.sortedByDescending { it.symbol.length }
                for (q in qualities) {
                    if (q.symbol.isEmpty()) continue
                    if (trimmed.endsWith(q.symbol)) {
                        val root = trimmed.removeSuffix(q.symbol)
                        val rootClass = ScaleManager.NOTE_NAMES.indexOfFirst { it.equals(root, ignoreCase = true) }
                        if (rootClass >= 0) {
                            val midi = (rootMidi / 12) * 12 + rootClass
                            return Chord(root, q, midi)
                        }
                        return null
                    }
                }
                // Acorde mayor simple.
                val rootClass = ScaleManager.NOTE_NAMES.indexOfFirst { it.equals(trimmed, ignoreCase = true) }
                if (rootClass >= 0) {
                    val midi = (rootMidi / 12) * 12 + rootClass
                    return Chord(trimmed, Quality.MAJOR, midi)
                }
                return null
            }
        }
    }

    /**
     * Devuelve las funciones diatónicas (acordes) de una escala (mayor o menor),
     * es decir, los acordes construidos sobre cada grado usando solo notas de la escala.
     */
    fun diatonicChords(scale: ScaleManager.Scale, rootName: String): List<Chord> {
        val rootMidi = ScaleManager.rootNameToMidi(rootName)
        val pitchClasses = ScaleManager.scalePitchClasses(scale, rootMidi)
        val rootClassOf = { midi: Int -> ((midi % 12) + 12) % 12 }

        val isMinor = isMinorScale(scale)

        return pitchClasses.map { degreeClass ->
            // Construir tríada sobre el grado usando notas dentro de la escala.
            val chordNotes = mutableListOf<Int>()
            val scaleClassSet = pitchClasses.toSet()
            val idxDeg = pitchClasses.indexOf(degreeClass)
            val degrees = listOf(0, 2, 4)
            for (d in degrees) {
                val target = pitchClasses[(idxDeg + d) % pitchClasses.size]
                chordNotes.add(target)
            }

            // Determinar cualidad según intervalos reales (en semitonos desde la raíz del acorde).
            val root = chordNotes[0]
            val third = chordNotes[1]
            val fifth = chordNotes[2]
            val thirdInterval = ((third - root) + 12) % 12
            val fifthInterval = ((fifth - root) + 12) % 12
            val seventh = if (pitchClasses.size >= 7) chordNotes.getOrNull(4) else null

            val quality = when {
                seventh != null && ((seventh - root) + 12) % 12 == 10 && fifthInterval == 7 && thirdInterval == 4 ->
                    Quality.DOMINANT7
                seventh != null && ((seventh - root) + 12) % 12 == 11 && fifthInterval == 7 && thirdInterval == 4 ->
                    Quality.MAJOR7
                seventh != null && ((seventh - root) + 12) % 12 == 10 && fifthInterval == 7 && thirdInterval == 3 ->
                    Quality.MINOR7
                fifthInterval == 6 -> Quality.DIMINISHED
                thirdInterval == 3 -> Quality.MINOR
                else -> Quality.MAJOR
            }

            val rootNameFor = ScaleManager.midiToPitchClass(rootMidi + ((degreeClass - rootClassOf(rootMidi) + 12) % 12))
            CircleNames.pitchClassToName(degreeClass)
                .let { Chord(it, quality, 60 + degreeClass) }
        }.distinctBy { it.rootName + it.quality.symbol }
    }

    private fun isMinorScale(scale: ScaleManager.Scale): Boolean =
        scale.name == ScaleManager.Scale.MINOR.name ||
            scale.name == ScaleManager.Scale.HARMONIC_MINOR.name ||
            scale.name == ScaleManager.Scale.DORIAN.name ||
            scale.name == ScaleManager.Scale.MELODIC_MINOR.name

    /** Nombres de clases de nota circulares. */
    object CircleNames {
        fun pitchClassToName(pc: Int): String = ScaleManager.NOTE_NAMES[((pc % 12) + 12) % 12]
    }

    /**
     * Genera una progresión de acordes diatónicos de [length] grados.
     * Intenta que haya un movimiento armónico real (predomina la tónica y la
     * dominante/subdominante, con camino por grados).
     */
    fun generateProgression(scale: ScaleManager.Scale, rootName: String, length: Int, random: Random = Random.Default): List<Chord> {
        val diatonic = diatonicChords(scale, rootName)
        if (diatonic.isEmpty()) return emptyList()
        val n = length.coerceIn(1, 16)
        val progression = mutableListOf<Chord>()
        // Empieza en la tónica (grado 0).
        progression.add(diatonic[0])
        var lastIdx = 0
        val pool = diatomicByLikelihood(diatonic)
        while (progression.size < n) {
            val next = pool[random.nextInt(pool.size)]
            progression.add(next)
        }
        return progression
    }

    /** Reordena los acordes según su probabilidad de aparición (tónica > dominante > resto). */
    private fun diatomicByLikelihood(diatonic: List<Chord>): List<Chord> {
        if (diatonic.size < 2) return diatonic
        val ordered = mutableListOf<Chord>()
        ordered.add(diatonic[0]) // Tónica
        val rest = diatonic.drop(1)
        ordered.addAll(rest)
        return ordered
    }

    /**
     * Mapa de acordes: para un acorde actual, sugiere acordes relacionados
     * (del mapa diatónico y por cercanía de quinta/cuarta).
     */
    fun suggestChords(current: Chord?, scale: ScaleManager.Scale, rootName: String): List<Chord> {
        val diatonic = diatonicChords(scale, rootName)
        if (diatonic.isEmpty()) return emptyList()
        if (current == null) return diatonic.take(5)

        val currentRootClass = ((current.baseMidi % 12) + 12) % 12
        val ordered = orderByFifthRelation(diatonic, currentRootClass)
        return ordered.distinctBy { it.name }.take(5)
    }

    private fun orderByFifthRelation(diatonic: List<Chord>, fromClass: Int): List<Chord> {
        val result = mutableListOf<Chord>()
        val upFifth = (fromClass + 7) % 12
        val upFourth = (fromClass + 5) % 12
        val rest = diatonic.sortedBy { chord ->
            val c = ((chord.baseMidi % 12) + 12) % 12
            val dFifth = Math.min(12 - kotlin.math.abs(((c - upFifth) + 24) % 12 - 12), 12)
            val dFourth = Math.min(12 - kotlin.math.abs(((c - upFourth) + 24) % 12 - 12), 12)
            Math.min(dFifth, dFourth)
        }
        result.addAll(rest)
        return result
    }

    /** Convierte una lista de nombres de acordes a objetos Chord. */
    fun parseChordNames(names: List<String>, rootMidi: Int = 48): List<Chord> =
        names.mapNotNull { Chord.fromName(it, rootMidi) }
}
