package com.kangtaeyoung.daynote.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kangtaeyoung.daynote.domain.model.AiAction
import com.kangtaeyoung.daynote.domain.model.AiResult
import com.kangtaeyoung.daynote.domain.usecase.AskAiUseCase
import com.kangtaeyoung.daynote.domain.usecase.DeleteAiResultUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveAiResultsUseCase
import com.kangtaeyoung.daynote.domain.usecase.RunAiActionUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** AI 패널 상태 — 한 번에 하나(CLAUDE.md 4-B). */
sealed interface AiUiState {
    data object Idle : AiUiState
    data class Loading(val action: AiAction) : AiUiState
    data class Success(val result: AiResult) : AiUiState
    data class Error(val message: String) : AiUiState
}

/**
 * ViewModel 은 [RunAiActionUseCase] 만 호출하고 OpenAI 를 전혀 모른다(설계원칙 4).
 * 결과는 Repository 가 이미 Room 에 저장했으므로, "메모에 반영"은 노트 본문 갱신만 담당한다.
 */
class AiViewModel(
    private val runAiAction: RunAiActionUseCase,
    private val askAi: AskAiUseCase,
    observeAiResults: ObserveAiResultsUseCase,
    private val deleteAiResult: DeleteAiResultUseCase,
    noteId: String?,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AiUiState>(AiUiState.Idle)
    val uiState: StateFlow<AiUiState> = _uiState.asStateFlow()

    /**
     * 이 메모의 과거 AI 결과 이력(최신순). Room 이 진실의 원천이라 삭제·새 결과가 자동 반영된다.
     * 아직 저장 전(noteId == null)인 새 메모는 이력이 없다(빈 목록).
     */
    val history: StateFlow<List<AiResult>> =
        (if (noteId != null) observeAiResults(noteId) else emptyFlow())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun run(action: AiAction, sourceText: String, noteId: String?) {
        if (_uiState.value is AiUiState.Loading) return // 중복 호출 방지
        _uiState.value = AiUiState.Loading(action)
        viewModelScope.launch {
            runAiAction(action, sourceText, noteId)
                .onSuccess { _uiState.value = AiUiState.Success(it) }
                .onFailure { _uiState.value = AiUiState.Error(it.message ?: "AI 호출 실패") }
        }
    }

    /** 자유 질문 — 메모를 맥락으로 OpenAI 에 묻고 답을 인라인 표시(앱 전환 없음). */
    fun ask(question: String, sourceText: String, noteId: String?) {
        if (_uiState.value is AiUiState.Loading) return
        _uiState.value = AiUiState.Loading(AiAction.ASK)
        viewModelScope.launch {
            askAi(question, sourceText, noteId)
                .onSuccess { _uiState.value = AiUiState.Success(it) }
                .onFailure { _uiState.value = AiUiState.Error(it.message ?: "AI 호출 실패") }
        }
    }

    /** 이력 항목 1건 삭제 — Room 삭제 후 [history] Flow 가 자동 갱신. */
    fun deleteHistory(id: String) {
        viewModelScope.launch { deleteAiResult(id) }
    }

    fun reset() {
        _uiState.value = AiUiState.Idle
    }
}
