package com.kangtaeyoung.daynote.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kangtaeyoung.daynote.core.atTimeMillis
import com.kangtaeyoung.daynote.core.hourOfDay
import com.kangtaeyoung.daynote.core.isMidnight
import com.kangtaeyoung.daynote.core.minuteOfHour
import com.kangtaeyoung.daynote.core.startOfDayMillis
import com.kangtaeyoung.daynote.core.toLocalDate
import com.kangtaeyoung.daynote.domain.model.Task
import com.kangtaeyoung.daynote.ui.theme.CloseXIcon
import kotlinx.datetime.LocalDate

/**
 * 할 일 수정 다이얼로그 — 내용·시작 날짜/시각·종료 날짜/시각을 한 번에 고친다.
 * 몇 시간짜리(같은 날 시각 범위)도, 여러 날에 걸친 기간도 표현할 수 있다:
 * 종료는 [Task.endDate] 한 필드에 담는다(날짜만 지정=자정 millis, 시각까지 지정=그 시각 millis).
 *
 * 날짜 없는 할 일(To-Do 탭에서 추가)은 날짜·시각을 건드리지 않고 내용만 고치면
 * 계속 날짜 없이 남는다(무심코 캘린더에 얹히는 것 방지).
 */
@Composable
fun EditTaskDialog(
    task: Task,
    onSave: (Task) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(task.text) }
    var startDate by remember { mutableStateOf((task.dueDate ?: task.createdAt).toLocalDate()) }
    var allDay by remember { mutableStateOf(task.allDay) }
    var startHour by remember { mutableStateOf(task.dueDate?.takeIf { !task.allDay }?.hourOfDay() ?: 9) }
    var startMinute by remember { mutableStateOf(task.dueDate?.takeIf { !task.allDay }?.minuteOfHour() ?: 0) }
    var endDate by remember { mutableStateOf(task.endDate?.toLocalDate()) }
    var endTimeOn by remember { mutableStateOf(task.endDate?.let { !it.isMidnight() } ?: false) }
    var endHour by remember { mutableStateOf(task.endDate?.takeIf { !it.isMidnight() }?.hourOfDay() ?: 10) }
    var endMinute by remember { mutableStateOf(task.endDate?.takeIf { !it.isMidnight() }?.minuteOfHour() ?: 0) }
    // 날짜/시각을 한 번도 안 건드렸으면 원래 일정(없음 포함)을 그대로 보존한다.
    var scheduleTouched by remember { mutableStateOf(false) }

    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
        ) {
            Column(
                modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("할 일 수정", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("내용") },
                    modifier = Modifier.fillMaxWidth(),
                )

                // 시작 날짜 + 종일 토글
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showStartPicker = true }) {
                        Text("시작: ${startDate.monthNumber}월 ${startDate.dayOfMonth}일")
                    }
                    Text("종일", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = allDay, onCheckedChange = { allDay = it; scheduleTouched = true })
                }

                if (!allDay) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("시작 시각", style = MaterialTheme.typography.bodyMedium)
                        NumberDropdown("시", startHour, 0..23, { startHour = it; scheduleTouched = true }, Modifier.width(64.dp))
                        Text(":")
                        NumberDropdown("분", startMinute, 0..59, { startMinute = it; scheduleTouched = true }, Modifier.width(64.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("종료 시각", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = endTimeOn, onCheckedChange = { endTimeOn = it; scheduleTouched = true })
                        if (endTimeOn) {
                            NumberDropdown("시", endHour, 0..23, { endHour = it; scheduleTouched = true }, Modifier.width(64.dp))
                            Text(":")
                            NumberDropdown("분", endMinute, 0..59, { endMinute = it; scheduleTouched = true }, Modifier.width(64.dp))
                        }
                    }
                }

                // 종료일 — 하루를 넘겨 수행하는 일이면 지정(같은 날 시각 범위는 종료 시각만으로 충분).
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = { showEndPicker = true }) {
                        Text(
                            endDate?.let { "종료일: ${it.monthNumber}월 ${it.dayOfMonth}일" } ?: "종료일: 없음 (지정)",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (endDate != null) {
                        IconButton(onClick = { endDate = null; scheduleTouched = true }) {
                            Icon(
                                CloseXIcon,
                                contentDescription = "종료일 지우기",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("취소") }
                    TextButton(
                        enabled = text.isNotBlank(),
                        onClick = {
                            val keepOriginalSchedule = !scheduleTouched && task.dueDate == null
                            val due = when {
                                keepOriginalSchedule -> null
                                allDay -> startDate.startOfDayMillis()
                                else -> startDate.atTimeMillis(startHour, startMinute)
                            }
                            val end = when {
                                keepOriginalSchedule || due == null -> null
                                !allDay && endTimeOn -> (endDate ?: startDate).atTimeMillis(endHour, endMinute)
                                else -> endDate?.startOfDayMillis()
                            }?.takeIf { it > due!! }
                            onSave(
                                task.copy(
                                    text = text.trim(),
                                    dueDate = due,
                                    allDay = if (keepOriginalSchedule) task.allDay else allDay,
                                    endDate = end,
                                ),
                            )
                        },
                    ) { Text("저장") }
                }
            }
        }
    }

    if (showStartPicker) {
        MiniCalendarDialog(
            initial = startDate,
            title = "시작일 선택",
            pickMode = true,
            onPick = { picked ->
                startDate = picked
                scheduleTouched = true
                // 시작일이 종료일을 넘어서면 종료일을 지운다(역전 방지).
                if (endDate?.let { it < picked } == true) endDate = null
                showStartPicker = false
            },
            onDismiss = { showStartPicker = false },
        )
    }
    if (showEndPicker) {
        MiniCalendarDialog(
            initial = endDate ?: startDate,
            title = "종료일 선택 (시작: ${startDate.monthNumber}월 ${startDate.dayOfMonth}일)",
            pickMode = true,
            onPick = { picked ->
                endDate = picked.takeIf { it >= startDate }
                scheduleTouched = true
                showEndPicker = false
            },
            onDismiss = { showEndPicker = false },
        )
    }
}

/** 이 할 일이 하루를 넘겨 걸치는지(캘린더 bar 대상). 같은 날 시각 범위(몇 시간짜리)는 false. */
fun Task.spansDays(): Boolean {
    val end = endDate ?: return false
    return end.toLocalDate() > (dueDate ?: createdAt).toLocalDate()
}
