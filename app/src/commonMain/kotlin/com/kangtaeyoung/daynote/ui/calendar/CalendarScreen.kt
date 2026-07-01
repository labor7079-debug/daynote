package com.kangtaeyoung.daynote.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kangtaeyoung.daynote.core.firstOfMonthPlusMonths
import com.kangtaeyoung.daynote.core.monthGridDays
import com.kangtaeyoung.daynote.core.startOfDayMillis
import com.kangtaeyoung.daynote.core.today
import com.kangtaeyoung.daynote.core.toMillisRange
import com.kangtaeyoung.daynote.core.weekDays
import com.kangtaeyoung.daynote.domain.model.Note
import com.kangtaeyoung.daynote.domain.model.Task
import com.kangtaeyoung.daynote.domain.usecase.AddTaskUseCase
import com.kangtaeyoung.daynote.domain.usecase.DeleteNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.DeleteTaskUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveNotesByDateUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveTasksByDateUseCase
import com.kangtaeyoung.daynote.domain.usecase.ToggleTaskUseCase
import com.kangtaeyoung.daynote.ui.components.DayNoteBottomBar
import com.kangtaeyoung.daynote.ui.components.TaskRow
import com.kangtaeyoung.daynote.ui.components.TopDestination
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import org.koin.compose.koinInject

private val weekdayHeaders = listOf("월", "화", "수", "목", "금", "토", "일")

private fun koreanDow(dow: DayOfWeek): String = weekdayHeaders[dow.isoDayNumber - 1]
private fun LocalDate.monthLabel(): String = "${year}년 ${monthNumber}월"
private fun LocalDate.dayLabel(): String = "${year}년 ${monthNumber}월 ${dayOfMonth}일 (${koreanDow(dayOfWeek)})"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onOpenNote: (String) -> Unit,
    onAddNoteForDate: (Long) -> Unit,
    onSelectDestination: (TopDestination) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val observeNotesByDate = koinInject<ObserveNotesByDateUseCase>()
    val observeTasksByDate = koinInject<ObserveTasksByDateUseCase>()
    val addTask = koinInject<AddTaskUseCase>()
    val toggleTask = koinInject<ToggleTaskUseCase>()
    val deleteTask = koinInject<DeleteTaskUseCase>()
    val deleteNote = koinInject<DeleteNoteUseCase>()
    val vm = viewModel {
        CalendarViewModel(observeNotesByDate, observeTasksByDate, addTask, toggleTask, deleteTask, deleteNote)
    }

    val selectedDate by vm.selectedDate.collectAsState()
    val notesByDate by vm.notesByDate.collectAsState()
    val notesForSelected by vm.notesForSelected.collectAsState()
    val tasksForSelected by vm.tasksForSelected.collectAsState()

    var anchor by remember { mutableStateOf(today()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                actions = { TextButton(onClick = onOpenSettings) { Text("Settings") } },
            )
        },
        bottomBar = { DayNoteBottomBar(current = TopDestination.Calendar, onSelect = onSelectDestination) },
    ) { padding ->
        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            val compact = maxWidth < 600.dp
            // Expanded(≥840dp — 탭 가로·폴드 펼침·PC): 좌 월 달력 + 우 상세 마스터-디테일 2단.
            val twoPane = maxWidth >= 840.dp
            val visibleDays = if (compact) anchor.weekDays() else monthGridDays(anchor)

            LaunchedEffect(visibleDays.first(), visibleDays.last()) {
                val (start, end) = visibleDays.toMillisRange()
                vm.setVisibleRange(start, end)
            }

            // 이전/다음 이동(버튼·스와이프 공용). compact=주 단위, 아니면 월 단위.
            // navDir: 슬라이드 방향(+1=다음/오른쪽에서, -1=이전/왼쪽에서).
            var navDir by remember { mutableStateOf(1) }
            val goPrev = { navDir = -1; anchor = if (compact) anchor.plus(-7, DateTimeUnit.DAY) else anchor.firstOfMonthPlusMonths(-1) }
            val goNext = { navDir = 1; anchor = if (compact) anchor.plus(7, DateTimeUnit.DAY) else anchor.firstOfMonthPlusMonths(1) }

            // 달력(헤더 + 그리드/아젠다) — 1단·2단 배치가 공유. 가로 스와이프로 이전/다음(왼쪽=다음, 오른쪽=이전).
            val calendarArea: @Composable () -> Unit = {
                CalendarHeader(
                    label = if (compact) "${visibleDays.first().monthNumber}월 ${visibleDays.first().dayOfMonth}일 주" else anchor.monthLabel(),
                    onPrev = goPrev,
                    onNext = goNext,
                    onToday = { anchor = today(); vm.selectDate(today()) },
                )
                Box(
                    modifier = Modifier.fillMaxWidth().pointerInput(compact) {
                        var total = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { total = 0f },
                            onDragEnd = {
                                val threshold = 56.dp.toPx()
                                when {
                                    total <= -threshold -> goNext()
                                    total >= threshold -> goPrev()
                                }
                            },
                            onHorizontalDrag = { _, dragAmount -> total += dragAmount },
                        )
                    },
                ) {
                    // 이전/다음 전환에 가로 슬라이드 애니메이션(방향은 navDir).
                    AnimatedContent(
                        targetState = anchor,
                        transitionSpec = {
                            val dir = navDir
                            (slideInHorizontally(tween(280)) { w -> dir * w } + fadeIn(tween(280)))
                                .togetherWith(slideOutHorizontally(tween(280)) { w -> -dir * w } + fadeOut(tween(280)))
                        },
                        label = "calendar-nav",
                    ) { a ->
                        val days = if (compact) a.weekDays() else monthGridDays(a)
                        if (compact) {
                            WeekAgenda(days, selectedDate, notesByDate, onSelect = vm::selectDate, onOpenNote = onOpenNote)
                        } else {
                            MonthGrid(a, days, selectedDate, notesByDate, onSelect = vm::selectDate, onOpenNote = onOpenNote)
                        }
                    }
                }
            }

            val detailArea: @Composable () -> Unit = {
                DayDetail(
                    date = selectedDate,
                    notes = notesForSelected,
                    tasks = tasksForSelected,
                    onOpenNote = onOpenNote,
                    onAddNote = { onAddNoteForDate(selectedDate.startOfDayMillis()) },
                    onDeleteNote = vm::removeNote,
                    onAddTask = vm::addTaskForSelectedDate,
                    onToggleTask = vm::toggle,
                    onDeleteTask = vm::removeTask,
                )
            }

            if (twoPane) {
                // 좌우 2단 — 같은 선택 날짜 상태를 공유(달력 칸 탭 → 우측 상세 즉시 갱신).
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.weight(0.58f).fillMaxHeight().verticalScroll(rememberScrollState()),
                    ) { calendarArea() }
                    VerticalDivider()
                    Column(
                        modifier = Modifier.weight(0.42f).fillMaxHeight().verticalScroll(rememberScrollState())
                            .padding(top = 12.dp),
                    ) { detailArea() }
                }
            } else {
                // 1단(스택) — 폰=주 아젠다, 태블릿 세로/폴드 접힘=월 달력, 아래 상세.
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    calendarArea()
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    detailArea()
                }
            }
        }
    }
}

