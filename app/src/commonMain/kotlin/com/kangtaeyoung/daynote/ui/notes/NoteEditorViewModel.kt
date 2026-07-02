package com.kangtaeyoung.daynote.ui.notes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import com.kangtaeyoung.daynote.core.startOfDayMillis
import com.kangtaeyoung.daynote.core.today
import com.kangtaeyoung.daynote.domain.model.Note
import com.kangtaeyoung.daynote.domain.model.Task
import com.kangtaeyoung.daynote.domain.usecase.AddNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.AddTaskUseCase
import com.kangtaeyoung.daynote.domain.usecase.DeleteNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.DeleteTaskUseCase
import com.kangtaeyoung.daynote.data.repository.SettingsRepository
import com.kangtaeyoung.daynote.domain.usecase.FindRelatedNotesUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveNoteTasksUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.SuggestTitleUseCase
import com.kangtaeyoung.daynote.domain.usecase.ToggleTaskUseCase
import com.kangtaeyoung.daynote.domain.usecase.UpdateNoteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
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
    private val suggestTitle: SuggestTitleUseCase,
    private val settings: SettingsRepository,
    private val findRelatedNotes: FindRelatedNotesUseCase,
) : ViewModel() {

    var title by mutableStateOf("")
        private set
    var content by mutableStateOf("")
        private set

    /** 제목+본문 스냅샷 — 유사 메모 추천의 입력(디바운스용 Flow 미러). */
    private val sourceFlow = MutableStateFlow("")

    /** AI 제목 생성 진행 중(버튼 스피너·중복 호출 방지). */
    var titleLoading by mutableStateOf(false)
        private set

    private val idFlow = MutableStateFlow(initialNoteId)
    /** 이미 저장된 메모인지(=할 일 영역 노출 여부). */
    val savedId: StateFlow<String?> = idFlow

    private var loaded: Note? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val tasks: StateFlow<List<Task>> = idFlow
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else observeNoteTasks(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * 실시간 유사 메모 추천 — 입력을 디바운스한 뒤 로컬 FTS(키워드 OR 매칭)로 다른 날의
     * 비슷한 메모를 찾는다. 오프라인 우선 원칙대로 네트워크 없이 즉시 동작한다.
     */
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val relatedNotes: StateFlow<List<Note>> =
        combine(sourceFlow.debounce(RELATED_DEBOUNCE_MS), idFlow) { source, id -> source to id }
            .flatMapLatest { (source, id) ->
                if (source.trim().length < 2) flowOf(emptyList())
                else findRelatedNotes(source, excludeNoteId = id, excludeDate = noteDate())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        val id = initialNoteId
        if (id != null) {
            viewModelScope.launch {
                observeNote(id).first()?.let { note ->
                    loaded = note
                    title = note.title
                    content = note.content
                    sourceFlow.value = "${note.title}\n${note.content}"
                }
            }
        }
    }

    fun onTitleChange(value: String) {
        title = value
        sourceFlow.value = "$value\n$content"
    }

    fun onContentChange(value: String) {
        content = value
        sourceFlow.value = "$title\n$value"
    }

    /** AI 제목 생성 등 내부에서 제목을 바꿀 때도 추천 입력을 함께 갱신한다. */
    private fun setTitleInternal(value: String) {
        title = value
        sourceFlow.value = "$value\n$content"
    }

    /**
     * 이 메모가 놓일 날짜. 기존 메모=저장된 date, 새 메모=캘린더에서 받은 initialDate,
     * 그 외(메모 탭에서 새로 작성)=오늘. "날짜 위에 메모가 얹힌다"(설계원칙 5)를 지켜
     * 어떤 경로로 만들어도 캘린더에서 보이게 한다.
     */
    private fun noteDate(): Long = loaded?.date ?: initialDate ?: today().startOfDayMillis()

    /**
     * 명시적 저장(저장·뒤로). 빈 메모 방지: 새 메모인데 제목·본문이 모두 비면 만들지 않는다.
     * (단, 할 일을 이미 붙였다면 [idFlow] 가 비어 있지 않아 정상 갱신된다.)
     */
    fun save(onSaved: () -> Unit = {}) {
        if (idFlow.value == null && title.isBlank() && content.isBlank()) return
        viewModelScope.launch {
            maybeAutoTitle()
            persist()
            onSaved()
        }
    }

    /**
     * 제목칸 옆 ✨ 버튼 — AI 로 제목 생성. 실패(키 없음·오프라인·오류)하면 본문 첫 줄로 폴백한다.
     * 이미 진행 중이거나 본문이 비면 아무것도 안 한다.
     */
    fun suggestTitleNow() {
        if (titleLoading || content.isBlank()) return
        titleLoading = true
        viewModelScope.launch {
            setTitleInternal(generateTitleOrFallback())
            titleLoading = false
        }
    }

    /** 저장 시 자동 제목: 토글 ON + 제목 비었고 본문 있을 때만. */
    private suspend fun maybeAutoTitle() {
        if (title.isNotBlank() || content.isBlank()) return
        if (!settings.isAutoTitleEnabled()) return
        setTitleInternal(generateTitleOrFallback())
    }

    /** AI 제목 생성 시도 → 실패 시 본문 첫 줄(마크다운 기호 제거). */
    private suspend fun generateTitleOrFallback(): String =
        suggestTitle(content).getOrElse { firstLineTitle(content) }

    /** 본문 첫 비어있지 않은 줄을 제목으로. `#`·`-`·`*`·`>` 등 마크다운 앞머리 기호 제거, 40자 컷. */
    private fun firstLineTitle(text: String): String =
        text.lineSequence()
            .map { it.trim().trimStart('#', '-', '*', '>', ' ', '\t').trim() }
            .firstOrNull { it.isNotBlank() }
            ?.take(40)
            ?: ""

    /** 새 메모면 생성, 기존이면 갱신. 항상 메모를 보장한다(할 일 부모 확보용으로도 쓰임). */
    private suspend fun persist() {
        val id = idFlow.value
        if (id == null) {
            // 날짜를 항상 채워(캘린더에서 열었으면 그 날짜, 아니면 오늘) 캘린더에서 보이게 한다.
            val created = addNote(title, content, noteDate())
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
            // 메모의 날짜를 dueDate 로 상속해 캘린더(해당 날짜)에서도 보이게 한다.
            addTask(text, id, dueDate = noteDate(), allDay = true)
        }
    }

    fun toggle(taskId: String) {
        viewModelScope.launch { toggleTask(taskId) }
    }

    fun removeTask(taskId: String) {
        viewModelScope.launch { deleteTask(taskId) }
    }

    /**
     * 저장하지 않은 변경이 있는지 — 뒤로 나갈 때 "저장하지 않고 나가시겠습니까?" 확인용.
     * 새 메모(미저장): 뭐라도 입력했으면 dirty. 기존 메모: 마지막 저장본과 제목/본문이 다르면 dirty.
     * (할 일은 추가 즉시 저장되므로 비교 대상이 아니다.)
     */
    fun isDirty(): Boolean {
        val base = loaded
        return if (base == null) {
            idFlow.value == null && (title.isNotBlank() || content.isNotBlank())
        } else {
            title != base.title || content != base.content
        }
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

    private companion object {
        /** 유사 메모 추천 디바운스 — 타이핑을 묶어 검색을 과도하게 돌리지 않는다. */
        const val RELATED_DEBOUNCE_MS = 450L
    }
}
