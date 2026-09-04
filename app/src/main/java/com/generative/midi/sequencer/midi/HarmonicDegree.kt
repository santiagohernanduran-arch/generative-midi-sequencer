package com.generative.midi.sequencer.midi

/**
 * Mapeo de grados armónicos: chord → degree, numeral romano.
 *
 * Dado un root y una escala, asigna a cada chord su grado diatónico.
 * La capa de dominio NO depende de Compose: en lugar de expone un índice
 * de color (colorIndex) que la capa UI resuelve a un Color real.
 */
object HarmonicDegree {

    data class DegreeInfo(
        val degree: Int,        // 1..7
        val romanNumeral: String,
        val qualityLabel: String,
        val colorIndex: Int     // 0..6 — resuelto a Color en la capa UI
    )

    private val ROMAN_MAJOR = listOf("I", "II", "III", "IV", "V", "VI", "VII")
    private val ROMAN_MINOR = listOf("i", "ii°", "iii", "iv", "v", "vi", "vii°")

    /**
     * Calcula el degree de un chord dado el root y la escala.
     * Devuelve null si el chord no es diatónico de la escala.
     */
    fun chordDegree(chord: Harmony.Chord, rootName: String, scale: ScaleManager.Scale): DegreeInfo? {
        val rootMidi = ScaleManager.rootNameToMidi(rootName)
        val pitchClasses = ScaleManager.scalePitchClasses(scale, rootMidi)
        val chordClass = ((chord.baseMidi % 12) + 12) % 12
        val degreeIndex = pitchClasses.indexOf(chordClass)
        if (degreeIndex < 0) return null

        val degree = degreeIndex + 1
        val isMinor = isMinorDegree(chord, scale, degreeIndex)
        val isDim = chord.quality == Harmony.Quality.DIMINISHED
        val roman = when {
            degree > 7 -> "$degree"
            isDim -> ROMAN_MINOR[degreeIndex].replace("°", "°")
            isMinor -> ROMAN_MINOR[degreeIndex]
            else -> ROMAN_MAJOR[degreeIndex]
        }
        val qualityLabel = when {
            isDim -> "dim"
            chord.quality == Harmony.Quality.MINOR -> "m"
            chord.quality == Harmony.Quality.MAJOR7 -> "maj7"
            chord.quality == Harmony.Quality.MINOR7 -> "m7"
            chord.quality == Harmony.Quality.DOMINANT7 -> "7"
            chord.quality == Harmony.Quality.SUS2 -> "sus2"
            chord.quality == Harmony.Quality.SUS4 -> "sus4"
            else -> ""
        }
        val colorIndex = degreeIndex.coerceIn(0, 6)

        return DegreeInfo(degree, roman, qualityLabel, colorIndex)
    }

    /**
     * Devuelve todos los grados diatónicos de la escala con sus chords.
     * Para escalas pentatónicas usa las estructuras compatibles.
     */
    fun scaleDegrees(rootName: String, scale: ScaleManager.Scale): List<Pair<DegreeInfo, Harmony.Chord>> {
        val chords = IntelligentChordMap.candidates(
            IntelligentChordMap.Context(rootName, scale, emptyList())
        )
        val rootMidi = ScaleManager.rootNameToMidi(rootName)
        val pitchClasses = ScaleManager.scalePitchClasses(scale, rootMidi)

        return chords.mapNotNull { chord ->
            val degreeInfo = chordDegree(chord, rootName, scale)
            if (degreeInfo != null) degreeInfo to chord else null
        }
    }

    private fun isMinorDegree(chord: Harmony.Chord, scale: ScaleManager.Scale, degreeIndex: Int): Boolean {
        val isMinorScale = scale.name in listOf(
            ScaleManager.Scale.MINOR.name,
            ScaleManager.Scale.DORIAN.name,
            ScaleManager.Scale.HARMONIC_MINOR.name,
            ScaleManager.Scale.MELODIC_MINOR.name
        )
        return when {
            chord.quality == Harmony.Quality.MINOR -> true
            chord.quality == Harmony.Quality.MAJOR -> false
            isMinorScale && degreeIndex in listOf(0, 3, 4) -> false
            !isMinorScale && degreeIndex in listOf(1, 5, 6) -> true
            else -> false
        }
    }
}
