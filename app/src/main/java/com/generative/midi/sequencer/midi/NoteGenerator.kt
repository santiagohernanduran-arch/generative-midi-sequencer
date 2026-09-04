package com.generative.midi.sequencer.midi

import kotlin.random.Random

/**
 * Motor de generación de notas basado en reglas musicales.
 *
 * El objetivo NO es producir un ruido aleatorio caótico, sino patrones musicales
 * apropiados para ambient / minimal. Para ello se utilizan estas estrategias:
 *
 *  - **Frases de 4, 8 y 16 pasos**: la secuencia se divide en frases y cada frase
 *    comienza, con cierta probabilidad, desde un punto estable (repetición de la
 *    nota de inicio o movimiento conjunto).
 *  - **Repetición de motivos**: es probable repetir la nota anterior.
 *  - **Movimiento conjunto**: la mayoría de las notas sucesivas se mueven por
 *    pasos conjuntos (intervalos pequeños dentro de la escala).
 *  - **Saltos ocasionales**: solo ocasionalmente se producen saltos grandes,
 *    controlados por RANDOMNESS.
 *  - **Silencios**: según la densidad, se intercalan silencios.
 *  - **Variaciones de octava controladas**: los cambios de octava se producen
 *    con probabilidad limitada y dentro del rango elegido.
 *  - **Resolución periódica**: al final de cada frase hay probabilidad de resolver
 *    a la raíz o a un grado estable de la escala.
 *
 * Esta clase solo genera objetos [Step]; no depende de la UI ni de MIDI I/O.
 */
object NoteGenerator {

    /**
     * Genera una secuencia de [Step] respetando estrictamente la escala.
     *
     * @param length cantidad de pasos a generar (= STEP LENGTH).
     * @param scale escala musical a respetar.
     * @param rootMidi raíz MIDI de base.
     * @param density 0..100 probabilidad (porcentaje) de que un paso suene (vs silencio).
     * @param randomness 0..100 cuánta imprevisibilidad en la elección de notas y saltos.
     * @param octaveLow desplazamiento de octava inferior (p. ej. -1).
     * @param octaveHigh desplazamiento de octava superior (p. ej. +1).
     * @param ambient si true, aplica estilo ambient.
     * @param random generador de aleatoriedad (para permitir repetibilidad).
     */
    fun generate(
        length: Int = 64,
        scale: ScaleManager.Scale,
        rootMidi: Int,
        density: Int,
        randomness: Int,
        octaveLow: Int,
        octaveHigh: Int,
        ambient: Boolean,
        random: Random = Random.Default
    ): List<Step> {

        val pool = mutableListOf<Int>()
        for (oct in octaveLow..octaveHigh) {
            pool.addAll(ScaleManager.scaleNotes(scale, rootMidi, oct))
        }
        pool.sort()

        // Densidad efectiva. En modo ambient se reduce para aumentar silencios.
        val effDensity: Double = if (ambient) (density * 0.35).coerceIn(0.0, 100.0) else density.toDouble()
        val silenceChance = 1.0 - (effDensity / 100.0)

        // Factores suavizados por randomness.
        val repeatChance = (0.45 + (1.0 - randomness / 100.0) * 0.25).coerceIn(0.0, 0.85)
        val stepMotionChance = (0.55 + (1.0 - randomness / 100.0) * 0.3).coerceIn(0.3, 0.9)
        // En ambient favorecemos movimiento aún más suave y repetición.
        val motionUp = if (ambient) 2 else 3

        val steps = mutableListOf<Step>()
        var prev = 0
        var phraseIndex = 0
        var motifPhase = 0
        val phraseBase = listOf(0, 4, 8, 16)

        for (i in 0 until length) {
            // Marcar resolución al inicio/cambio de frase (4, 8 o 16 pasos).
            phraseIndex = i % 16
            if (phraseIndex == 0) {
                motifPhase = (motifPhase + 1) % pool.size
            }

            // Decidir silencio.
            if (random.nextDouble() < silenceChance) {
                // Con baja probabilidad un silencio al principio de frase se rellena
                // si la densidad no es mínima, para mantener el pulso musical.
                steps.add(Step.silence(i))
                continue
            }

            // Elección de nota: al inicio de frase, tendencia a asentar un motivo.
            val note: Int = when {
                steps.isEmpty() || prev == 0 -> randomNoteFromPool(pool, random, pHint = motifPhase)
                phraseIndex in phraseBase && random.nextDouble() < 0.35 ->
                    pool[(motifPhase % pool.size).coerceIn(0, pool.size - 1)]
                else -> chooseNextNote(prev, pool, repeatChance, stepMotionChance, motionUp, random)
            }
            prev = note

            val velocity = computeVelocity(density, ambient, random)
            val gate = if (ambient) 0.9f + random.nextFloat() * 0.1f else 0.55f + random.nextFloat() * 0.4f
            steps.add(Step.note(i, note, velocity, gate.coerceIn(0.05f, 1.0f)))
        }

        if (steps.isEmpty()) {
            return (0 until length).map { Step.silence(it) }
        }
        return steps
    }

