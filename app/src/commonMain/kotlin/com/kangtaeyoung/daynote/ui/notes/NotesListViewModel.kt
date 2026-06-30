package com.kangtaeyoung.daynote.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kangtaeyoung.daynote.domain.model.Note
import com.kangtaeyoung.daynote.domain.usecase.DeleteNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveNotesUseCase
import com.kangtaeyoung.daynote.domain.usecase.SetNotePinnedUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 메모 목록 화면의 상태/동작. 진실의 원천(Room) Flow 를 그대로 화면 상태로 흘려보낸다. */
class NotesListViewModel(
    observeNotes: ObserveNotesUseCase,
    private val deleteNote: DeleteNoteUseCase,
    private val setPinned: SetNotePinnedUseCase,
) : ViewModel() {

    val notes: StateFlow<List<Note>> = observeNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: String) {
        viewModelScope.launch { deleteNote(id) }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch { setPinned(note.id, !note.isPinned) }
    }
}
