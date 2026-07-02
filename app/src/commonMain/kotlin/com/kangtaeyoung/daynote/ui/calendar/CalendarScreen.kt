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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kangtaeyoung.daynote.core.firstOfMonthPlusMonths
import com.kangtaeyoung.daynote.core.isoWeekNumber
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
import com.kangtaeyoung.daynote.domain.holiday.KoreanHolidays
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
import com.kangtaeyoung.daynote.ui.components.MiniCalendarDialog
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
import kotlinx.datetime.minus
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
                    // 주 단위 헤더엔 ISO 연중 주차(W27)를 병기한다.
                    label = if (compact) {
                        val first = visibleDays.first()
                        "${first.monthNumber}월 ${first.dayOfMonth}일 주 (W${first.isoWeekNumber()})"
                    } else {
                        anchor.monthLabel()
                    },
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

            // 상세 영역 좌우 스와이프 → 전날/다음날로 이동(왼쪽으로 밀기=다음날).
            val detailSwipe = Modifier.pointerInput(Unit) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        val threshold = 56.dp.toPx()
                        when {
                            total <= -threshold -> vm.selectDate(vm.selectedDate.value.plus(1, DateTimeUnit.DAY))
                            total >= threshold -> vm.selectDate(vm.selectedDate.value.minus(1, DateTimeUnit.DAY))
                        }
                    },
                    onHorizontalDrag = { _, dragAmount -> total += dragAmount },
                )
            }

            val detailArea: @Composable () -> Unit = {
                DayDetail(
                    date = selectedDate,
                    notes = notesForSelected,
                    tasks = tasksForSelected,
                    onOpenNote = onOpenNote,
                    onAddNote = { onAddNoteForDate(selectedDate.startOfDayMillis()) },
                    onDeleteNote = vm::removeNote,
                    onAddTask = { text, allDay, hour, minute, endDate ->
                        // 추가 피드백: 실제 추가됐을 때만 확인 스낵바(빈 입력은 안내).
                        if (text.isBlank()) {
                            scope.launch { snackbarHostState.showSnackbar("할 일 내용을 입력하세요.") }
                        } else {
                            vm.addTaskForSelectedDate(text, allDay, hour, minute, endDate)
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
                                .padding(top = 12.dp)
                                .then(detailSwipe),
                        ) { detailArea() }
                    }
                } else {
                    // 1단(스택) — 폰=주 아젠다, 태블릿 세로/폴드 접힘=월 달력, 아래 상세.
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        calendarArea()
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Box(modifier = Modifier.fillMaxWidth().then(detailSwipe)) {
                            detailArea()
                        }
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
    val holidayName = KoreanHolidays.nameOf(day)
    val tileColor = when {
        !inMonth -> Color.Transparent
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        // 공휴일도 주말과 같은 음영 타일(관공서 공휴일 = 쉬는 날).
        isWeekend || holidayName != null -> MaterialTheme.colorScheme.secondaryContainer
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
            DayNumber(day.dayOfMonth, isToday, inMonth, isRed = isSunday || holidayName != null)
            DensityDots(notes, tasks)
        }
        Spacer(Modifier.height(4.dp))
        if (holidayName != null && inMonth) {
            Text(
                text = holidayName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.tertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // 기간 할 일(여러 날)은 이어지는 bar 조각으로 — 시작/끝 칸만 모서리를 둥글려 걸친 모양을 만든다.
        val (ranged, single) = tasks.partition { it.endDate != null }
        ranged.take(2).forEach { task -> TaskBarSegment(task, day) }
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
        // 하루짜리 할 일은 내용을 옅은 박스로(접힘/펼침 표기 통일).
        single.take(2).forEach { task -> TaskLineChip(task) }
        val more = (notes.size - 2).coerceAtLeast(0) +
            (single.size - 2).coerceAtLeast(0) +
            (ranged.size - 2).coerceAtLeast(0)
        if (more > 0) {
            Text(
                text = "+${more}개 더",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * 달력 칸 안의 할 일 한 줄 — 첨부 컨셉대로 옅은 회색 박스로 메모(맨글자)와 구분한다.
 * 첫 줄만 한 줄 말줄임으로 간략 표기, 완료된 할 일은 취소선. 기간 할 일은 "9/14~9/16" 병기.
 */
@Composable
private fun TaskLineChip(task: Task) {
    val head = task.text.lineSequence().firstOrNull()?.trim().orEmpty().ifBlank { "(내용 없음)" }
    val range = task.endDate?.let { end ->
        val s = (task.dueDate ?: task.createdAt).toLocalDate()
        val e = end.toLocalDate()
        "${s.monthNumber}/${s.dayOfMonth}~${e.monthNumber}/${e.dayOfMonth} "
    }.orEmpty()
    Text(
        text = range + head,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 5.dp, vertical = 2.dp),
    )
}

/**
 * 월 달력 칸의 기간 할 일 bar 조각 — 걸치는 모든 날짜 칸에 같은 슬레이트 bar 를 깔고,
 * 시작 칸만 왼쪽·끝 칸만 오른쪽 모서리를 둥글려 하나의 bar 가 이어진 것처럼 보이게 한다.
 * 텍스트는 시작 칸에만 표기(중간 칸은 bar 만).
 */
@Composable
private fun TaskBarSegment(task: Task, day: LocalDate) {
    val start = (task.dueDate ?: task.createdAt).toLocalDate()
    val end = task.endDate?.toLocalDate() ?: start
    val isStart = day == start
    val isEnd = day == end
    val shape = RoundedCornerShape(
        topStart = if (isStart) 5.dp else 0.dp,
        bottomStart = if (isStart) 5.dp else 0.dp,
        topEnd = if (isEnd) 5.dp else 0.dp,
        bottomEnd = if (isEnd) 5.dp else 0.dp,
    )
    val barColor = MaterialTheme.colorScheme.primary.copy(alpha = if (task.isDone) 0.45f else 1f)
    Text(
        text = if (isStart) task.text.lineSequence().firstOrNull()?.trim().orEmpty().ifBlank { " " } else " ",
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
        color = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp)
            .clip(shape)
            .background(barColor)
            .padding(horizontal = 5.dp, vertical = 2.dp),
    )
}

/** 오늘은 슬레이트 원으로 감싼 숫자, 그 외는 색만(일요일·공휴일=클레이, 이번달 밖=흐리게). */
@Composable
private fun DayNumber(dayOfMonth: Int, isToday: Boolean, inMonth: Boolean, isRed: Boolean) {
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
                isRed -> MaterialTheme.colorScheme.tertiary
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
            val holidayName = KoreanHolidays.nameOf(day)
            val tileColor = when {
                day == selected -> MaterialTheme.colorScheme.primaryContainer
                // 공휴일도 주말과 같은 음영 타일.
                isWeekend || holidayName != null -> MaterialTheme.colorScheme.secondaryContainer
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
                        color = if (isSunday || holidayName != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(3.dp))
                    DayNumber(day.dayOfMonth, isToday, inMonth = true, isRed = isSunday || holidayName != null)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    if (holidayName != null) {
                        Text(
                            text = holidayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (notes.isEmpty() && tasks.isEmpty()) {
                        if (holidayName == null) {
                            Text("—", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
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
                        // "할 일 N개" 대신 내용을 박스로 — 월 달력(펼침)과 같은 표기.
                        tasks.take(2).forEach { task -> TaskLineChip(task) }
                        if (tasks.size > 2) {
                            Text(
                                "+할 일 ${tasks.size - 2}개 더",
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
    onAddTask: (String, Boolean, Int, Int, LocalDate?) -> Unit,
    onToggleTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onMoveNote: (Note, LocalDate) -> Unit,
    onCopyNote: (Note, LocalDate) -> Unit,
    onMoveTask: (Task, LocalDate) -> Unit,
    onCopyTask: (Task, LocalDate) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // 날짜 헤더 — 세리프 + 클레이 스와시(캘린더 헤더와 통일). 공휴일이면 이름을 클레이로 병기.
        val holidayName = KoreanHolidays.nameOf(date)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = date.dayLabel(),
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Medium,
                color = if (holidayName != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface,
            )
            if (holidayName != null) {
                Text(
                    text = holidayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
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
        TaskQuickAdd(selectedDate = date, onAdd = onAddTask)
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
private fun TaskQuickAdd(selectedDate: LocalDate, onAdd: (String, Boolean, Int, Int, LocalDate?) -> Unit) {
    var text by remember { mutableStateOf("") }
    var allDay by remember { mutableStateOf(true) }
    var hour by remember { mutableStateOf(9) }
    var minute by remember { mutableStateOf(0) }
    // 기간 할 일: 종료일(시작=선택 날짜). null 이면 하루짜리.
    var endDate by remember(selectedDate) { mutableStateOf<LocalDate?>(null) }
    var showEndPicker by remember { mutableStateOf(false) }

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
                onAdd(text, allDay, hour, minute, endDate)
                text = ""
                endDate = null
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
        // 여러 날에 걸친 일이면 종료일을 지정한다(캘린더에 bar 로 걸쳐 표시).
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            TextButton(onClick = { showEndPicker = true }) {
                Text(
                    endDate?.let { "기간: ~ ${it.monthNumber}월 ${it.dayOfMonth}일" } ?: "기간: 하루 (종료일 지정)",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (endDate != null) {
                TextButton(onClick = { endDate = null }) { Text("지우기", style = MaterialTheme.typography.bodySmall) }
            }
        }
    }

    if (showEndPicker) {
        MiniCalendarDialog(
            initial = endDate ?: selectedDate,
            title = "종료일 선택 (시작: ${selectedDate.monthNumber}월 ${selectedDate.dayOfMonth}일)",
            pickMode = true,
            onPick = { picked ->
                endDate = picked.takeIf { it > selectedDate }
                showEndPicker = false
            },
            onDismiss = { showEndPicker = false },
        )
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
