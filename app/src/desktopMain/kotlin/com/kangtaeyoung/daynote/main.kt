package com.kangtaeyoung.daynote

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.kangtaeyoung.daynote.di.initKoin

fun main() {
    initKoin()
    application {
        Window(onCloseRequest = ::exitApplication, title = "DayNote") {
            App()
        }
    }
}
