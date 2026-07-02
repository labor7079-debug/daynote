package com.kangtaeyoung.daynote.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kangtaeyoung.daynote.core.atTimeMillis
import com.kangtaeyoung.daynote.core.dayRange
import com.kangtaeyoung.daynote.core.movedToDate
import com.kangtaeyoung.daynote.core.startOfDayMillis
import com.kangtaeyoung.daynote.core.toLocalDate
import com.kangtaeyoung.daynote.core.today
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * 캘린더(홈) 화면 상태/동작. "날짜 위에 메모가 얹힌다"가 중심 은유다.
 *
 * - [selectedDate] : 사용자가 고른 날짜(월/주 뷰가 공유).
 * - [visibleRange] : 화면에 보이는 달력 범위(월 그리드 또는 주). UI 가 활성 뷰에 맞춰 설정하고,
 *   그 범위의 메모를 날짜별로 묶어 칸 미리보기에 쓴다(VM 은 창 너비를 모름 — 분기는 UI 책임).
 * - 선택 날짜의 메모/할 일은 별도 Flow 로 상세 영역에 공급한다.
 */
class CalendarViewModel(
    private val observeNotesByDate: ObserveNotesByDateUseCase,
    private val observeTasksByDate: ObserveTasksByDateUseCase,
    private val addTask: AddTaskUseCase,
    private val toggleTask: ToggleTaskUseCase,
    private val deleteTask: DeleteTaskUseCase,
    private val deleteNote: DeleteNoteUseCase,
    private val updateNote: UpdateNoteUseCase,
    private val addNote: AddNoteUseCase,
    private val updateTask: UpdateTaskUseCase,
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(today())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    private val visibleRange = MutableStateFlow(today().dayRange())

    /** 보이는 범위의 메모를 날짜별로 묶은 맵 — 달력 칸 미리보기용. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val notesByDate: StateFlow<Map<LocalDate, List<Note>>> = visibleRange
        .flatMapLatest { (start, end) -> observeNotesByDate(start, end) }
        .map { notes -> notes.groupBy { it.date?.toLocalDate() ?: it.createdAt.toLocalDate() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /**
     * 보이는 범위의 할 일을 날짜별로 묶은 맵 — 달력 칸 표시용.
     * 기간 할 일(endDate 있음)은 걸치는 **모든 날짜**에 넣어 bar 로 이어 보이게 한다.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val tasksByDate: StateFlow<Map<LocalDate, List<Task>>> = visibleRange
        .flatMapLatest { (start, end) -> observeTasksByDate(start, end) }
        .map { tasks ->
            val byDate = mutableMapOf<LocalDate, MutableList<Task>>()
            tasks.forEach { task ->
                val start = (task.dueDate ?: task.createdAt).toLocalDate()
                val end = task.endDate?.toLocalDate()?.takeIf { it > start } ?: start
                var day = start
                var guard = 0
                while (day <= end && guard < MAX_SPAN_DAYS) {
                    byDate.getOrPut(day) { mutableListOf() }.add(task)
                    day = day.plus(1, DateTimeUnit.DAY)
                    guard++
                }
            }
            byDate
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    val notesForSelected: StateFlow<List<Note>> = _selectedDate
        .flatMapLatest { date -> val (s, e) = date.dayRange(); observeNotesByDate(s, e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasksForSelected: StateFlow<List<Task>> = _selectedDate
        .flatMapLatest { date -> val (s, e) = date.dayRange(); observeTasksByDate(s, e) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    /** UI 가 활성 뷰(월 그리드/주)의 표시 범위를 알려준다. */
    fun setVisibleRange(startOfDay: Long, endExclusive: Long) {
        visibleRange.value = startOfDay to endExclusive
    }

    /**
     * 선택 날짜에 할 일 추가. [allDay]=true 면 그 날짜 종일, false 면 [hour]:[minute] 시각으로 dueDate 주입.
     * [endDate] 를 주면 여러 날에 걸친 기간 할 일(선택 날짜 이후여야 유효).
     */
    fun addTaskForSelectedDate(text: String, allDay: Boolean, hour: Int, minute: Int, endDate: LocalDate? = null) {
        if (text.isBlank()) return
        val date = _selectedDate.value
        val due = if (allDay) date.startOfDayMillis() else date.atTimeMillis(hour, minute)
        val end = endDate?.takeIf { it > date }?.startOfDayMillis()
        viewModelScope.launch { addTask(text, noteId = null, dueDate = due, allDay = allDay, endDate = end) }
    }

    fun toggle(taskId: String) {
        viewModelScope.launch { toggleTask(taskId) }
    }

    fun removeTask(taskId: String) {
        viewModelScope.launch { deleteTask(taskId) }
    }

    fun removeNote(noteId: String) {
        viewModelScope.launch { deleteNote(noteId) }
    }

    // --- 길게 누름 메뉴(상세 영역) — 다른 날짜로 이동/복사 ---

    fun moveNoteTo(note: Note, date: LocalDate) {
        viewModelScope.launch { updateNote(note.copy(date = date.startOfDayMillis())) }
    }

    fun copyNoteTo(note: Note, date: LocalDate) {
        viewModelScope.launch { addNote(note.title, note.content, date.startOfDayMillis()) }
    }

    /** 시간 지정 할 일은 시:분 유지, 종일은 그 날 자정. */
    private fun Task.dueDateOn(date: LocalDate): Long =
        if (!allDay && dueDate != null) dueDate.movedToDate(date) else date.startOfDayMillis()

    /** 기간 할 일 이동/복사 시 종료일도 같은 간격만큼 함께 민다. */
    private fun Task.endDateOn(date: LocalDate): Long? {
        val end = endDate ?: return null
        val oldStart = dueDate ?: return null
        return end + (date.startOfDayMillis() - oldStart.toLocalDate().startOfDayMillis())
    }

    fun moveTaskTo(task: Task, date: LocalDate) {
        viewModelScope.launch {
            updateTask(task.copy(dueDate = task.dueDateOn(date), endDate = task.endDateOn(date)))
        }
    }

    fun copyTaskTo(task: Task, date: LocalDate) {
        viewModelScope.launch {
            addTask(task.text, task.noteId, task.dueDateOn(date), task.allDay, endDate = task.endDateOn(date))
        }
    }

    private companion object {
        /** 기간 할 일이 달력 맵을 폭주시키지 않게 한 건당 확장 일수 상한. */
        const val MAX_SPAN_DAYS = 62
    }
}
