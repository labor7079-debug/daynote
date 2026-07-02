package com.kangtaeyoung.daynote.data.repository

import androidx.room.Room
import com.kangtaeyoung.daynote.data.local.AppDatabase
import com.kangtaeyoung.daynote.data.local.buildDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 유사 메모 실시간 추천 검증 — 키워드 OR 매칭 정제([toFtsMatchAny])와
 * [NoteRepository.searchRelated] 의 자기 자신·같은 날짜 제외 규칙을 확인한다.
 */
class RelatedNotesTest {

    private val dbFile: File = File.createTempFile("daynote-related", ".db").also { it.delete() }
    private val db: AppDatabase = buildDatabase(
        Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath),
    )
    private val changes = com.kangtaeyoung.daynote.data.sync.LocalChangeNotifier()
    private val notes = NoteRepositoryImpl(db.noteDao(), changes)

    @AfterTest
    fun tearDown() {
        db.close()
        dbFile.delete()
    }

    @Test
    fun toFtsMatchAnyBuildsOrQueryFromKeywords() {
        // 키워드가 OR 로 묶이고 접두 매칭(*)이 붙는다.
        val match = toFtsMatchAny("회의 준비 회의")
        assertTrue(match!!.contains(" OR "), "OR 매칭이어야 한다: $match")
        assertTrue(match.split(" OR ").all { it.endsWith("*") })
        // 빈도 우선: "회의"(2회)가 앞에 온다.
        assertTrue(match.startsWith("회의*"), "빈도 높은 토큰이 먼저: $match")
        // 1글자·숫자만인 토큰, 빈 입력은 제외/무효.
        assertNull(toFtsMatchAny("a 1 2"))
        assertNull(toFtsMatchAny("   "))
    }

    @Test
    fun searchRelatedFindsPartialOverlapAndExcludesSelfAndSameDate() = runBlocking<Unit> {
        val day1 = 1_000_000L
        val day2 = 2_000_000L
        val current = notes.addNote("오늘 회의", "프로젝트 일정 회의 준비", date = day1)
        val other = notes.addNote("지난 회의록", "회의 내용 정리", date = day2)
        notes.addNote("같은 날 메모", "회의 관련 다른 기록", date = day1)
        notes.addNote("무관한 메모", "장보기 목록", date = day2)

        // 키워드 하나("회의")만 겹쳐도 찾고, 자기 자신·같은 날짜는 제외한다.
        val related = notes.searchRelated(
            sourceText = "프로젝트 회의 준비",
            excludeNoteId = current.id,
            excludeDate = day1,
        ).first()

        assertEquals(listOf(other.id), related.map { it.id })
    }
}
