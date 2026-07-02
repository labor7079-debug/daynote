package com.kangtaeyoung.daynote.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kangtaeyoung.daynote.core.firstOfMonthPlusMonths
import com.kangtaeyoung.daynote.core.monthGridDays
import com.kangtaeyoung.daynote.core.startOfDayMillis
import com.kangtaeyoung.daynote.core.toLocalDate
import com.kangtaeyoung.daynote.core.today
import com.kangtaeyoung.daynote.core.toMillisRange
import com.kangtaeyoung.daynote.core.weekDays
import com.kangtaeyoung.daynote.data.repository.SettingsRepository
import com.kangtaeyoung.daynote.data.sync.CalendarSyncManager
import com.kangtaeyoung.daynote.data.sync.CloudSyncManager
import com.kangtaeyoung.daynote.data.sync.CloudSyncState
import com.kangtaeyoung.daynote.domain.model.Note
import com.kangtaeyoung.daynote.domain.model.Task
import com.kangtaeyoung.daynote.domain.usecase.AddNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.AddTaskUseCase
import com.kangtaeyoung.daynote.domain.usecase.DeleteNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.DeleteTaskUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveNotesByDateUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveTasksByDateUseCase
import com.kangtaeyoung.daynote.domain.usecase.ToggleTaskUseCase
import com.kangtaeyoung.daynote.domain.usecase.UpdateNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.UpdateTaskUseCase
import com.kangtaeyoung.daynote.ui.components.DayNoteBottomBar
import com.kangtaeyoung.daynote.ui.components.TaskRow
import com.kangtaeyoung.daynote.ui.components.TopDestination
import com.kangtaeyoung.daynote.ui.components.WithItemActions
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import com.kangtaeyoung.daynote.ui.theme.SettingsGearIcon
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    val updateNote = koinInject<UpdateNoteUseCase>()
    val addNote = koinInject<AddNoteUseCase>()
    val updateTask = koinInject<UpdateTaskUseCase>()
    val vm = viewModel {
        CalendarViewModel(
            observeNotesByDate, observeTasksByDate, addTask, toggleTask, deleteTask, deleteNote,
            updateNote, addNote, updateTask,
        )
    }

    val selectedDate by vm.selectedDate.collectAsState()
    val notesByDate by vm.notesByDate.collectAsState()
    val tasksByDate by vm.tasksByDate.collectAsState()
    val notesForSelected by vm.notesForSelected.collectAsState()
    val tasksForSelected by vm.tasksForSelected.collectAsState()

    var anchor by remember { mutableStateOf(today()) }

    // 당겨서 새로고침 → 동기화(클라우드 + 토글 켜진 경우 구글 캘린더 push). 결과는 스낵바로 알린다.
    val cloudSync = koinInject<CloudSyncManager>()
    val calendarSync = koinInject<CalendarSyncManager>()
    val settings = koinInject<SettingsRepository>()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    val onRefresh: () -> Unit = {
        if (!refreshing) {
            refreshing = true
            scope.launch {
                cloudSync.syncNow()
                if (calendarSync.isAvailable && settings.observeSyncEnabled().first()) {
                    calendarSync.syncNow()
                }
                refreshing = false
                val message = when (val s = cloudSync.state.value) {
                    is CloudSyncState.Synced -> "동기화 완료 ✓"
                    is CloudSyncState.Error -> "동기화 실패: ${s.message.lineSequence().first()}"
                    CloudSyncState.Disabled -> "클라우드 동기화가 꺼져 있어요. 설정에서 켤 수 있어요."
                    CloudSyncState.NeedsConfig -> "동기화 설정(설정 화면)이 필요해요."
                    CloudSyncState.SignedOut -> "동기화 로그인이 필요해요(설정 화면)."
                    else -> null
                }
                message?.let { snackbarHostState.showSnackbar(it) }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Calendar") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = SettingsGearIcon,
                            contentDescription = "설정",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
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
                            WeekAgenda(days, selectedDate, notesByDate, tasksByDate, onSelect = vm::selectDate, onOpenNote = onOpenNote)
                        } else {
                            MonthGrid(a, days, selectedDate, notesByDate, tasksByDate, onSelect = vm::selectDate, onOpenNote = onOpenNote)
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
                    onAddTask = { text, allDay, hour, minute ->
                        // 추가 피드백: 실제 추가됐을 때만 확인 스낵바(빈 입력은 안내).
                        if (text.isBlank()) {
                            scope.launch { snackbarHostState.showSnackbar("할 일 내용을 입력하세요.") }
                        } else {
                            vm.addTaskForSelectedDate(text, allDay, hour, minute)
                            scope.launch { snackbarHostState.showSnackbar("할 일이 추가되었습니다 ✓") }
                        }
                    },
                    onToggleTask = vm::toggle,
                    onDeleteTask = vm::removeTask,
                    onMoveNote = { note, d ->
                        vm.moveNoteTo(note, d)
                        scope.launch { snackbarHostState.showSnackbar("메모를 ${d.monthNumber}월 ${d.dayOfMonth}일로 이동했습니다 ✓") }
                    },
                    onCopyNote = { note, d ->
                        vm.copyNoteTo(note, d)
                        scope.launch { snackbarHostState.showSnackbar("메모를 ${d.monthNumber}월 ${d.dayOfMonth}일에 복사했습니다 ✓") }
                    },
                    onMoveTask = { task, d ->
                        vm.moveTaskTo(task, d)
                        scope.launch { snackbarHostState.showSnackbar("할 일을 ${d.monthNumber}월 ${d.dayOfMonth}일로 이동했습니다 ✓") }
                    },
                    onCopyTask = { task, d ->
                        vm.copyTaskTo(task, d)
                        scope.launch { snackbarHostState.showSnackbar("할 일을 ${d.monthNumber}월 ${d.dayOfMonth}일에 복사했습니다 ✓") }
                    },
                )
            }

            // 위에서 아래로 당기면 동기화(업데이트). 스크롤 최상단에서 동작한다.
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
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
}