@Composable
private fun CalendarHeader(label: String, onPrev: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TextButton(onClick = onPrev) { Text("◀") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onToday) { Text("오늘") }
        }
        TextButton(onClick = onNext) { Text("▶") }
    }
}

@Composable
private fun MonthGrid(
    anchor: LocalDate,
    days: List<LocalDate>,
    selected: LocalDate,
    notesByDate: Map<LocalDate, List<Note>>,
    onSelect: (LocalDate) -> Unit,
    onOpenNote: (String) -> Unit,
) {
    // 요일 헤더 + 6주 행을 세로로 쌓는다. Column 이 없으면(부모가 Box/AnimatedContent) 행들이 겹쳐
    // "한 줄"처럼 보인다 — 반드시 Column 으로 감싼다.
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            weekdayHeaders.forEach { d ->
                Text(
                    text = d,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(4.dp),
                )
            }
        }
        val today = today()
        days.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                week.forEach { day ->
                    val inMonth = day.monthNumber == anchor.monthNumber && day.year == anchor.year
                    DayCell(
                        day = day,
                        inMonth = inMonth,
                        isSelected = day == selected,
                        isToday = day == today,
                        notes = notesByDate[day].orEmpty(),
                        onClick = { onSelect(day) },
                        onOpenNote = onOpenNote,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    inMonth: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    notes: List<Note>,
    onClick: () -> Unit,
    onOpenNote: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .heightIn(min = 72.dp)
            .padding(4.dp),
    ) {
        Text(
            text = day.dayOfMonth.toString(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isToday -> MaterialTheme.colorScheme.primary
                !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
        notes.take(2).forEach { note ->
            Text(
                text = note.title.ifBlank { "(제목 없음)" },
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().clickable { onOpenNote(note.id) },
            )
        }
        if (notes.size > 2) {
            Text(
                text = "+${notes.size - 2}개 더",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun WeekAgenda(
    days: List<LocalDate>,
    selected: LocalDate,
    notesByDate: Map<LocalDate, List<Note>>,
    onSelect: (LocalDate) -> Unit,
    onOpenNote: (String) -> Unit,
) {
    val today = today()
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        days.forEach { day ->
            val notes = notesByDate[day].orEmpty()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (day == selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                    .clickable { onSelect(day) }
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${koreanDow(day.dayOfWeek)}\n${day.dayOfMonth}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (day == today) FontWeight.Bold else FontWeight.Normal,
                    color = if (day == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(end = 12.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    if (notes.isEmpty()) {
                        Text("—", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        notes.take(3).forEach { n ->
                            Text(
                                text = n.title.ifBlank { "(제목 없음)" },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth().clickable { onOpenNote(n.id) },
                            )
                        }
                        if (notes.size > 3) {
                            Text("+${notes.size - 3}개 더", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayDetail(
    date: LocalDate,
    notes: List<Note>,
    tasks: List<Task>,
    onOpenNote: (String) -> Unit,
    onAddNote: () -> Unit,
    onDeleteNote: (String) -> Unit,
    onAddTask: (String, Boolean, Int, Int) -> Unit,
    onToggleTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(date.dayLabel(), style = MaterialTheme.typography.titleMedium)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Memo", style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = onAddNote) { Text("+ Memo 추가") }
        }
        if (notes.isEmpty()) {
            // 빈 안내 영역을 탭해도 곧바로 새 메모 작성으로 진입한다(+ Memo 버튼과 동일).
            Text(
                "이 날의 Memo가 없습니다. 탭하여 추가하세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAddNote)
                    .padding(vertical = 12.dp),
            )
        }
        notes.forEach { note ->
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f).clickable { onOpenNote(note.id) }.padding(vertical = 6.dp)) {
                    Text(note.title.ifBlank { "(제목 없음)" }, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (note.content.isNotBlank()) {
                        Text(note.content.trim(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                TextButton(onClick = { onDeleteNote(note.id) }) { Text("삭제") }
            }
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text("To-Do", style = MaterialTheme.typography.titleSmall)
        tasks.forEach { task ->
            TaskRow(task = task, onToggle = { onToggleTask(task.id) }, onDelete = { onDeleteTask(task.id) })
        }
        TaskQuickAdd(onAdd = onAddTask)
        Box(modifier = Modifier.heightIn(min = 24.dp))
    }
}

@Composable
private fun TaskQuickAdd(onAdd: (String, Boolean, Int, Int) -> Unit) {
    var text by remember { mutableStateOf("") }
    var allDay by remember { mutableStateOf(true) }
    var hour by remember { mutableStateOf(9) }
    var minute by remember { mutableStateOf(0) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("이 날 To-Do 추가") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = {
                onAdd(text, allDay, hour, minute)
                text = ""
            }) { Text("추가") }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("종일", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = allDay, onCheckedChange = { allDay = it })
            if (!allDay) {
                NumberDropdown(label = "시", value = hour, range = 0..23, onValue = { hour = it }, modifier = Modifier.width(96.dp))
                Text(":")
                NumberDropdown(label = "분", value = minute, range = 0..59, onValue = { minute = it }, modifier = Modifier.width(96.dp))
            }
        }
    }
}

/** 시/분 선택 — 드롭다운 목록에서 고르거나 숫자를 직접 입력. (시계 다이얼 대체) */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NumberDropdown(
    label: String,
    value: Int,
    range: IntRange,
    onValue: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var text by remember(value) { mutableStateOf(value.toString().padStart(2, '0')) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { input ->
                val digits = input.filter { it.isDigit() }.take(2)
                text = digits
                digits.toIntOrNull()?.let { if (it in range) onValue(it) }
            },
            label = { Text(label) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            range.forEach { n ->
                DropdownMenuItem(
                    text = { Text(n.toString().padStart(2, '0')) },
                    onClick = {
                        onValue(n)
                        text = n.toString().padStart(2, '0')
                        expanded = false
                    },
                )
            }
        }
    }
}
