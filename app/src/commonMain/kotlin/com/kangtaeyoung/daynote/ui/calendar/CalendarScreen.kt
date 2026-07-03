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
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.kangtaeyoung.daynote.core.firstOfMonthPlusMonths
import com.kangtaeyoung.daynote.core.isoWeekNumber
import com.kangtaeyoung.daynote.core.monthGridDays
import com.kangtaeyoung.daynote.core.startOfDayMillis
import com.kangtaeyoung.daynote.core.toHourMinuteLabel
import com.kangtaeyoung.daynote.core.toLocalDate
import com.kangtaeyoung.daynote.core.today
import com.kangtaeyoung.daynote.core.toMillisRange
import com.kangtaeyoung.daynote.core.weekDays
import com.kangtaeyoung.daynote.data.repository.SettingsRepository
import com.kangtaeyoung.daynote.data.sync.CalendarSyncManager
import com.kangtaeyoung.daynote.data.sync.CloudSyncManager
import com.kangtaeyoung.daynote.data.sync.CloudSyncState
import com.kangtaeyoung.daynote.domain.holiday.KoreanHolidays
import com.kangtaeyoung.daynote.domain.model.ExternalEvent
import com.kangtaeyoung.daynote.domain.model.Note
import com.kangtaeyoung.daynote.domain.model.Task
import com.kangtaeyoung.daynote.domain.usecase.AddNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.AddTaskUseCase
import com.kangtaeyoung.daynote.domain.usecase.DeleteNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.DeleteTaskUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveExternalEventsByDateUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveNotesByDateUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveTasksByDateUseCase
import com.kangtaeyoung.daynote.domain.usecase.ToggleTaskUseCase
import com.kangtaeyoung.daynote.domain.usecase.UpdateNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.UpdateTaskUseCase
import com.kangtaeyoung.daynote.ui.components.DayNoteBottomBar
import com.kangtaeyoung.daynote.ui.components.SyncFab
import com.kangtaeyoung.daynote.ui.components.cloudSyncResultMessage
import com.kangtaeyoung.daynote.ui.components.MiniCalendarDialog
import com.kangtaeyoung.daynote.ui.components.NumberDropdown
import com.kangtaeyoung.daynote.ui.components.TaskRow
import com.kangtaeyoung.daynote.ui.components.parseHexColor
import com.kangtaeyoung.daynote.ui.components.spansDays
import com.kangtaeyoung.daynote.ui.components.TopDestination
import com.kangtaeyoung.daynote.ui.components.WithItemActions
import com.kangtaeyoung.daynote.ui.theme.AddPlusIcon
import com.kangtaeyoung.daynote.ui.theme.CloseXIcon
import com.kangtaeyoung.daynote.ui.theme.DragHandleIcon
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
    val observeExternalByDate = koinInject<ObserveExternalEventsByDateUseCase>()
    val vm = viewModel {
        CalendarViewModel(
            observeNotesByDate, observeTasksByDate, addTask, toggleTask, deleteTask, deleteNote,
            updateNote, addNote, updateTask, observeExternalByDate,
        )
    }

    val selectedDate by vm.selectedDate.collectAsState()
    val notesByDate by vm.notesByDate.collectAsState()
    val tasksByDate by vm.tasksByDate.collectAsState()
    val externalByDate by vm.externalByDate.collectAsState()
    val notesForSelected by vm.notesForSelected.collectAsState()
    val tasksForSelected by vm.tasksForSelected.collectAsState()
    val externalForSelected by vm.externalForSelected.collectAsState()

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
                // 결과 문구는 우측 하단 동기화 FAB 과 공용(components/SyncFab.kt).
                cloudSyncResultMessage(cloudSync.state.value)?.let { snackbarHostState.showSnackbar(it) }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Calendar") }) },
        // 설정(톱니)은 하단바로 이동, 동기화는 우측 하단 상시 FAB.
        bottomBar = {
            DayNoteBottomBar(
                current = TopDestination.Calendar,
                onSelect = onSelectDestination,
                onOpenSettings = onOpenSettings,
            )
        },
        floatingActionButton = { SyncFab(snackbarHostState) },
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
                    }.pointerInput(compact) {
                        // PC·태블릿: 마우스 휠/트랙패드 세로 스크롤 → 아래=다음, 위=이전(주/월 공통, 되돌리면 복귀).
                        awaitPointerEventScope {
                            var acc = 0f
                            while (true) {
                                val e = awaitPointerEvent()
                                if (e.type == PointerEventType.Scroll) {
                                    acc += e.changes.fold(0f) { a, c -> a + c.scrollDelta.y }
                                    if (acc >= 1f) {
                                        acc = 0f
                                        goNext()
                                    } else if (acc <= -1f) {
                                        acc = 0f
                                        goPrev()
                                    }
                                    e.changes.forEach { it.consume() }
                                }
                            }
                        }
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
                            WeekAgenda(days, selectedDate, notesByDate, tasksByDate, externalByDate, onSelect = vm::selectDate, onOpenNote = onOpenNote)
                        } else {
                            MonthGrid(a, days, selectedDate, notesByDate, tasksByDate, externalByDate, onSelect = vm::selectDate, onOpenNote = onOpenNote)
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
                    externalEvents = externalForSelected,
                    onOpenNote = onOpenNote,
                    onAddNote = { onAddNoteForDate(selectedDate.startOfDayMillis()) },
                    onDeleteNote = vm::removeNote,
                    onAddTask = { text, allDay, hour, minute, endDate, endHour, endMinute ->
                        // 추가 피드백: 실제 추가됐을 때만 확인 스낵바(빈 입력은 안내).
                        if (text.isBlank()) {
                            scope.launch { snackbarHostState.showSnackbar("할 일 내용을 입력하세요.") }
                        } else {
                            vm.addTaskForSelectedDate(text, allDay, hour, minute, endDate, endHour, endMinute)
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
                    onEditTask = { task ->
                        vm.editTask(task)
                        scope.launch { snackbarHostState.showSnackbar("할 일이 수정되었습니다 ✓") }
                    },
                    onConvertNoteToTask = { note ->
                        vm.convertNoteToTask(note)
                        scope.launch { snackbarHostState.showSnackbar("메모를 To-Do로 전환했습니다 ✓") }
                    },
                    onConvertTaskToNote = { task ->
                        vm.convertTaskToNote(task)
                        scope.launch { snackbarHostState.showSnackbar("To-Do를 메모로 전환했습니다 ✓") }
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
                // 앱 전체 한글 글씨체 통일 — 세리프 혼용을 없애고 기본체 하나만 쓴다(사용자 피드백).
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
    externalByDate: Map<LocalDate, List<ExternalEvent>>,
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
            // 기간 할 일 bar 는 주(週) 단위로 레인을 배정한다 — 한 할 일이 걸친 모든 날에서
            // 같은 레인(세로 위치)을 쓰게 해, 칸을 가로질러 하나의 막대로 이어져 보이게 한다.
            val bars = computeWeekBars(week, tasksByDate)
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
                        external = externalByDate[day].orEmpty(),
                        laneTasks = bars.laneTasksByDay[day].orEmpty(),
                        rangedOverflow = bars.overflowByDay[day].orEmpty(),
                        weekStart = week.first(),
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
    external: List<ExternalEvent>,
    laneTasks: List<Task?>,
    rangedOverflow: List<Task>,
    weekStart: LocalDate,
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
    // 칸(=weight 1f) 전체를 Box 로 잡는다. 둥근 배경 타일은 3dp 안쪽에 그리고(칸 사이 간격),
    // 기간 bar 는 타일이 아니라 '칸 전체 폭'에 그린다 — 칸끼리는 맞닿아 있어 bar 가 틈 없이 이어진다.
    Box(modifier = modifier.heightIn(min = 92.dp)) {
        Box(
            Modifier
                .matchParentSize()
                .padding(3.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(tileColor)
                .then(
                    if (isSelected) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(13.dp))
                    else Modifier,
                )
                .clickable(onClick = onClick),
        )
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            // 날짜 줄은 고정 높이(24dp) — 모든 칸에서 bar 레인의 세로 위치가 같아지도록.
            Row(
                modifier = Modifier.fillMaxWidth().height(24.dp).padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DayNumber(day.dayOfMonth, isToday, inMonth, isRed = isSunday || holidayName != null)
                DensityDots(notes, tasks)
            }
            Spacer(Modifier.height(4.dp))
            // 기간 bar 레인 — 칸 전체 폭(가로 여백 0)이라 옆 칸과 이어진다. 빈 레인도 같은 높이 확보(정렬 유지).
            laneTasks.forEach { t ->
                if (t != null) SeamlessDayBar(t, day, weekStart) else Spacer(Modifier.height(18.dp))
            }
            if (holidayName != null && inMonth) {
                Text(
                    text = holidayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp),
                )
            }
            // "+N개 더" 탭 → 숨겨진 항목을 칸 아래로 펼치는 드롭다운. 날짜가 바뀌면 접힌 상태로 초기화.
            var expanded by remember(day) { mutableStateOf(false) }
            // 구글 캘린더 외부 일정(읽기 전용) — 캘린더 색 점 + 제목.
            val visibleExternal = if (expanded) external else external.take(2)
            visibleExternal.forEach { event ->
                ExternalEventLine(event, modifier = Modifier.padding(horizontal = 10.dp))
            }
            val visibleNotes = if (expanded) notes else notes.take(2)
            visibleNotes.forEach { note ->
                Text(
                    text = note.title.ifBlank { "(제목 없음)" },
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (note.isPinned) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (note.isPinned) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp).clickable { onOpenNote(note.id) },
                )
            }
            // 하루를 넘기지 않는 할 일(종일·시각·같은 날 시각 범위)은 내용을 옅은 박스로.
            val single = tasks.filter { !it.spansDays() }
            val visibleSingle = if (expanded) single else single.take(2)
            visibleSingle.forEach { task -> TaskLineChip(task) }
            // 레인 부족으로 bar 를 못 그린 기간 할 일은 펼쳤을 때 칩(기간 병기)으로 보여준다.
            if (expanded) rangedOverflow.forEach { task -> TaskLineChip(task) }
            val more = (notes.size - 2).coerceAtLeast(0) +
                (single.size - 2).coerceAtLeast(0) +
                (external.size - 2).coerceAtLeast(0) +
                rangedOverflow.size
            if (more > 0) {
                Text(
                    text = if (expanded) "접기" else "+${more}개 더",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp).clickable { expanded = !expanded },
                )
            }
        }
    }
}

