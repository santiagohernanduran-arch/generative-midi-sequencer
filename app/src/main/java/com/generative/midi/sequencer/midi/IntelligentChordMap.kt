package com.generative.midi.sequencer.midi

/**
 * Motor de Mapa de Acordes Inteligente (dominio puro).
 *
 * Responde "¿qué acorde podría sonar lógicamente a continuación?" combinando:
 *  - la tonalidad (root + scale);
 *  - el acorde actual;
 *  - el historial reciente de acordes;
 *  - relaciones armónicas y un sistema de puntuación ponderada.
 *
 * Todo es **determinístico** (sin Machine Learning, sin API, sin red; funciona
 * offline). Cada candidato recibe una puntuación musical y se normaliza a un
 * porcentaje relativo. La arquitectura permite que un futuro modelo reemplace
 * o aumente el motor de scoring.
 */
object IntelligentChordMap {

    /** Una sugerencia con toda la info armónica. */
    data class Suggestion(
        val chord: Harmony.Chord,
        val percent: Int,
        val top: Boolean,
        val degreeInfo: HarmonicDegree.DegreeInfo? = null
    )

    /** Contexto musical de entrada para calcular las sugerencias. */
    data class Context(
        val rootName: String,
        val scale: ScaleManager.Scale,
        val history: List<Harmony.Chord>,
        val ambient: Boolean = false
    )

    private const val HISTORY_DEPTH = 4

    // Pesos de los distintos factores musicales (ajustables durante el desarrollo).
    private const val W_TONIC = 6
    private const val W_DOMINANT = 4
    private const val W_FIFTH = 5
    private const val W_FOURTH = 5
    private const val W_STEP = 3
    private const val W_TRITONE = -3
    private const val W_SAME = -6
    private const val REPEAT_PENALTY = 7
    private const val VARIETY_PENALTY = 3

    // ---- Generación de candidatos ----

    /**
     * Acordes candidatos para el mapa. Para escalas heptatónicas usamos las
     * tríadas diatónicas; para pentatónicas/novedosas priorizamos estructuras
     * compatibles (power/dyads/sus/triadas disponibles) sin forzar lógica de 7 notas.
     */
    fun candidates(context: Context): List<Harmony.Chord> {
        val isPentatonic = context.scale.intervals.size < 7
        return if (isPentatonic) pentatonicCandidates(context)
        else Harmony.diatonicChords(context.scale, context.rootName)
    }

    /**
     * Genera acordes compatibles con una escala pentatónica/no tradicional.
     * Para cada nota de la escala como fundamental elige la mejor estructura
     * construible con notas dentro de la propia escala (tríada, dyad, sus, power).
     */
    private fun pentatonicCandidates(context: Context): List<Harmony.Chord> {
        val rootMidi = ScaleManager.rootNameToMidi(context.rootName)
        val classes = ScaleManager.scalePitchClasses(context.scale, rootMidi).toSet()
        val result = mutableListOf<Harmony.Chord>()
        val seen = mutableSetOf<String>()

        for (rootClass in classes) {
            val hasMinor3 = (rootClass + 3) % 12 in classes
            val hasMajor3 = (rootClass + 4) % 12 in classes
            val hasFifth = (rootClass + 7) % 12 in classes
            val hasSus2 = (rootClass + 2) % 12 in classes
            val hasSus4 = (rootClass + 5) % 12 in classes

            val quality = when {
                hasMajor3 && hasFifth -> Harmony.Quality.MAJOR
                hasMinor3 && hasFifth -> Harmony.Quality.MINOR
                hasSus4 && hasFifth -> Harmony.Quality.SUS4
                hasFifth && hasSus2 -> Harmony.Quality.SUS2
                hasFifth -> Harmony.Quality.MAJOR
                hasSus4 -> Harmony.Quality.SUS4
                hasSus2 -> Harmony.Quality.SUS2
                hasMajor3 -> Harmony.Quality.MAJOR
                hasMinor3 -> Harmony.Quality.MINOR
                else -> Harmony.Quality.MAJOR
            }

            val rootNameFor = ScaleManager.midiToPitchClass(rootMidi + rootClass)
            val baseMidi = (rootMidi / 12) * 12 + rootClass
            val chord = Harmony.Chord(rootNameFor, quality, baseMidi)
            if (seen.add(chord.name)) result.add(chord)
        }
        return result
    }

