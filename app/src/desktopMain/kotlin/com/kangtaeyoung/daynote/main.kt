package com.kangtaeyoung.daynote

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.kangtaeyoung.daynote.di.initKoin

fun main() {
    initKoin()
    application {
        // 넉넉한 기본 크기 — Expanded(≥840dp)라 캘린더 2단(달력+상세)이 기본 노출된다.
        val state = rememberWindowState(size = DpSize(1180.dp, 780.dp))
        Window(onCloseRequest = ::exitApplication, state = state, title = "DayNote") {
            App()
        }
    }
}
