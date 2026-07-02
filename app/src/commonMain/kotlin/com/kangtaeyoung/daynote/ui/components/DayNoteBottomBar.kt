package com.kangtaeyoung.daynote.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kangtaeyoung.daynote.ui.theme.SettingsGearIcon

/** 하단 1차 내비게이션 — 캘린더(홈) · 메모 · 할 일. */
enum class TopDestination { Calendar, Notes, Todo }

@Composable
fun DayNoteBottomBar(
    current: TopDestination,
    onSelect: (TopDestination) -> Unit,
    onOpenSettings: (() -> Unit)? = null,
) {
    // 배경을 앱 바탕(Paper)에 녹여 튀지 않게 — 투명 컨테이너 + 옅은 상단 헤어라인만.
    Column {
        HorizontalDivider(thickness = Dp.Hairline, color = MaterialTheme.colorScheme.outlineVariant)
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
        ) {
            // 선택 표시는 절제된 웜 톤(secondaryContainer). 라벤더 기본값을 쓰지 않는다.
            val itemColors = NavigationBarItemDefaults.colors(
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
            )
            NavigationBarItem(
                selected = current == TopDestination.Calendar,
                onClick = { onSelect(TopDestination.Calendar) },
                icon = { Text("Calendar") },
                colors = itemColors,
            )
            NavigationBarItem(
                selected = current == TopDestination.Notes,
                onClick = { onSelect(TopDestination.Notes) },
                icon = { Text("Memo") },
                colors = itemColors,
            )
            NavigationBarItem(
                selected = current == TopDestination.Todo,
                onClick = { onSelect(TopDestination.Todo) },
                icon = { Text("To-Do") },
                colors = itemColors,
            )
            // 설정(톱니) — 상단바에서 하단으로 이동. 탭 선택 상태와 무관한 이동 버튼.
            if (onOpenSettings != null) {
                NavigationBarItem(
                    selected = false,
                    onClick = onOpenSettings,
                    icon = {
                        Icon(
                            imageVector = SettingsGearIcon,
                            contentDescription = "설정",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    colors = itemColors,
                )
            }
        }
    }
}
