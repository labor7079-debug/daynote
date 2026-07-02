package com.kangtaeyoung.daynote.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kangtaeyoung.daynote.domain.model.Note
import com.kangtaeyoung.daynote.domain.usecase.DeleteNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveNotesUseCase
import com.kangtaeyoung.daynote.domain.usecase.SetNotePinnedUseCase
import com.kangtaeyoung.daynote.ui.components.Period
import com.kangtaeyoung.daynote.ui.components.cutoffMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 메모 목록 화면의 상태/동작. 진실의 원천(Room) Flow 를 기간 필터와 결합해 화면 상태로 흘려보낸다. */
class NotesListViewModel(
    observeNotes: ObserveNotesUseCase,
    private val deleteNote: DeleteNoteUseCase,
    private val setPinned: SetNotePinnedUseCase,
) : ViewModel() {

    private val _period = MutableStateFlow(Period.ALL)
    val period: StateFlow<Period> = _period

    fun setPeriod(period: Period) {
        _period.value = period
    }

    /** 기간 필터 적용된 메모. 기준 날짜는 캘린더 날짜([Note.date]), 없으면 작성일. */
    val notes: StateFlow<List<Note>> = combine(observeNotes(), _period) { list, period ->
        val cutoff = period.cutoffMillis() ?: return@combine list
        list.filter { (it.date ?: it.createdAt) >= cutoff }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: String) {
        viewModelScope.launch { deleteNote(id) }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch { setPinned(note.id, !note.isPinned) }
    }
}
