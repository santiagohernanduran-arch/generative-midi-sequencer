package com.generative.midi.sequencer.midi

import kotlin.random.Random

/**
 * Generador de patrones musicales (orquestador de alto nivel).
 *
 * Es responsable de:
 *  - generar un patrón completo de [stepLength] pasos (GENERATE);
 *  - mutar el patrón actual (MUTATE) sobre los pasos activos, sin regenerarlo;
 *  - decidir el peso de la mutación según VARIATION.
 *
 * Mantiene la independencia respecto a la UI y a MIDI I/O: solo trabaja sobre
 * listas de [Step].
 */
object PatternGenerator {

    /** Número máximo de pasos del patrón. */
    const val MAX_STEPS = 64

    /**
     * Genera un patrón nuevo de [stepLength] pasos respetando todos los parámetros.
     * El valor por defecto (64) mantiene compatibilidad con la V1.
     */
    fun generate(
        stepLength: Int = MAX_STEPS,
        scale: ScaleManager.Scale,
        rootMidi: Int,
        density: Int,
        randomness: Int,
        octaveLow: Int,
        octaveHigh: Int,
        ambient: Boolean,
        seed: Long? = null
    ): List<Step> {
        val length = stepLength.coerceIn(1, MAX_STEPS)
        val random = if (seed == null) Random.Default else Random(seed)
        return NoteGenerator.generate(
            length = length,
            scale = scale,
            rootMidi = rootMidi,
            density = density,
            randomness = randomness,
            octaveLow = octaveLow,
            octaveHigh = octaveHigh,
            ambient = ambient,
            random = random
        )
    }

    /**
     * Muta el patrón actual modificando algunos pasos activos, respetando la
     * escala. El número de pasos modificados depende de VARIATION:
     *
     *  - Variation baja: 1–3 cambios.
     *  - Variation media: 10–25% de los pasos.
     *  - Variation alta: 25–50% de los pasos.
     *
     * Solo se trabaja sobre los primeros [stepLength] pasos (los activos).
     */
    fun mutate(
        pattern: List<Step>,
        scale: ScaleManager.Scale,
        rootMidi: Int,
        variation: Int,
        octaveLow: Int,
        octaveHigh: Int,
        stepLength: Int = pattern.size
    ): List<Step> {
        val result = pattern.toMutableList()
        val active = stepLength.coerceIn(1, MAX_STEPS).coerceAtMost(result.size)
        if (result.isEmpty()) return result

        val n = active
        val toChange = when {
            variation < 33 -> randomInt(1, 3)
            variation < 66 -> (n * 0.125).toInt().coerceAtLeast(2)
            else -> (n * 0.35).toInt().coerceAtLeast(4)
        }

        val pool = mutableListOf<Int>()
        for (oct in octaveLow..octaveHigh) {
            pool.addAll(ScaleManager.scaleNotes(scale, rootMidi, oct))
        }
        pool.sort()
        if (pool.isEmpty()) pool.add(rootMidi)

        val indices = (0 until n).toMutableList().shuffled().take(toChange.coerceAtMost(n))

        for (idx in indices) {
            val current = result.getOrElse(idx) { Step.silence(idx) }
            when {
                current.isRest -> {
                    // Un REST puede reactivarse con nueva nota (o quedarse en silencio).
                    if (Random.nextInt(100) < 55) {
                        result[idx] = Step.note(idx, pool.random(), randomVelocity(), randomGate())
                    }
                }
                current.isSilence -> {
                    if (Random.nextInt(100) < 55) {
                        result[idx] = Step.note(idx, pool.random(), randomVelocity(), randomGate())
                    }
                }
                else -> {
                    if (Random.nextInt(100) < 25) {
                        result[idx] = Step.silence(idx)
                    } else {
                        // MUTATE musical: cambia uno o varios parámetros de forma
                        // controlada según VARIATION (no siempre todos a la vez).
                        val newNote = if (Random.nextInt(100) < 70) pickNearby(pool, current.midiNote) else current.midiNote
                        val newVel = if (Random.nextInt(100) < 55) {
                            (current.velocity + randomInt(-20, 20)).coerceIn(1, 127)
                        } else {
                            current.velocity
                        }
                        val newGate = if (Random.nextInt(100) < 45) randomGate() else current.gate
                        val newOctave = if (Random.nextInt(100) < 30) {
                            (current.octave + randomInt(-1, 1)).coerceIn(-2, 2)
                        } else {
                            current.octave
                        }
                        result[idx] = Step.note(idx, newNote, newVel, newGate, newOctave)
                    }
                }
            }
        }

        return result
    }

    /** Elige una nota de la escala cercana a la dada (para mutaciones suaves). */
    private fun pickNearby(pool: List<Int>, current: Int): Int {
        val idx = pool.indexOfFirst { it == current }
        val base = if (idx < 0) pool.size / 2 else idx
        val offset = randomInt(-3, 3)
        return pool[(base + offset).coerceIn(0, pool.size - 1)]
    }

    private fun randomVelocity(): Int = randomInt(50, 110)
    private fun randomGate(): Float = (0.5f + Random.nextFloat() * 0.4f).coerceIn(0.05f, 1.0f)

    private fun randomInt(from: Int, to: Int): Int = Random.nextInt(from, to + 1)
}
