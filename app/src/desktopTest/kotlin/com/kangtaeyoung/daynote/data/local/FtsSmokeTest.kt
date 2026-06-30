package com.kangtaeyoung.daynote.data.local

import androidx.room.Room
import com.kangtaeyoung.daynote.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Phase 0.5-B 스모크 테스트 — `@Fts4` external-content 검색이 **데스크톱 JVM + BundledSQLiteDriver**
 * 환경에서 실제로 동작하는지 검증한다.
 *
 * Room KMP 의 FTS 동작은 공식 보증이 없어(번들 SQLite 의 FTS 컴파일 여부, 트리거 생성, 토크나이저 동작 등)
 * 직접 데이터를 넣고 MATCH 검색으로 결과를 확인해야 한다. JUnit 은 테스트 메서드마다 새 인스턴스를
 * 만들므로 각 테스트는 독립된 임시 DB 를 쓴다.
 */
class FtsSmokeTest {

    private val dbFile: File = File.createTempFile("daynote-fts-smoke", ".db").also { it.delete() }
    private val db: AppDatabase = buildDatabase(
        Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath),
    )

    @AfterTest
    fun tearDown() {
        db.close()
        dbFile.delete()
    }

    /** external content FTS 트리거가 자동으로 동기화돼, 매칭되는 메모 1건만 돌려준다. */
    @Test
    fun ftsSearchReturnsOnlyMatchingNote() = runBlocking {
        val dao = db.noteDao()
        val now = System.currentTimeMillis()
        dao.upsert(NoteEntity(id = "1", title = "Meeting", content = "calendar sync tomorrow", createdAt = now, updatedAt = now))
        dao.upsert(NoteEntity(id = "2", title = "Groceries", content = "milk and eggs", createdAt = now, updatedAt = now))

        val hits = dao.search("calendar").first()

        assertEquals(1, hits.size, "FTS MATCH 'calendar' 는 1건만 매칭해야 한다")
        assertEquals("1", hits.first().id)
    }

    /** 한글 토큰도 매칭되는지(기본 토크나이저의 CJK 처리) 확인한다. */
    @Test
    fun ftsSearchMatchesKoreanToken() = runBlocking {
        val dao = db.noteDao()
        val now = System.currentTimeMillis()
        dao.upsert(NoteEntity(id = "10", title = "회의록", content = "내일 캘린더 회의 준비", createdAt = now, updatedAt = now))
        dao.upsert(NoteEntity(id = "11", title = "장보기", content = "우유 계란 구입", createdAt = now, updatedAt = now))

        val hits = dao.search("캘린더").first()

        assertTrue(hits.any { it.id == "10" }, "한글 토큰 '캘린더' 가 매칭돼야 한다")
        assertTrue(hits.none { it.id == "11" }, "관련 없는 메모는 제외돼야 한다")
    }

    /** 소프트 삭제된 메모는 FTS 검색에서 제외된다(DAO 의 deletedAt 필터). */
    @Test
    fun ftsSearchExcludesSoftDeleted() = runBlocking {
        val dao = db.noteDao()
        val now = System.currentTimeMillis()
        dao.upsert(NoteEntity(id = "20", title = "Report", content = "quarterly report draft", createdAt = now, updatedAt = now))
        dao.softDelete("20", now)

        val hits = dao.search("quarterly").first()

        assertTrue(hits.isEmpty(), "소프트 삭제된 메모는 검색되지 않아야 한다")
    }
}
