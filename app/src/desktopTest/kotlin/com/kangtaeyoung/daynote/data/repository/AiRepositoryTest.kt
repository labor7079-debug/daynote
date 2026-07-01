package com.kangtaeyoung.daynote.data.repository

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.kangtaeyoung.daynote.data.local.AppDatabase
import com.kangtaeyoung.daynote.data.local.entity.AiResultEntity
import com.kangtaeyoung.daynote.data.remote.openai.OpenAiClient
import com.kangtaeyoung.daynote.data.security.ApiKeyProvider
import com.kangtaeyoung.daynote.domain.model.AiAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Phase 4-B 스모크 — AI 계층 검증(네트워크 없이).
 * 1) AiResultDao 왕복 + 메모별 최신순 정렬.
 * 2) API 키 미설정 시 run() 이 안전하게 실패하고 DB 에 아무것도 쓰지 않는다(네트워크 호출 전 차단).
 */
class AiRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var dbFile: File

    private class FakeKeys(private var key: String?) : ApiKeyProvider {
        override fun openAiKey(): String? = key
        override fun setOpenAiKey(key: String) { this.key = key }
        override fun clear() { key = null }
    }

    @BeforeTest
    fun setup() {
        dbFile = File.createTempFile("daynote-ai-test", ".db").also { it.delete() }
        db = Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
    }

    @AfterTest
    fun tearDown() {
        db.close()
        dbFile.delete()
    }

    @Test
    fun aiResultDao_roundTrip_orderedByCreatedAtDesc() = runBlocking {
        val dao = db.aiResultDao()
        dao.upsert(AiResultEntity("a", "note-1", "SUMMARIZE", "src", "old", "m", createdAt = 100))
        dao.upsert(AiResultEntity("b", "note-1", "EXPAND", "src", "new", "m", createdAt = 200))
        dao.upsert(AiResultEntity("c", "note-2", "SUMMARIZE", "src", "other", "m", createdAt = 300))

        val forNote1 = dao.observeForNote("note-1").first()
        assertEquals(2, forNote1.size)
        assertEquals("new", forNote1[0].resultText) // 최신순(createdAt desc)
        assertEquals("old", forNote1[1].resultText)
    }

    @Test
    fun run_withoutApiKey_failsAndWritesNothing() = runBlocking {
        val repo = AiRepositoryImpl(
            api = OpenAiClient(),
            keys = FakeKeys(null),
            dao = db.aiResultDao(),
        )

        val result = repo.run(AiAction.SUMMARIZE, "요약할 내용", noteId = "note-1")

        assertTrue(result.isFailure, "키가 없으면 실패해야 한다")
        assertEquals(0, db.aiResultDao().observeForNote("note-1").first().size)
    }

    @Test
    fun suggestTitle_withoutApiKey_failsSafely() = runBlocking {
        // 키가 없으면 네트워크 호출 전 실패해야 한다(상위에서 "본문 첫 줄" 폴백으로 처리).
        val repo = AiRepositoryImpl(api = OpenAiClient(), keys = FakeKeys(null), dao = db.aiResultDao())

        val result = repo.suggestTitle("오늘 회의 준비: 슬라이드 3장, 예산 검토")

        assertTrue(result.isFailure, "키가 없으면 제목 생성은 실패해야 한다")
    }

    @Test
    fun suggestTitle_blankSource_failsSafely() = runBlocking {
        // 키가 있어도 내용이 비면 실패(불필요한 호출 방지).
        val repo = AiRepositoryImpl(api = OpenAiClient(), keys = FakeKeys("sk-test"), dao = db.aiResultDao())

        val result = repo.suggestTitle("   ")

        assertTrue(result.isFailure, "빈 내용이면 실패해야 한다")
    }
}
