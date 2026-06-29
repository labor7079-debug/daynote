package com.kangtaeyoung.daynote

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kangtaeyoung.daynote.ui.theme.DayNoteTheme

/** 공유 진입 컴포저블 — Android·Desktop 양쪽이 이 하나를 호출한다. */
@Composable
fun App() {
    DayNoteTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            PlaceholderScreen(Modifier.padding(innerPadding))
        }
    }
}

/** Phase 0.5 자리표시 화면. Phase 1~2에서 캘린더 홈으로 교체된다. */
@Composable
private fun PlaceholderScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("DayNote", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Phase 0.5 — Compose Multiplatform",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