/** 한 주에 걸치는 기간 할 일에 레인(세로 줄)을 배정한 결과. */
private data class WeekBars(
    val laneTasksByDay: Map<LocalDate, List<Task?>>,
    // 레인 부족으로 bar 를 못 그린 기간 할 일 — "+N개 더" 개수이자 펼침(드롭다운) 시 보여줄 목록.
    val overflowByDay: Map<LocalDate, List<Task>>,
    val laneCount: Int,
)

/**
 * 주 단위 bar 레인 배정 — 겹치는 기간 할 일에 서로 다른 레인을 주되, 한 할 일은 걸친 모든 날에서
 * 같은 레인을 쓴다(칸을 가로질러 이어지도록). 레인은 [maxLanes] 개까지만; 초과분은 "+N개 더"로 센다.
 */
private fun computeWeekBars(
    week: List<LocalDate>,
    tasksByDate: Map<LocalDate, List<Task>>,
    maxLanes: Int = 2,
): WeekBars {
    fun startOf(t: Task) = (t.dueDate ?: t.createdAt).toLocalDate()
    fun endOf(t: Task) = t.endDate!!.toLocalDate()
    fun overlaps(a: Task, b: Task) = startOf(a) <= endOf(b) && startOf(b) <= endOf(a)

    // 이 주에 걸치는 기간 할 일(중복 제거) — 시작일·id 순으로 안정 정렬.
    // 같은 날 시각 범위(endDate 가 같은 날)는 bar 가 아니라 칩으로 다룬다(spansDays=false).
    val ranged = week.asSequence()
        .flatMap { (tasksByDate[it].orEmpty()).asSequence() }
        .filter { it.spansDays() }
        .distinctBy { it.id }
        .sortedWith(compareBy({ it.dueDate ?: it.createdAt }, { it.id }))
        .toList()

    val lanes = ArrayList<ArrayList<Task>>()
    val laneOf = HashMap<String, Int>()
    for (t in ranged) {
        var placed = -1
        for (i in lanes.indices) {
            if (lanes[i].none { overlaps(it, t) }) { lanes[i].add(t); placed = i; break }
        }
        if (placed == -1 && lanes.size < maxLanes) {
            lanes.add(arrayListOf(t)); placed = lanes.size - 1
        }
        laneOf[t.id] = placed // -1 = 레인 부족(초과) → "+N개 더"
    }
    val laneCount = lanes.size

    val laneTasksByDay = HashMap<LocalDate, List<Task?>>()
    val overflowByDay = HashMap<LocalDate, List<Task>>()
    for (day in week) {
        val onDay = (tasksByDate[day].orEmpty()).filter { it.spansDays() }
        val arr = arrayOfNulls<Task>(laneCount)
        val overflow = ArrayList<Task>()
        for (t in onDay) {
            val lane = laneOf[t.id] ?: -1
            if (lane in 0 until laneCount) arr[lane] = t else overflow.add(t)
        }
        laneTasksByDay[day] = arr.asList()
        overflowByDay[day] = overflow
    }
    return WeekBars(laneTasksByDay, overflowByDay, laneCount)
}

