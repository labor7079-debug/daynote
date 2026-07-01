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

class ObserveAiResultsUseCase(private val repo: AiRepository) {
    operator fun invoke(noteId: String): Flow<List<AiResult>> = repo.observeResults(noteId)
}
