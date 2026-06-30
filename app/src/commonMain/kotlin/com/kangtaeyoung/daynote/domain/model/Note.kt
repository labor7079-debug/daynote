package com.kangtaeyoung.daynote.domain.model

/**
 * 메모 도메인 모델 — UI·도메인이 다루는 순수 모델.
 *
 * 동기화 메타(remoteId/syncStatus/deletedAt)는 데이터 계층 내부 사정이라 여기서 노출하지 않는다
 * (설계원칙 4: UI·도메인은 데이터 출처를 모른다). Repository 가 엔티티 ↔ 도메인 매핑을 책임진다.
 */
data class Note(
    val id: String,
    val title: String,
    val content: String, // 마크다운 원문
    val date: Long? = null, // 캘린더 연결용 (epoch millis, nullable)
    val isPinned: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
)