/**
 * 달력 칸 안의 할 일 한 줄 — 첨부 컨셉대로 옅은 회색 박스로 메모(맨글자)와 구분한다.
 * 첫 줄만 한 줄 말줄임으로 간략 표기, 완료된 할 일은 취소선. 기간 할 일은 "9/14~9/16" 병기.
 */
@Composable
private fun TaskLineChip(task: Task) {
    val head = task.text.lineSequence().firstOrNull()?.trim().orEmpty().ifBlank { "(내용 없음)" }
    val range = task.endDate?.let { end ->
        val due = task.dueDate
        val s = (due ?: task.createdAt).toLocalDate()
        val e = end.toLocalDate()
        // 같은 날 시각 범위(몇 시간짜리)는 "14:00~16:00", 여러 날 기간은 "9/14~9/16".
        if (e == s && due != null) "${due.toHourMinuteLabel()}~${end.toHourMinuteLabel()} "
        else "${s.monthNumber}/${s.dayOfMonth}~${e.monthNumber}/${e.dayOfMonth} "
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
            // 하루짜리 칩은 타일 안쪽으로 들여쓴다(가로 10dp = 타일 여백 3 + 안쪽 7). 기간 bar 만 칸 끝까지 꽉 채운다.
            .padding(start = 10.dp, end = 10.dp, top = 2.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 5.dp, vertical = 2.dp),
    )
}

