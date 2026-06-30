package com.kangtaeyoung.daynote.ui.notes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.kangtaeyoung.daynote.domain.model.Note
import com.kangtaeyoung.daynote.domain.model.Task
import com.kangtaeyoung.daynote.domain.usecase.AddNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.AddTaskUseCase
import com.kangtaeyoung.daynote.domain.usecase.DeleteNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.DeleteTaskUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveNoteTasksUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.ToggleTaskUseCase
import com.kangtaeyoung.daynote.domain.usecase.UpdateNoteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 메모 에디터 화면의 상태/동작. [initialNoteId] 가 null 이면 새 메모.
 *
 * 제목/본문은 Compose 상태로 들고, 저장 시 Repository 규칙(타임스탬프 등)에 맡긴다.
 * 할 일(체크리스트)은 메모가 저장돼 id 가 생긴 뒤부터 붙는다 — id 변화를 [idFlow] 로 반응형 관찰.
 */
class NoteEditorViewModel(
    private val initialNoteId: String?,
    private val initialDate: Long? = null,
    private val observeNote: ObserveNoteUseCase,
    private val addNote: AddNoteUseCase,
    private val updateNote: UpdateNoteUseCase,
    private val deleteNote: DeleteNoteUseCase,
    private val observeNoteTasks: ObserveNoteTasksUseCase,
    private val addTask: AddTaskUseCase,
    private val toggleTask: ToggleTaskUseCase,
    private val deleteTask: DeleteTaskUseCase,
) : ViewModel() {

    var title by mutableStateOf("")
    var content by mutableStateOf("")
        private set

    private val idFlow = MutableStateFlow(initialNoteId)
    /** 이미 저장된 메모인지(=할 일 영역 노출 여부). */
    val savedId: StateFlow<String?> = idFlow

    private var loaded: Note? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasks: StateFlow<List<Task>> = idFlow
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else observeNoteTasks(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        val id = initialNoteId
        if (id != null) {
            viewModelScope.launch {
                observeNote(id).first()?.let { note ->
                    loaded = note
                    title = note.title
                    content = note.content
                }
            }
        }
    }

    fun onContentChange(value: String) {
        content = value
    }

    /**
     * 명시적 저장(저장·뒤로). 빈 메모 방지: 새 메모인데 제목·본문이 모두 비면 만들지 않는다.
     * (단, 할 일을 이미 붙였다면 [idFlow] 가 비어 있지 않아 정상 갱신된다.)
     */
    fun save() {
        if (idFlow.value == null && title.isBlank() && content.isBlank()) return
        viewModelScope.launch { persist() }
    }

    /** 새 메모면 생성, 기존이면 갱신. 항상 메모를 보장한다(할 일 부모 확보용으로도 쓰임). */
    private suspend fun persist() {
        val id = idFlow.value
        if (id == null) {
            val created = addNote(title, content, initialDate)
            loaded = created
            idFlow.value = created.id
        } else {
            val base = loaded ?: return
            val updated = base.copy(title = title.trim(), content = content)
            updateNote(updated)
            loaded = updated
        }
    }

    fun addNewTask(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            // 빈 메모라도 할 일을 붙이려면 부모 메모가 있어야 하므로 먼저 생성한다.
            if (idFlow.value == null) persist()
            val id = idFlow.value ?: return@launch
            addTask(text, id)
        }
    }

    fun toggle(taskId: String) {
        viewModelScope.launch { toggleTask(taskId) }
    }

    fun removeTask(taskId: String) {
        viewModelScope.launch { deleteTask(taskId) }
    }

    /** 메모 삭제(소프트). 새 메모(미저장)면 할 일 없음. */
    fun deleteCurrent(onDeleted: () -> Unit) {
        val id = idFlow.value
        if (id == null) {
            onDeleted()
            return
        }
        viewModelScope.launch {
            deleteNote(id)
            onDeleted()
        }
    }
}
