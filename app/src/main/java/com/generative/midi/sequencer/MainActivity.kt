package com.generative.midi.sequencer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.generative.midi.sequencer.midi.MidiManager
import com.generative.midi.sequencer.ui.AppRoot
import com.generative.midi.sequencer.ui.MainViewModel
import com.generative.midi.sequencer.ui.theme.GenerativeMidiTheme

/**
 * Actividad principal.
 *
 * Crea el [MidiManager] (capa MIDI) y el [MainViewModel], y monta la interfaz
 * Compose dentro del tema oscuro.
 */
class MainActivity : ComponentActivity() {

    private lateinit var midiManager: MidiManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        midiManager = MidiManager(applicationContext)

        val viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(applicationContext, midiManager) as T
                }
            }
        )[MainViewModel::class.java]

        setContent {
            GenerativeMidiTheme {
                AppRoot(viewModel = viewModel) { midiManager.close() }
            }
        }
    }

    override fun onDestroy() {
        // Asegurar limpieza MIDI (All Notes Off) si la actividad se destruye.
        super.onDestroy()
    }
}
