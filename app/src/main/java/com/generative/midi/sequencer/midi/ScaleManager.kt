package com.generative.midi.sequencer.midi

/**
 * Gestión de escalas musicales.
 *
 * Una escala se representa como una lista de intervalos (en semitonos) a partir
 * de una nota raíz. Cada escala lleva además:
 *  - una categoría (WESTERN / ASIAN / EXPERIMENTAL) para organizar la UI;
 *  - un peso "ambient" para priorizar escalas apropiadas para música ambiental;
 *  - un nombre alternativo cuando corresponde.
 *
 * La clase proporciona utilidades para:
 *  - obtener las notas de la escala en un rango de octavas;
 *  - trasponer/convertir una nota a la escala;
 *  - visualizar los nombres de las notas activas.
 *
 * La escala principal empleada es RYUKYU PENTATONIC (D – F – G – A – C).
 */
object ScaleManager {

    enum class Category { WESTERN, ASIAN, EXPERIMENTAL }

    /** Definición de las escalas disponibles. Los intervalos son semitonos desde la raíz. */
    enum class Scale(
        val displayName: String,
        val intervals: List<Int>,
        val category: Category,
        val ambientWeight: Int = 1,
        val altName: String? = null
    ) {
        // ---- Western ----
        CHROMATIC("Chromatic", listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11), Category.WESTERN, 0),
        MAJOR("Major", listOf(0, 2, 4, 5, 7, 9, 11), Category.WESTERN, 1, "Ionian"),
        MINOR("Minor", listOf(0, 2, 3, 5, 7, 8, 10), Category.WESTERN, 2, "Aeolian"),
        DORIAN("Dorian", listOf(0, 2, 3, 5, 7, 9, 10), Category.WESTERN, 3),
        PHRYGIAN("Phrygian", listOf(0, 1, 3, 5, 7, 8, 10), Category.WESTERN, 2),
        LYDIAN("Lydian", listOf(0, 2, 4, 6, 7, 9, 11), Category.WESTERN, 3),
        MIXOLYDIAN("Mixolydian", listOf(0, 2, 4, 5, 7, 9, 10), Category.WESTERN, 1),
        LOCRIAN("Locrian", listOf(0, 1, 3, 5, 6, 8, 10), Category.WESTERN, 1),
        MAJOR_PENTATONIC("Major Pentatonic", listOf(0, 2, 4, 7, 9), Category.WESTERN, 2),
        MINOR_PENTATONIC("Minor Pentatonic", listOf(0, 3, 5, 7, 10), Category.WESTERN, 3),
        WHOLE_TONE("Whole Tone", listOf(0, 2, 4, 6, 8, 10), Category.WESTERN, 2),

        // ---- Japanese / East Asian ----
        RYUKYU_PENTATONIC("Ryukyu", listOf(0, 3, 5, 7, 10), Category.ASIAN, 4),
        HIRAJOSHI("Hirajoshi", listOf(0, 2, 3, 7, 8), Category.ASIAN, 4),
        INSEN("Insen", listOf(0, 1, 5, 7, 10), Category.ASIAN, 4),
        KUMOI("Kumoi", listOf(0, 2, 3, 7, 9), Category.ASIAN, 4),
        YO("Yo", listOf(0, 2, 5, 7, 9), Category.ASIAN, 4),
        PELOG("Pelog", listOf(0, 1, 3, 7, 8), Category.ASIAN, 3),
        SLENDRO("Slendro", listOf(0, 2, 4, 7, 9), Category.ASIAN, 3),

        // ---- Experimental ----
        HARMONIC_MINOR("Harmonic Minor", listOf(0, 2, 3, 5, 7, 8, 11), Category.EXPERIMENTAL, 2),
        MELODIC_MINOR("Melodic Minor", listOf(0, 2, 3, 5, 7, 9, 11), Category.EXPERIMENTAL, 2),
        HUNGARIAN_MINOR("Hungarian Minor", listOf(0, 2, 3, 6, 7, 8, 11), Category.EXPERIMENTAL, 3),
        DOUBLE_HARMONIC("Double Harmonic", listOf(0, 1, 4, 5, 7, 8, 11), Category.EXPERIMENTAL, 2),
        PHRYGIAN_DOMINANT("Phrygian Dominant", listOf(0, 1, 4, 5, 7, 8, 10), Category.EXPERIMENTAL, 2);

        companion object {
            fun fromDisplayName(name: String): Scale =
                entries.firstOrNull { it.displayName.equals(name, ignoreCase = true) } ?: RYUKYU_PENTATONIC

            fun byId(id: String): Scale =
                entries.firstOrNull { it.name == id } ?: RYUKYU_PENTATONIC
        }
    }

    /** Lista de nombres de notas occidentales (C..B). */
    const val NOTES = "C|C#|D|D#|E|F|F#|G|G#|A|A#|B"
    val NOTE_NAMES: List<String> = NOTES.split("|")

    /** Nombres de las 12 raíces para KEY SELECT. */
    val ROOT_NAMES: List<String> = NOTE_NAMES

    /** Convierte el nombre de una raíz (p. ej. "D") a un número MIDI de octava 4 (D4 = 62). */
    fun rootNameToMidi(rootName: String): Int {
        val index = NOTE_NAMES.indexOfFirst { it == rootName }
        if (index < 0) return 62
        // Octava 4: la raíz queda en la octava 4 (C4 = 60).
        return 60 + index
    }

    /** Convierte un número MIDI a su representación "letra+octava" (p. ej. D4). */
    fun midiToName(midi: Int): String {
        if (midi < 0) return "-"
        val octave = (midi / 12) - 1
        val name = NOTE_NAMES[((midi % 12) + 12) % 12]
        return "$name$octave"
    }

    /** Convierte un número MIDI a su nombre de clase de nota (p. ej. 62 -> "D"). */
    fun midiToPitchClass(midi: Int): String {
        if (midi < 0) return "-"
        return NOTE_NAMES[((midi % 12) + 12) % 12]
    }

    /**
     * Devuelve la lista de notas MIDI (en la octava especificada) pertenecientes
     * a la escala con la raíz dada.
     */
    fun scaleNotes(scale: Scale, rootMidi: Int, octaveShift: Int = 0): List<Int> {
        val base = rootMidi + (octaveShift * 12)
        return scale.intervals.map { base + it }
    }

    /** Devuelve las clases de nota (0..11) de la escala, únicas y ordenadas. */
    fun scalePitchClasses(scale: Scale, rootMidi: Int): List<Int> {
        val rootClass = ((rootMidi % 12) + 12) % 12
        return scale.intervals.map { (rootClass + it) % 12 }.distinct().sorted()
    }

    /** Devuelve los nombres de nota de la escala (p. ej. [D, F, G, A, C]). */
    fun scaleNoteNames(scale: Scale, rootMidi: Int): List<String> {
        return scalePitchClasses(scale, rootMidi).map { NOTE_NAMES[it] }
    }

    /** Intervalos de acordes básicos (semitonos desde la fundamental). */
    object Chords {
        val MINOR = listOf(0, 3, 7)
        val MAJOR = listOf(0, 4, 7)
        val DOMINANT7 = listOf(0, 4, 7, 10)
        val MINOR7 = listOf(0, 3, 7, 10)
        val MAJOR7 = listOf(0, 4, 7, 11)
        val SUS2 = listOf(0, 2, 7)
        val SUS4 = listOf(0, 5, 7)
        val DIM = listOf(0, 3, 6)
    }
}
