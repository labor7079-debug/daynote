package com.kangtaeyoung.daynote.domain.usecase

import com.kangtaeyoung.daynote.data.repository.AiRepository
import com.kangtaeyoung.daynote.domain.model.AiAction
import com.kangtaeyoung.daynote.domain.model.AiResult
import kotlinx.coroutines.flow.Flow

/**
 * AI UseCase. ViewModel 은 OpenAI 를 전혀 모르고 이 래퍼만 의존한다(MVVM + UseCase, 설계원칙 4).
 */
class RunAiActionUseCase(private val repo: AiRepository) {
    suspend operator fun invoke(action: AiAction, text: String, noteId: String?): Result<AiResult> =
        repo.run(action, text, noteId)
}

/** 자유 질문 — 메모를 맥락으로 OpenAI 에 질문한다(4-B, 인라인 답변). */
class AskAiUseCase(private val repo: AiRepository) {
    suspend operator fun invoke(question: String, sourceText: String, noteId: String?): Result<AiResult> =
        repo.ask(question, sourceText, noteId)
}

/** 메모 내용으로 짧은 제목을 생성한다(4-B). 실패 시 상위에서 "본문 첫 줄" 폴백. */
class SuggestTitleUseCase(private val repo: AiRepository) {
    suspend operator fun invoke(sourceText: String): Result<String> = repo.suggestTitle(sourceText)
}

class ObserveAiResultsUseCase(private val repo: AiRepository) {
    operator fun invoke(noteId: String): Flow<List<AiResult>> = repo.observeResults(noteId)
}
