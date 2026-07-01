package com.kangtaeyoung.daynote.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kangtaeyoung.daynote.core.atTimeMillis
import com.kangtaeyoung.daynote.core.dayRange
import com.kangtaeyoung.daynote.core.startOfDayMillis
import com.kangtaeyoung.daynote.core.toLocalDate
import com.kangtaeyoung.daynote.core.today
import com.kangtaeyoung.daynote.domain.model.Note
import com.kangtaeyoung.daynote.domain.model.Task
import com.kangtaeyoung.daynote.domain.usecase.AddTaskUseCase
import com.kangtaeyoung.daynote.domain.usecase.DeleteNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.DeleteTaskUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveNotesByDateUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveTasksByDateUseCase
import com.kangtaeyoung.daynote.domain.usecase.ToggleTaskUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

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

    /** 보이는 범위의 할 일을 날짜별로 묶은 맵 — 달력 칸 밀도 점(할일) 표시용. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val tasksByDate: StateFlow<Map<LocalDate, List<Task>>> = visibleRange
        .flatMapLatest { (start, end) -> observeTasksByDate(start, end) }
        .map { tasks -> tasks.groupBy { (it.dueDate ?: it.createdAt).toLocalDate() } }
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
     */
    fun addTaskForSelectedDate(text: String, allDay: Boolean, hour: Int, minute: Int) {
        if (text.isBlank()) return
        val date = _selectedDate.value
        val due = if (allDay) date.startOfDayMillis() else date.atTimeMillis(hour, minute)
        viewModelScope.launch { addTask(text, noteId = null, dueDate = due, allDay = allDay) }
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
}