    // ---- Puntuación ----

    private fun score(chord: Harmony.Chord, context: Context): Int {
        var score = 0
        val keyClass = ((ScaleManager.rootNameToMidi(context.rootName) % 12) + 12) % 12
        val chordClass = ((chord.baseMidi % 12) + 12) % 12

        // 1. Estructura tonal: tónica y dominante tienen atractivo funcional.
        if (chordClass == keyClass) score += W_TONIC
        if (chordClass == (keyClass + 7) % 12) score += W_DOMINANT

        // 2. Movimiento desde el acorde actual (quinta/cuarta → fuerte).
        val last = context.history.lastOrNull()
        if (last != null) {
            val lastClass = ((last.baseMidi % 12) + 12) % 12
            val dist = circularDist(lastClass, chordClass)
            score += when (dist) {
                7 -> W_FIFTH
                5 -> W_FOURTH
                1, 2 -> W_STEP
                6 -> W_TRITONE
                0 -> W_SAME
                else -> 0
            }

            // 3. Variedad/repetición: penalizar acordes ya muy usados recientemente.
            val recent = context.history.takeLast(HISTORY_DEPTH - 1)
            val repeats = recent.count { it.name == chord.name }
            score -= repeats * REPEAT_PENALTY
            if (repeats > 0) score -= VARIETY_PENALTY
        }

        // 4. Modo AMBIENT: armonía estable, movimiento pequeño, sin resoluciones dramáticas.
        if (context.ambient) {
            val overlap = sharedNotes(chord, last).toInt()
            score += overlap * 4
            if (chordClass == (keyClass + 7) % 12) score -= 3
            if (last != null) {
                val dist = circularDist(((last.baseMidi % 12) + 12) % 12, chordClass)
                if (dist <= 2) score += 2
            }
        }

        return score.coerceAtLeast(0)
    }

    /** Notas en común (clases) entre dos acordes, contando la fundamental. */
    private fun sharedNotes(a: Harmony.Chord, b: Harmony.Chord?): Int {
        if (b == null) return 0
        val aNotes = a.notes().map { ((it % 12) + 12) % 12 }.toSet()
        val bNotes = b.notes().map { ((it % 12) + 12) % 12 }.toSet()
        return aNotes.intersect(bNotes).size
    }

    private fun circularDist(a: Int, b: Int): Int {
        val d = kotlin.math.abs(a - b)
        return kotlin.math.min(d, 12 - d)
    }

    // ---- Resultados ----

    private fun scoredAll(context: Context): List<Pair<Harmony.Chord, Int>> =
        candidates(context)
            .map { it to score(it, context) }
            .sortedWith(compareByDescending<Pair<Harmony.Chord, Int>> { it.second }
                .thenBy { it.first.name })

    /** Las [count] mejores sugerencias con degree info. */
    fun suggest(context: Context, count: Int = 5): List<Suggestion> {
        val scored = scoredAll(context)
        return toSuggestions(scored.take(count), topIndex = 0, context)
    }

    /** Variante "MORE": candidatos de menor peso (más abajo en el ranking). */
    fun suggestMore(context: Context, count: Int = 5): List<Suggestion> {
        val scored = scoredAll(context)
        return toSuggestions(scored.drop(5).take(count), topIndex = -1, context)
    }

    private fun toSuggestions(
        scored: List<Pair<Harmony.Chord, Int>>,
        topIndex: Int,
        context: Context
    ): List<Suggestion> {
        if (scored.isEmpty()) return emptyList()
        val max = scored.maxOf { it.second }.coerceAtLeast(1)
        return scored.mapIndexed { i, (chord, weight) ->
            val percent = (weight * 100 / max).coerceIn(5, 99)
            val degreeInfo = HarmonicDegree.chordDegree(chord, context.rootName, context.scale)
            Suggestion(chord = chord, percent = percent, top = i == topIndex, degreeInfo = degreeInfo)
        }
    }
}
