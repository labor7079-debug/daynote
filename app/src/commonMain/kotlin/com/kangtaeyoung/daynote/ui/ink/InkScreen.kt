package com.kangtaeyoung.daynote.ui.ink

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 자유 필기 화면(Phase 5-C). Compose Canvas 잉크 — 마우스·손가락·S펜 공용.
 * S펜은 필압으로 굵기가 변하고, 펜을 뒤집으면 자동 지우개.
 * (텍스트 변환/저장은 5-C 후속 — 현재는 그리기·실행취소·지우기까지.)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InkScreen(onBack: () -> Unit) {
    val ink = rememberInkState()
    var eraser by remember { mutableStateOf(false) }

    val palette = listOf(
        MaterialTheme.colorScheme.onSurface,   // 잉크(기본)
        MaterialTheme.colorScheme.tertiary,    // 클레이
        MaterialTheme.colorScheme.primary,     // 슬레이트
        MaterialTheme.colorScheme.error,       // 붉은 교정
    )
    var color by remember { mutableStateOf(palette.first()) }
    var width by remember { mutableStateOf(3f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("필기") },
                navigationIcon = { TextButton(onClick = onBack) { Text("뒤로") } },
                actions = {
                    TextButton(onClick = { ink.undo() }) { Text("실행취소") }
                    TextButton(onClick = { ink.clear() }) { Text("전체지우기") }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            InkToolbar(
                eraser = eraser,
                onPen = { eraser = false },
                onEraser = { eraser = true },
                palette = palette,
                color = color,
                onColor = { color = it; eraser = false },
                width = width,
                onWidth = { width = it; eraser = false },
            )

            Text(
                "마우스·손가락·S펜으로 그리세요. S펜은 필압으로 굵기가 변하고, 뒤집으면 지우개가 됩니다.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
            ) {
                InkCanvas(
                    state = ink,
                    penColor = color,
                    strokeWidthDp = width,
                    eraser = eraser,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun InkToolbar(
    eraser: Boolean,
    onPen: () -> Unit,
    onEraser: () -> Unit,
    palette: List<Color>,
    color: Color,
    onColor: (Color) -> Unit,
    width: Float,
    onWidth: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ModeButton("펜", selected = !eraser, onClick = onPen)
        ModeButton("지우개", selected = eraser, onClick = onEraser)

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            palette.forEach { c ->
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(c)
                        .border(
                            width = if (!eraser && c == color) 3.dp else 1.dp,
                            color = if (!eraser && c == color) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            shape = CircleShape,
                        )
                        .clickable { onColor(c) },
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf("가늘게" to 2f, "보통" to 3.5f, "굵게" to 7f).forEach { (label, w) ->
                ModeButton(label, selected = !eraser && width == w, onClick = { onWidth(w) })
            }
        }
    }
}

@Composable
private fun ModeButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        FilledTonalButton(onClick = onClick) { Text(label) }
    } else {
        TextButton(onClick = onClick) { Text(label) }
    }
}
