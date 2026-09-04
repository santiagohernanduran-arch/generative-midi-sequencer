package com.generative.midi.sequencer.midi

/**
 * Representa un paso del secuenciador.
 *
 * Cada paso puede contener una nota (o un acorde) con su velocidad y duración de
 * gate, o ser un silencio. Los pasos se ordenan en un patrón de hasta 64 pasos.
 *
 * Un paso simple usa [midiNote] (la única nota). Un paso de acorde establece
 * [midiNote] como la fundamental y [notes] con todas las notas del acorde.
 * Cuando [notes] está vacía, el transporte reproduce solo [midiNote], lo que
 * mantiene compatibilidad total con la reproducción de pasos simples (V1).
 *
 * @property stepIndex índice del paso dentro del patrón (0..63).
 * @property midiNote número de nota MIDI (60 = C4). -1 indica silencio.
 * @property velocity velocidad de Note On (1..127).
 * @property gate fracción de la duración del paso durante la que la nota suena
 *               (0.0..1.0). Se usa para darle groove / variación de articulación.
 * @property octave desplazamiento de octava relativo del paso (-2..2). Se suma a
 *               [midiNote] (y a las notas del acorde) en la reproducción MIDI.
 * @property active true = el paso suena; false = REST (se silencia pero se
 *               conservan su nota/velocidad/gate para restaurarlos después).
 * @property notes notas adicionales (las del acorde). Si está vacía, es un paso
 *               de nota simple.
 */
data class Step(
    val stepIndex: Int,
    val midiNote: Int = -1,
    val velocity: Int = 100,
    val gate: Float = 0.9f,
    val octave: Int = 0,
    val active: Boolean = true,
    val notes: List<Int> = emptyList()
) {
    /** true si el paso es un silencio (no posee nota). */
    val isSilence: Boolean get() = midiNote < 0

    /** Un REST (Active=false) no emite ninguna nota pero conserva sus datos. */
    val isRest: Boolean get() = !active

    /** Lista completa de notas a reproducir (fundamental + acorde). */
    val allNotes: List<Int>
        get() = if (notes.isEmpty()) {
            if (midiNote < 0) emptyList() else listOf(midiNote)
        } else {
            notes
        }

    companion object {
        /** Crea un paso de silencio. */
        fun silence(index: Int) = Step(stepIndex = index, midiNote = -1, velocity = 0, gate = 0f, active = false)

        /** Crea un paso con nota. */
        fun note(index: Int, midiNote: Int, velocity: Int = 100, gate: Float = 0.9f, octave: Int = 0, active: Boolean = true) =
            Step(stepIndex = index, midiNote = midiNote, velocity = velocity, gate = gate, octave = octave, active = active)

        /** Crea un paso de acorde: [midiNote] es la fundamental y [notes] las notas del acorde. */
        fun chord(index: Int, midiNote: Int, notes: List<Int>, velocity: Int = 90, gate: Float = 0.9f, octave: Int = 0, active: Boolean = true) =
            Step(
                stepIndex = index,
                midiNote = midiNote,
                velocity = velocity,
                gate = gate,
                octave = octave,
                active = active,
                notes = notes.filter { it in 0..127 }
            )
    }
}
