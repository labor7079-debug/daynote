package com.kangtaeyoung.daynote.data.repository

import com.kangtaeyoung.daynote.core.nowMillis
import com.kangtaeyoung.daynote.core.randomUuid
import com.kangtaeyoung.daynote.data.local.dao.AiResultDao
import com.kangtaeyoung.daynote.data.local.entity.AiResultEntity
import com.kangtaeyoung.daynote.data.remote.openai.OpenAiClient
import com.kangtaeyoung.daynote.data.security.ApiKeyProvider
import com.kangtaeyoung.daynote.domain.model.AiAction
import com.kangtaeyoung.daynote.domain.model.AiResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * AI 데이터 계층(설계원칙 4). UI·도메인은 이 인터페이스만 본다 — OpenAI 호출·파싱·저장은
 * 구현체 한 곳에 가둔다(제공자 단일화). 다른 도구로 바꿀 일이 생기면 이 파일만 교체한다.
 */
interface AiRepository {
    suspend fun run(action: AiAction, sourceText: String, noteId: String?): Result<AiResult>

    /** 자유 질문 — 메모([sourceText])를 맥락으로 [question] 에 답한다. 메모가 비면 순수 질문. */
    suspend fun ask(question: String, sourceText: String, noteId: String?): Result<AiResult>

    fun observeResults(noteId: String): Flow<List<AiResult>>
}

class AiRepositoryImpl(
    private val api: OpenAiClient,
    private val keys: ApiKeyProvider,
    private val dao: AiResultDao,
    private val model: String = "gpt-4o-mini", // 교체 가능
) : AiRepository {

    override suspend fun run(
        action: AiAction,
        sourceText: String,
        noteId: String?,
    ): Result<AiResult> = runCatching {
        val key = keys.openAiKey()?.takeIf { it.isNotBlank() }
            ?: error("OpenAI API 키가 설정되지 않았습니다. 설정에서 키를 입력하세요.")
        if (sourceText.isBlank()) error("보낼 내용이 없습니다.")

        // ① 보내기: 도메인 동작 → OpenAI 메시지
        val text = api.chat(
            apiKey = key,
            model = model,
            system = action.systemPrompt,
            user = "${action.instruction}\n\n$sourceText",
        )

        // ② 저장: Room(진실의 원천) 기록 후 도메인 모델 반환 → Flow 로 화면 자동 갱신
        AiResult(
            id = randomUuid(),
            noteId = noteId,
            action = action,
            sourceText = sourceText,
            resultText = text,
            model = model,
            createdAt = nowMillis(),
        ).also { dao.upsert(it.toEntity()) }
    }

    override suspend fun ask(
        question: String,
        sourceText: String,
        noteId: String?,
    ): Result<AiResult> = runCatching {
        val key = keys.openAiKey()?.takeIf { it.isNotBlank() }
            ?: error("OpenAI API 키가 설정되지 않았습니다. 설정에서 키를 입력하세요.")
        if (question.isBlank()) error("질문을 입력하세요.")

        // 메모가 있으면 맥락으로 함께 보낸다(없으면 순수 질문).
        val user = if (sourceText.isBlank()) {
            question
        } else {
            "다음은 내 메모야:\n\n$sourceText\n\n---\n\n위 메모를 참고해서 답해줘.\n질문: $question"
        }
        val text = api.chat(apiKey = key, model = model, system = AiAction.ASK.systemPrompt, user = user)

        // 이력에는 질문 문장을 sourceText 로 남긴다(무엇을 물었는지 보이도록).
        AiResult(
            id = randomUuid(),
            noteId = noteId,
            action = AiAction.ASK,
            sourceText = question,
            resultText = text,
            model = model,
            createdAt = nowMillis(),
        ).also { dao.upsert(it.toEntity()) }
    }

    override fun observeResults(noteId: String): Flow<List<AiResult>> =
        dao.observeForNote(noteId).map { list -> list.map { it.toDomain() } }
}

private fun AiResult.toEntity(): AiResultEntity = AiResultEntity(
    id = id,
    noteId = noteId,
    action = action.name,
    sourceText = sourceText,
    resultText = resultText,
    model = model,
    createdAt = createdAt,
)

private fun AiResultEntity.toDomain(): AiResult = AiResult(
    id = id,
    noteId = noteId,
    action = runCatching { AiAction.valueOf(action) }.getOrDefault(AiAction.SUMMARIZE),
    sourceText = sourceText,
    resultText = resultText,
    model = model,
    createdAt = createdAt,
)