/**
 * 기간 할 일 bar 한 칸 조각 — 중간 조각은 **칸 전체 폭**(가로 여백 0)에 그려, 맞닿은 이웃 칸의
 * 조각과 틈 없이 이어져 하나의 막대로 보인다. 단, 실제 시작일의 왼쪽·종료일의 오른쪽 끝은
 * 타일(칸 안쪽 3dp) 밖으로 삐져나가지 않도록 6dp 들여쓴다. 시작일만 왼쪽·종료일만 오른쪽
 * 모서리를 둥글리고 나머지(맞닿는 변)는 각지게 둔다. 라벨(제목)은 시작일 또는 그 주의 첫 칸([weekStart])에만 표기.
 */
@Composable
private fun SeamlessDayBar(task: Task, day: LocalDate, weekStart: LocalDate) {
    val start = (task.dueDate ?: task.createdAt).toLocalDate()
    val end = task.endDate?.toLocalDate() ?: start
    val roundLeft = day == start
    val roundRight = day == end
    val shape = RoundedCornerShape(
        topStart = if (roundLeft) 5.dp else 0.dp,
        bottomStart = if (roundLeft) 5.dp else 0.dp,
        topEnd = if (roundRight) 5.dp else 0.dp,
        bottomEnd = if (roundRight) 5.dp else 0.dp,
    )
    val barColor = MaterialTheme.colorScheme.primary.copy(alpha = if (task.isDone) 0.45f else 1f)
    // 라벨은 이 주에서 처음 보이는 칸(시작일이거나 주의 첫 칸)에만 — 여러 주에 걸쳐도 매주 제목이 보인다.
    val showLabel = day == start || day == weekStart
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (roundLeft) 6.dp else 0.dp,
                end = if (roundRight) 6.dp else 0.dp,
                top = 2.dp,
            )
            .height(16.dp)
            .clip(shape)
            .background(barColor),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (showLabel) {
            Text(
                text = task.text.lineSequence().firstOrNull()?.trim().orEmpty().ifBlank { " " },
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 6.dp),
            )
        }
    }
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
    externalByDate: Map<LocalDate, List<ExternalEvent>>,
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
            val external = externalByDate[day].orEmpty()
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
                    if (notes.isEmpty() && tasks.isEmpty() && external.isEmpty()) {
                        if (holidayName == null) {
                            Text("—", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        // "+N개 더" 탭 → 숨겨진 항목을 아래로 펼치는 드롭다운(월 달력 칸과 동일 동작).
                        var expanded by remember(day) { mutableStateOf(false) }
                        // 구글 캘린더 외부 일정(읽기 전용) — 캘린더 색 점 + 제목.
                        val visibleExternal = if (expanded) external else external.take(2)
                        visibleExternal.forEach { event -> ExternalEventLine(event) }
                        if (!expanded && external.size > 2) {
                            Text(
                                "+일정 ${external.size - 2}개 더",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable { expanded = true },
                            )
                        }
                        val visibleNotes = if (expanded) notes else notes.take(3)
                        visibleNotes.forEach { n ->
                            Text(
                                text = n.title.ifBlank { "(제목 없음)" },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = if (n.isPinned) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.fillMaxWidth().clickable { onOpenNote(n.id) },
                            )
                        }
                        if (!expanded && notes.size > 3) {
                            Text(
                                "+${notes.size - 3}개 더",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { expanded = true },
                            )
                        }
                        // "할 일 N개" 대신 내용을 박스로 — 월 달력(펼침)과 같은 표기.
                        val visibleTasks = if (expanded) tasks else tasks.take(2)
                        visibleTasks.forEach { task -> TaskLineChip(task) }
                        if (!expanded && tasks.size > 2) {
                            Text(
                                "+할 일 ${tasks.size - 2}개 더",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable { expanded = true },
                            )
                        }
                        if (expanded && (notes.size > 3 || tasks.size > 2 || external.size > 2)) {
                            Text(
                                "접기",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable { expanded = false },
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
    externalEvents: List<ExternalEvent>,
    onOpenNote: (String) -> Unit,
    onAddNote: () -> Unit,
    onDeleteNote: (String) -> Unit,
    onAddTask: (String, Boolean, Int, Int, LocalDate?, Int?, Int?) -> Unit,
    onToggleTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onMoveNote: (Note, LocalDate) -> Unit,
    onCopyNote: (Note, LocalDate) -> Unit,
    onMoveTask: (Task, LocalDate) -> Unit,
    onCopyTask: (Task, LocalDate) -> Unit,
    onEditTask: (Task) -> Unit,
    onConvertNoteToTask: (Note) -> Unit,
    onConvertTaskToNote: (Task) -> Unit,
) {
    // 드래그 전환 — 행의 핸들(⋮⋮)을 끌어 반대 섹션(MEMO↔TO-DO)에 놓으면 전환된다.
    // 좌표는 전부 window 기준으로 통일(섹션 경계 판정·고스트 위치 계산 공용).
    var dragPayload by remember(date) { mutableStateOf<ConvertDrag?>(null) }
    var dragPointer by remember { mutableStateOf(Offset.Zero) }
    var rootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var memoBounds by remember { mutableStateOf(Rect.Zero) }
    var todoBounds by remember { mutableStateOf(Rect.Zero) }
    val hoverTodo = dragPayload?.note != null && todoBounds.contains(dragPointer)
    val hoverMemo = dragPayload?.task != null && memoBounds.contains(dragPointer)

    Box(modifier = Modifier.fillMaxWidth().onGloballyPositioned { rootCoords = it }) {
        DayDetailContent(
            date = date,
            notes = notes,
            tasks = tasks,
            externalEvents = externalEvents,
            onOpenNote = onOpenNote,
            onAddNote = onAddNote,
            onDeleteNote = onDeleteNote,
            onAddTask = onAddTask,
            onToggleTask = onToggleTask,
            onDeleteTask = onDeleteTask,
            onMoveNote = onMoveNote,
            onCopyNote = onCopyNote,
            onMoveTask = onMoveTask,
            onCopyTask = onCopyTask,
            onEditTask = onEditTask,
            hoverMemo = hoverMemo,
            hoverTodo = hoverTodo,
            onMemoBounds = { memoBounds = it },
            onTodoBounds = { todoBounds = it },
            onDragStart = { payload, pos -> dragPayload = payload; dragPointer = pos },
            onDragBy = { delta -> dragPointer += delta },
            onDragEnd = {
                val p = dragPayload
                when {
                    p?.note != null && todoBounds.contains(dragPointer) -> onConvertNoteToTask(p.note)
                    p?.task != null && memoBounds.contains(dragPointer) -> onConvertTaskToNote(p.task)
                }
                dragPayload = null
            },
            onDragCancel = { dragPayload = null },
        )
        // 드래그 고스트 — 포인터를 따라다니는 작은 칩(어디로 전환되는지 표기).
        dragPayload?.let { payload ->
            val local = rootCoords?.windowToLocal(dragPointer) ?: Offset.Zero
            Surface(
                shape = RoundedCornerShape(10.dp),
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
                modifier = Modifier.offset { IntOffset(local.x.roundToInt() + 14, local.y.roundToInt() + 14) },
            ) {
                Text(
                    text = payload.label + if (payload.note != null) "  → TO-DO" else "  → MEMO",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp).widthIn(max = 220.dp),
                )
            }
        }
    }
}

/** 드래그 전환 중인 항목(메모 또는 할 일 중 하나) + 고스트에 표시할 라벨. */
private data class ConvertDrag(val label: String, val note: Note? = null, val task: Task? = null)

@Composable
private fun DayDetailContent(
    date: LocalDate,
    notes: List<Note>,
    tasks: List<Task>,
    externalEvents: List<ExternalEvent>,
    onOpenNote: (String) -> Unit,
    onAddNote: () -> Unit,
    onDeleteNote: (String) -> Unit,
    onAddTask: (String, Boolean, Int, Int, LocalDate?, Int?, Int?) -> Unit,
    onToggleTask: (String) -> Unit,
    onDeleteTask: (String) -> Unit,
    onMoveNote: (Note, LocalDate) -> Unit,
    onCopyNote: (Note, LocalDate) -> Unit,
    onMoveTask: (Task, LocalDate) -> Unit,
    onCopyTask: (Task, LocalDate) -> Unit,
    onEditTask: (Task) -> Unit,
    hoverMemo: Boolean,
    hoverTodo: Boolean,
    onMemoBounds: (Rect) -> Unit,
    onTodoBounds: (Rect) -> Unit,
    onDragStart: (ConvertDrag, Offset) -> Unit,
    onDragBy: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // 날짜 헤더 — 클레이 스와시(캘린더 헤더와 통일). 공휴일이면 이름을 클레이로 병기.
        // 글씨체는 앱 공통 기본체(세리프 혼용 제거 — 사용자 피드백).
        val holidayName = KoreanHolidays.nameOf(date)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = date.dayLabel(),
                style = MaterialTheme.typography.titleLarge,
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

        // 구글 캘린더(공유받은 캘린더 포함) 일정 — 읽기 전용, 캘린더 색 점으로 구분.
        if (externalEvents.isNotEmpty()) {
            SectionLabel("GOOGLE")
            externalEvents.forEach { event -> ExternalEventDetailRow(event) }
            Spacer(Modifier.height(8.dp))
        }

        // MEMO 섹션 — 드롭 대상 경계 추적 + To-Do 를 끌어올 때 하이라이트.
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { onMemoBounds(it.boundsInWindow()) }
                .background(
                    if (hoverMemo) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                    RoundedCornerShape(12.dp),
                ),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("MEMO")
                IconButton(onClick = onAddNote) {
                    Icon(AddPlusIcon, contentDescription = "메모 추가", tint = MaterialTheme.colorScheme.primary)
                }
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
                DragConvertRow(
                    key = note.id,
                    onStart = { pos ->
                        onDragStart(ConvertDrag(label = note.title.ifBlank { "(제목 없음)" }, note = note), pos)
                    },
                    onDragBy = onDragBy,
                    onEnd = onDragEnd,
                    onCancel = onDragCancel,
                ) {
                    NoteDetailRow(
                        note = note,
                        onOpen = { onOpenNote(note.id) },
                        onDelete = { onDeleteNote(note.id) },
                        onMoveTo = { d -> onMoveNote(note, d) },
                        onCopyTo = { d -> onCopyNote(note, d) },
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // TO-DO 섹션 — 드롭 대상 경계 추적 + 메모를 끌어올 때 하이라이트.
        Column(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { onTodoBounds(it.boundsInWindow()) }
                .background(
                    if (hoverTodo) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                    RoundedCornerShape(12.dp),
                ),
        ) {
            SectionLabel("TO-DO")
            tasks.forEach { task ->
                DragConvertRow(
                    key = task.id,
                    onStart = { pos ->
                        val head = task.text.lineSequence().firstOrNull()?.trim().orEmpty().ifBlank { "(내용 없음)" }
                        onDragStart(ConvertDrag(label = head, task = task), pos)
                    },
                    onDragBy = onDragBy,
                    onEnd = onDragEnd,
                    onCancel = onDragCancel,
                ) {
                    TaskRow(
                        task = task,
                        onToggle = { onToggleTask(task.id) },
                        onDelete = { onDeleteTask(task.id) },
                        onMoveTo = { d -> onMoveTask(task, d) },
                        onCopyTo = { d -> onCopyTask(task, d) },
                        onUpdate = onEditTask,
                    )
                }
            }
            TaskQuickAdd(selectedDate = date, onAdd = onAddTask)
        }
        Box(modifier = Modifier.heightIn(min = 24.dp))
    }
}

/**
 * 드래그 핸들(⋮⋮)이 붙은 행 — 핸들을 끌면 [onStart]/[onDragBy]/[onEnd] 로 전환 드래그를 보고한다.
 * 행 본문의 탭/길게 누르기(이동·복사 메뉴)와 충돌하지 않도록 핸들에서만 드래그가 시작된다.
 */
@Composable
private fun DragConvertRow(
    key: String,
    onStart: (Offset) -> Unit,
    onDragBy: (Offset) -> Unit,
    onEnd: () -> Unit,
    onCancel: () -> Unit,
    content: @Composable () -> Unit,
) {
    // pointerInput 은 key 로 고정되므로 최신 콜백을 rememberUpdatedState 로 참조한다(스테일 클로저 방지).
    val currentOnStart by rememberUpdatedState(onStart)
    val currentOnDragBy by rememberUpdatedState(onDragBy)
    val currentOnEnd by rememberUpdatedState(onEnd)
    val currentOnCancel by rememberUpdatedState(onCancel)
    var handleCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            DragHandleIcon,
            contentDescription = "끌어서 메모/To-Do 전환",
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier
                .size(20.dp)
                .onGloballyPositioned { handleCoords = it }
                .pointerInput(key) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentOnStart(handleCoords?.localToWindow(offset) ?: Offset.Zero)
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            currentOnDragBy(amount)
                        },
                        onDragEnd = { currentOnEnd() },
                        onDragCancel = { currentOnCancel() },
                    )
                },
        )
        Spacer(Modifier.width(6.dp))
        Box(modifier = Modifier.weight(1f)) { content() }
    }
}

