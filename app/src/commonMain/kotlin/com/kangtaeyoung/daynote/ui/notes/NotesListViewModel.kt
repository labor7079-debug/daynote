package com.kangtaeyoung.daynote.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kangtaeyoung.daynote.core.startOfDayMillis
import com.kangtaeyoung.daynote.core.today
import com.kangtaeyoung.daynote.domain.model.Note
import com.kangtaeyoung.daynote.domain.usecase.DeleteNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveNotesUseCase
import com.kangtaeyoung.daynote.domain.usecase.SetNotePinnedUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus

/** 메모 목록 기간 필터 — "언제 쓴 메모인지"로 좁혀 본다. */
enum class NotesPeriod(val label: String) {
    ALL("전체"),
    TODAY("오늘"),
    WEEK("최근 7일"),
    MONTH("최근 30일"),
}

/** 메모 목록 화면의 상태/동작. 진실의 원천(Room) Flow 를 기간 필터와 결합해 화면 상태로 흘려보낸다. */
class NotesListViewModel(
    observeNotes: ObserveNotesUseCase,
    private val deleteNote: DeleteNoteUseCase,
    private val setPinned: SetNotePinnedUseCase,
) : ViewModel() {

    private val _period = MutableStateFlow(NotesPeriod.ALL)
    val period: StateFlow<NotesPeriod> = _period

    fun setPeriod(period: NotesPeriod) {
        _period.value = period
    }

    /** 기간 필터 적용된 메모. 기준 날짜는 캘린더 날짜([Note.date]), 없으면 작성일. */
    val notes: StateFlow<List<Note>> = combine(observeNotes(), _period) { list, period ->
        val cutoff = when (period) {
            NotesPeriod.ALL -> return@combine list
            NotesPeriod.TODAY -> today().startOfDayMillis()
            NotesPeriod.WEEK -> today().minus(6, DateTimeUnit.DAY).startOfDayMillis()
            NotesPeriod.MONTH -> today().minus(29, DateTimeUnit.DAY).startOfDayMillis()
        }
        list.filter { (it.date ?: it.createdAt) >= cutoff }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: String) {
        viewModelScope.launch { deleteNote(id) }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch { setPinned(note.id, !note.isPinned) }
    }
}