@Composable
private fun CalendarHeader(label: String, onPrev: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    // 「Warm Journal」 헤더 — 세리프 월 제목 + 클레이 스와시 밑줄(절제된 강조). 좌우 얇은 네비.
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.tertiary), // 클레이 스와시
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onPrev) { Text("‹", style = MaterialTheme.typography.titleLarge) }
            TextButton(onClick = onToday) { Text("오늘") }
            TextButton(onClick = onNext) { Text("›", style = MaterialTheme.typography.titleLarge) }
        }
    }
}

@Composable
private fun MonthGrid(
    anchor: LocalDate,
    days: List<LocalDate>,
    selected: LocalDate,
    notesByDate: Map<LocalDate, List<Note>>,
    tasksByDate: Map<LocalDate, List<Task>>,
    onSelect: (LocalDate) -> Unit,
    onOpenNote: (String) -> Unit,
) {
    // 요일 헤더 + 6주 행을 세로로 쌓는다. Column 이 없으면(부모가 Box/AnimatedContent) 행들이 겹쳐
    // "한 줄"처럼 보인다 — 반드시 Column 으로 감싼다.
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            weekdayHeaders.forEachIndexed { i, d ->
                Text(
                    text = d,
                    style = MaterialTheme.typography.labelMedium,
                    // 일요일(index 6)만 절제된 클레이 강조.
                    color = if (i == 6) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f).padding(4.dp),
                )
            }
        }
        val today = today()
        days.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEachIndexed { i, day ->
                    val inMonth = day.monthNumber == anchor.monthNumber && day.year == anchor.year
                    DayCell(
                        day = day,
                        inMonth = inMonth,
                        isSelected = day == selected,
                        isToday = day == today,
                        isSunday = i == 6,
                        isWeekend = i >= 5,
                        notes = notesByDate[day].orEmpty(),
                        tasks = tasksByDate[day].orEmpty(),
                        onClick = { onSelect(day) },
                        onOpenNote = onOpenNote,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * 「Warm Journal」 날짜 칸 — 부드러운 라운드 타일. 상단에 날짜 + 밀도 점(할일·중요·메모),
 * 그 아래 메모 제목 미리보기 최대 2줄 + "+N개 더". 점은 밀도를, 글자는 내용을 담당한다.
 */
@Composable
private fun DayCell(
    day: LocalDate,
    inMonth: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    isSunday: Boolean,
    isWeekend: Boolean,
    notes: List<Note>,
    tasks: List<Task>,
    onClick: () -> Unit,
    onOpenNote: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tileColor = when {
        !inMonth -> Color.Transparent
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isWeekend -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    Column(
        modifier = modifier
            .padding(3.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(tileColor)
            .then(
                if (isSelected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(13.dp))
                else Modifier,
            )
            .clickable(onClick = onClick)
            .heightIn(min = 92.dp)
            .padding(horizontal = 7.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DayNumber(day.dayOfMonth, isToday, inMonth, isSunday)
            DensityDots(notes, tasks)
        }
        Spacer(Modifier.height(4.dp))
        notes.take(2).forEach { note ->
            Text(
                text = note.title.ifBlank { "(제목 없음)" },
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (note.isPinned) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (note.isPinned) FontWeight.SemiBold else FontWeight.Normal,
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

/** 오늘은 슬레이트 원으로 감싼 숫자, 그 외는 색만(일요일=클레이, 이번달 밖=흐리게). */
@Composable
private fun DayNumber(dayOfMonth: Int, isToday: Boolean, inMonth: Boolean, isSunday: Boolean) {
    if (isToday) {
        Box(
            modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = dayOfMonth.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.SemiBold,
            )
        }
    } else {
        Text(
            text = dayOfMonth.toString(),
            style = MaterialTheme.typography.labelLarge,
            color = when {
                !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                isSunday -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

private enum class DotKind { Important, Task, Note }

/** 그날 밀도 점 — 중요(클레이)·할일(슬레이트)·메모(빈 원) 순으로 최대 4개. */
@Composable
private fun DensityDots(notes: List<Note>, tasks: List<Task>) {
    val dots = buildList {
        repeat(notes.count { it.isPinned }) { add(DotKind.Important) }
        repeat(tasks.size) { add(DotKind.Task) }
        repeat(notes.count { !it.isPinned }) { add(DotKind.Note) }
    }.take(4)
    if (dots.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        dots.forEach { Dot(it) }
    }
}

@Composable
private fun Dot(kind: DotKind) {
    val base = Modifier.size(6.dp)
    when (kind) {
        DotKind.Important -> Box(base.clip(CircleShape).background(MaterialTheme.colorScheme.tertiary))
        DotKind.Task -> Box(base.clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        DotKind.Note -> Box(base.clip(CircleShape).border(1.3.dp, MaterialTheme.colorScheme.outline, CircleShape))
    }
}

@Composable
private fun WeekAgenda(
    days: List<LocalDate>,
    selected: LocalDate,
    notesByDate: Map<LocalDate, List<Note>>,
    tasksByDate: Map<LocalDate, List<Task>>,
    onSelect: (LocalDate) -> Unit,
    onOpenNote: (String) -> Unit,
) {
    val today = today()
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        days.forEach { day ->
            val notes = notesByDate[day].orEmpty()
            val tasks = tasksByDate[day].orEmpty()
            val isToday = day == today
            val isSunday = day.dayOfWeek == DayOfWeek.SUNDAY
            val isWeekend = isSunday || day.dayOfWeek == DayOfWeek.SATURDAY
            val tileColor = when {
                day == selected -> MaterialTheme.colorScheme.primaryContainer
                isWeekend -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(tileColor)
                    .then(
                        if (day == selected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(13.dp))
                        else Modifier,
                    )
                    .clickable { onSelect(day) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(40.dp),
                ) {
                    Text(
                        koreanDow(day.dayOfWeek),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSunday) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(3.dp))
                    DayNumber(day.dayOfMonth, isToday, inMonth = true, isSunday = isSunday)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    if (notes.isEmpty() && tasks.isEmpty()) {
                        Text("—", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        notes.take(3).forEach { n ->
                            Text(
                                text = n.title.ifBlank { "(제목 없음)" },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (n.isPinned) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.fillMaxWidth().clickable { onOpenNote(n.id) },
                            )
                        }
                        if (notes.size > 3) {
                            Text("+${notes.size - 3}개 더", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        if (tasks.isNotEmpty()) {
                            Text(
                                "할 일 ${tasks.size}개",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                DensityDots(notes, tasks)
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
    onMoveNote: (Note, LocalDate) -> Unit,
    onCopyNote: (Note, LocalDate) -> Unit,
    onMoveTask: (Task, LocalDate) -> Unit,
    onCopyTask: (Task, LocalDate) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // 날짜 헤더 — 세리프 + 클레이 스와시(캘린더 헤더와 통일).
        Text(
            text = date.dayLabel(),
            style = MaterialTheme.typography.titleLarge,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Box(
            modifier = Modifier
                .padding(top = 1.dp)
                .width(40.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.tertiary),
        )
        Spacer(Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            SectionLabel("MEMO")
            TextButton(onClick = onAddNote) { Text("+ 추가") }
        }
        if (notes.isEmpty()) {
            // 빈 안내 영역을 탭해도 곧바로 새 메모 작성으로 진입한다(+ 추가 버튼과 동일).
            Text(
                "이 날의 메모가 없습니다. 탭하여 추가하세요.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onAddNote)
                    .padding(vertical = 14.dp, horizontal = 12.dp),
            )
        }
        notes.forEach { note ->
            NoteDetailRow(
                note = note,
                onOpen = { onOpenNote(note.id) },
                onDelete = { onDeleteNote(note.id) },
                onMoveTo = { d -> onMoveNote(note, d) },
                onCopyTo = { d -> onCopyNote(note, d) },
            )
        }

        Spacer(Modifier.height(8.dp))

        SectionLabel("TO-DO")
        tasks.forEach { task ->
            TaskRow(
                task = task,
                onToggle = { onToggleTask(task.id) },
                onDelete = { onDeleteTask(task.id) },
                onMoveTo = { d -> onMoveTask(task, d) },
                onCopyTo = { d -> onCopyTask(task, d) },
            )
        }
        TaskQuickAdd(onAdd = onAddTask)
        Box(modifier = Modifier.heightIn(min = 24.dp))
    }
}

/** 작은 클리니컬 섹션 라벨(자간 넓힘) — Quiet Cadence 캡션 톤. */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 2.sp,
    )
}

/**
 * 상세의 메모 한 줄 — 부드러운 웜 타일. 핀은 굵게. 삭제는 절제된 ✕.
 * 길게 누르면 이동/복사/삭제 메뉴([WithItemActions]).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteDetailRow(
    note: Note,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onMoveTo: (LocalDate) -> Unit,
    onCopyTo: (LocalDate) -> Unit,
) {
    WithItemActions(
        calendarDate = (note.date ?: note.createdAt).toLocalDate(),
        onMoveTo = onMoveTo,
        onCopyTo = onCopyTo,
        onDelete = onDelete,
    ) { _, openMenu ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .combinedClickable(onClick = onOpen, onLongClick = openMenu)
                .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    note.title.ifBlank { "(제목 없음)" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (note.isPinned) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (note.content.isNotBlank()) {
                    Text(
                        note.content.trim(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            DeleteX(onDelete)
        }
    }
}

/** 절제된 삭제 버튼 — "삭제" 텍스트 대신 작은 ✕(onSurfaceVariant). */
@Composable
private fun DeleteX(onDelete: () -> Unit) {
    Text(
        text = "✕",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.clip(CircleShape).clickable(onClick = onDelete).padding(8.dp),
    )
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
