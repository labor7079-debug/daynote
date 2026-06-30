package com.kangtaeyoung.daynote.ui.components

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/** 하단 1차 내비게이션 — 캘린더(홈) · 메모 · 할 일. */
enum class TopDestination { Calendar, Notes, Todo }

@Composable
fun DayNoteBottomBar(
    current: TopDestination,
    onSelect: (TopDestination) -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            selected = current == TopDestination.Calendar,
            onClick = { onSelect(TopDestination.Calendar) },
            icon = { Text("Calendar") },
        )
        NavigationBarItem(
            selected = current == TopDestination.Notes,
            onClick = { onSelect(TopDestination.Notes) },
            icon = { Text("Memo") },
        )
        NavigationBarItem(
            selected = current == TopDestination.Todo,
            onClick = { onSelect(TopDestination.Todo) },
            icon = { Text("To-Do") },
        )
    }
}
