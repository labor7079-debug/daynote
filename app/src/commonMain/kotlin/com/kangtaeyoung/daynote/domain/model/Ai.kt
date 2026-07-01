package com.kangtaeyoung.daynote.domain.model

/**
 * AI 동작은 프롬프트 템플릿을 함께 들고 있다(CLAUDE.md 4-B). 제공자(OpenAI)는 모름 —
 * Repository 구현체에서 이 템플릿을 메시지로 변환한다(설계원칙 4).
 */
enum class AiAction(
    val label: String,
    val systemPrompt: String,
    val instruction: String,
) {
    SUMMARIZE(
        label = "요약",
        systemPrompt = "너는 한국어 메모 비서다. 핵심만 간결하게 답한다.",
        instruction = "다음 메모를 3줄 이내로 요약해줘.",
    ),
    EXPAND(
        label = "확장",
        systemPrompt = "너는 한국어 글쓰기 보조다.",
        instruction = "다음 메모를 자연스럽게 확장해줘.",
    ),
    FIX_GRAMMAR(
        label = "교정",
        systemPrompt = "너는 한국어 교정기다. 의미는 유지한다.",
        instruction = "다음 메모의 맞춤법과 문장을 다듬어줘.",
    ),

    /** 자유 질문 — instruction 은 호출 시 사용자 질문으로 대체된다(칩이 아니라 입력창에서 사용). */
    ASK(
        label = "질문",
        systemPrompt = "너는 한국어 메모 비서다. 사용자의 메모를 참고해 질문에 정확하고 간결하게 답한다.",
        instruction = "",
    ),
}

/** AI 호출 1건의 결과(진실의 원천 Room 에 저장 → 오프라인·재시작에 유지). */
data class AiResult(
    val id: String,
    val noteId: String?,
    val action: AiAction,
    val sourceText: String,
    val resultText: String,
    val model: String,
    val createdAt: Long,
)
