package com.kangtaeyoung.daynote.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4

/**
 * 메모 전문 검색용 FTS4 그림자 테이블.
 *
 * [contentEntity] 로 [NoteEntity] 의 `notes` 원본 테이블을 그대로 백킹한다(external content).
 * 본문 데이터를 중복 저장하지 않고, Room 이 원본 ↔ FTS 동기화 트리거를 자동 생성한다.
 * 컬럼명([title]/[content])은 [NoteEntity] 와 일치해야 하며, rowid 로 원본과 연결된다.
 *
 * 검색 예: `SELECT * FROM notes WHERE rowid IN (SELECT rowid FROM notes_fts WHERE notes_fts MATCH :query)`
 *
 * ⚠️ Room KMP 에서 `@Fts4` 동작은 공식 보증이 없어, 데스크톱 JVM 스모크 테스트(FtsSmokeTest)로 검증한다.
 */
@Fts4(contentEntity = NoteEntity::class)
@Entity(tableName = "notes_fts")
data class NoteFtsEntity(
    val title: String,
    val content: String,
)
