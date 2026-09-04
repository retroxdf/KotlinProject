package com.abtsplazita.posplazita

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    initializeFirebase()
    val windowState = rememberWindowState(placement = WindowPlacement.Maximized)

    Window(
        onCloseRequest = ::exitApplication,
        title = "Plazita POS v1.0.3",
        state = windowState
    ) {
        App()
    }
}
