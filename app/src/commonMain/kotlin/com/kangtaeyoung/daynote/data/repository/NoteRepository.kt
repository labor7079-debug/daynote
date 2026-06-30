package com.kangtaeyoung.daynote.data.repository

import com.kangtaeyoung.daynote.domain.model.Note
import kotlinx.coroutines.flow.Flow

/**
 * 메모 데이터 접근 추상화(설계원칙 4). UI·도메인은 이 인터페이스만 본다.
 *
 * 읽기는 모두 `Flow`(로컬 우선·변경 자동 반영), 쓰기는 `suspend`.
 * UUID·타임스탬프·소프트삭제 같은 규칙은 구현체가 책임진다 — 호출부는 의미만 표현한다.
 */
interface NoteRepository {

    /** 활성 메모 전체(고정 우선, 최근 수정 순). */
    fun observeNotes(): Flow<List<Note>>

    /** 특정 날짜 구간 [startOfDay, endOfDay) 의 메모 (캘린더 동선, Phase 2). */
    fun observeNotesByDate(startOfDay: Long, endOfDay: Long): Flow<List<Note>>

    /** 날짜 없는 메모(보조 목록). */
    fun observeUndatedNotes(): Flow<List<Note>>

    /** 단건 관찰(에디터 화면). */
    fun observeNote(id: String): Flow<Note?>

    suspend fun getNote(id: String): Note?

    /** 새 메모 생성 — id·createdAt·updatedAt 을 구현체가 채워 저장하고 도메인 모델로 돌려준다. */
    suspend fun addNote(title: String, content: String, date: Long? = null): Note

    /** 기존 메모 갱신 — updatedAt 을 현재 시각으로 찍는다. */
    suspend fun updateNote(note: Note)

    suspend fun setPinned(id: String, pinned: Boolean)

    /** 소프트 삭제(즉시 삭제 아님 — 동기화 대비). */
    suspend fun deleteNote(id: String)

    suspend fun restoreNote(id: String)

    /** 본문/제목 전문 검색. 사용자 입력은 구현체가 FTS MATCH 문법으로 정제한다. 빈 입력은 빈 결과. */
    fun search(query: String): Flow<List<Note>>
}
