package com.heisthunt.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "HeistHunt - 경찰과 도둑"
    ) {
        App()
    }
}