/** 외부 일정의 "HH:mm" 또는 "HH:mm~HH:mm" 라벨. 종일이면 null. */
private fun ExternalEvent.timeLabel(): String? {
    if (allDay) return null
    val start = startMillis.toHourMinuteLabel()
    val end = endMillis?.toHourMinuteLabel()
    return if (end != null) "$start~$end" else start
}

/**
 * 달력 칸·주 아젠다의 구글 캘린더 일정 한 줄 — 캘린더 색 점 + 제목(읽기 전용).
 * 색은 구글 캘린더에서 지정한 캘린더 색을 그대로 따른다.
 */
@Composable
private fun ExternalEventLine(event: ExternalEvent, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth()) {
        Box(
            Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(parseHexColor(event.colorHex) ?: MaterialTheme.colorScheme.outline),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = event.title,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 상세의 구글 캘린더 일정 한 줄 — 색 점 + 제목 + (시각·캘린더명). 읽기 전용이라 조작 없음. */
@Composable
private fun ExternalEventDetailRow(event: ExternalEvent) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(parseHexColor(event.colorHex) ?: MaterialTheme.colorScheme.outline),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = listOfNotNull(event.timeLabel(), event.calendarName).joinToString(" · ")
            if (sub.isNotBlank()) {
                Text(
                    text = sub,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
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
private fun TaskQuickAdd(
    selectedDate: LocalDate,
    onAdd: (String, Boolean, Int, Int, LocalDate?, Int?, Int?) -> Unit,
) {
    var text by remember { mutableStateOf("") }
    var allDay by remember { mutableStateOf(true) }
    var hour by remember { mutableStateOf(9) }
    var minute by remember { mutableStateOf(0) }
    // 종료 시각(선택) — 켜면 시작~종료 시각 범위가 된다(종료일 없으면 같은 날).
    var endTimeOn by remember { mutableStateOf(false) }
    var endHour by remember { mutableStateOf(10) }
    var endMinute by remember { mutableStateOf(0) }
    // 기간 할 일: 종료일(시작=선택 날짜). null 이면 하루짜리.
    var endDate by remember(selectedDate) { mutableStateOf<LocalDate?>(null) }
    var showEndPicker by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("이 날 To-Do 추가") },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = {
                onAdd(text, allDay, hour, minute, endDate, endHour.takeIf { !allDay && endTimeOn }, endMinute.takeIf { !allDay && endTimeOn })
                text = ""
                endDate = null
                endTimeOn = false
            }) { Icon(AddPlusIcon, contentDescription = "추가", tint = MaterialTheme.colorScheme.primary) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("종일", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = allDay, onCheckedChange = { allDay = it })
            if (!allDay) {
                NumberDropdown(label = "시", value = hour, range = 0..23, onValue = { hour = it }, modifier = Modifier.width(64.dp))
                Text(":")
                NumberDropdown(label = "분", value = minute, range = 0..59, onValue = { minute = it }, modifier = Modifier.width(64.dp))
            }
        }
        // 종료 시각(선택 사항) — 시작 시각만으로 끝나는 일이 아니면 함께 지정.
        if (!allDay) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("종료 시각", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = endTimeOn, onCheckedChange = { endTimeOn = it })
                if (endTimeOn) {
                    NumberDropdown(label = "시", value = endHour, range = 0..23, onValue = { endHour = it }, modifier = Modifier.width(64.dp))
                    Text(":")
                    NumberDropdown(label = "분", value = endMinute, range = 0..59, onValue = { endMinute = it }, modifier = Modifier.width(64.dp))
                }
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
                IconButton(onClick = { endDate = null }) {
                    Icon(
                        CloseXIcon,
                        contentDescription = "기간 지우기",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
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
