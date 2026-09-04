package com.generative.midi.sequencer.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Páginas disponibles en la navegación inferior. */
private enum class Page(val label: String, val icon: String) {
    SEQ("SEQ", "▦"),
    KEY("KEY", "♩"),
    ARP("ARP", "⊥"),
    TRACK("TRK", "⊞"),
    PROG("PROG", "Ⅰ"),
    CHSEQ("CHSEQ", "◫"),
    CHMAP("CHMAP", "◎"),
    OPT("OPT", "⚙")
}

/**
 * Raíz de la interfaz. Mantiene la página activa e inyecta la barra de
 * navegación inferior con acceso a las 8 secciones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(viewModel: MainViewModel, onDispose: () -> Unit) {
    var currentPage by rememberSaveable { mutableIntStateOf(0) }
    val pages = Page.entries

    Scaffold(
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                pages.forEachIndexed { index, page ->
                    NavigationBarItem(
                        selected = currentPage == index,
                        onClick = { currentPage = index },
                        icon = { Text(page.icon, fontSize = 16.sp) },
                        label = { Text(page.label, fontSize = 9.sp, fontWeight = FontWeight.SemiBold) },
                        colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                            selectedTextColor = Color(0xFFFFFFFF),
                            indicatorColor = Color(0xFF2E7D32)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)
        when (pages[currentPage]) {
            Page.SEQ -> MainScreen(viewModel, onDispose)
            Page.KEY -> KeySelectScreen(viewModel)
            Page.ARP -> ArpeggiatorScreen(viewModel)
            Page.TRACK -> TracksScreen(viewModel)
            Page.PROG -> ProgressionScreen(viewModel)
            Page.CHSEQ -> ChordSequencerScreen(viewModel)
            Page.CHMAP -> ChordMapScreen(viewModel)
            Page.OPT -> OptionsScreen(viewModel)
        }
    }
}