    /**
     * Devuelve todas las notas de la escala en un rango de octavas dado.
     * Utilidad para la edición manual de pasos.
     */
    fun scaleNotesFromRange(
        scale: ScaleManager.Scale,
        rootMidi: Int,
        octaveLow: Int,
        octaveHigh: Int
    ): List<Int> {
        val pool = mutableListOf<Int>()
        for (oct in octaveLow..octaveHigh) {
            pool.addAll(ScaleManager.scaleNotes(scale, rootMidi, oct))
        }
        return pool.distinct().sorted()
    }

    /** Nota aleatoria del pool, con sesgo hacia un índice sugerido (motivo). */
    private fun randomNoteFromPool(pool: List<Int>, random: Random, pHint: Int = -1): Int {
        if (pool.isEmpty()) return 60
        if (pHint >= 0 && random.nextDouble() < 0.5) {
            return pool[(pHint % pool.size).coerceIn(0, pool.size - 1)]
        }
        return pool[random.nextInt(pool.size)]
    }

    /**
     * Elige la siguiente nota aplicando reglas musicales:
     *  - alta probabilidad de repetir (motivo);
     *  - movimiento conjunto dentro de la escala;
     *  - saltos ocasionales.
     */
    private fun chooseNextNote(
        previous: Int,
        pool: List<Int>,
        repeatChance: Double,
        stepMotionChance: Double,
        motionUp: Int,
        random: Random
    ): Int {
        if (pool.isEmpty()) return 60
        val idx = pool.indexOf(previous)
        val baseIdx = if (idx < 0) pool.size / 2 else idx

        // 1) Repetición frecuente (motivo).
        if (random.nextDouble() < repeatChance) {
            return previous
        }

        // 2) Movimiento conjunto: elegir vecino (índice ±1..±motionUp) dentro del pool.
        if (random.nextDouble() < stepMotionChance) {
            val direction = if (random.nextBoolean()) 1 else -1
            val stepSize = 1 + random.nextInt(motionUp)
            val target = (baseIdx + direction * stepSize).coerceIn(0, pool.size - 1)
            return pool[target]
        }

        // 3) Salto ocasional.
        val jump = random.nextInt(3, 7) * (if (random.nextBoolean()) 1 else -1)
        val target = (baseIdx + jump).coerceIn(0, pool.size - 1)
        return pool[target]
    }

    /** Calcula la velocidad según densidad / ambient. Más suave en ambient. */
    private fun computeVelocity(density: Int, ambient: Boolean, random: Random): Int {
        val base = if (ambient) 55 else 70
        val spread = if (ambient) 25 else 45
        return (base + random.nextInt(spread)).coerceIn(1, 127)
    }
}
