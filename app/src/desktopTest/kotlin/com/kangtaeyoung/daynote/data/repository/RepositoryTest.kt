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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Phase 1 Step 3 검증 — Repository 가 DAO 위에서 도메인 모델·id/타임스탬프·검색 정제·소프트삭제를
 * 올바르게 처리하는지 데스크톱 JVM 에서 왕복 확인한다.
 */
class RepositoryTest {

    private val dbFile: File = File.createTempFile("daynote-repo", ".db").also { it.delete() }
    private val db: AppDatabase = buildDatabase(
        Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath),
    )
    private val changes = com.kangtaeyoung.daynote.data.sync.LocalChangeNotifier()
    private val notes = NoteRepositoryImpl(db.noteDao(), changes)
    private val tasks = TaskRepositoryImpl(db.taskDao(), changes)

    @AfterTest
    fun tearDown() {
        db.close()
        dbFile.delete()
    }

    @Test
    fun addNoteGeneratesIdAndPersists() = runBlocking<Unit> {
        val created = notes.addNote(title = "회의", content = "캘린더 정리", date = null)
        assertTrue(created.id.isNotBlank(), "id 가 생성돼야 한다")
        assertEquals(created.createdAt, created.updatedAt)

        val all = notes.observeNotes().first()
        assertEquals(1, all.size)
        assertEquals("회의", all.first().title)
    }

    @Test
    fun searchRefinesUserInputToPrefixMatch() = runBlocking<Unit> {
        notes.addNote("Meeting", "calendar sync tomorrow")
        notes.addNote("Groceries", "milk and eggs")

        // 부분 입력 "cal" 이 접두 매칭(calendar)으로 잡혀야 한다.
        val hits = notes.search("cal").first()
        assertEquals(1, hits.size)
        assertEquals("Meeting", hits.first().title)

        // 빈 입력은 빈 결과.
        assertTrue(notes.search("   ").first().isEmpty())
    }

    @Test
    fun deleteNoteIsSoftAndHiddenFromQueries() = runBlocking<Unit> {
        val n = notes.addNote("temp", "to be deleted")
        notes.deleteNote(n.id)

        assertTrue(notes.observeNotes().first().isEmpty(), "소프트 삭제 후 목록에서 빠져야 한다")
        assertNotNull(notes.restoreNote(n.id))
        assertEquals(1, notes.observeNotes().first().size, "복구되면 다시 보여야 한다")
    }

    @Test
    fun generalTasksIncludeDatedStandaloneTasks() = runBlocking<Unit> {
        // 캘린더에서 만든 할 일(dueDate 있음, noteId 없음)도 To-Do 탭 목록에 보여야 한다.
        tasks.addTask(text = "dated", noteId = null, dueDate = 1_700_000_000_000L)
        tasks.addTask(text = "undated", noteId = null, dueDate = null)
        // 메모에 속한 할 일은 To-Do 마스터 목록에서 제외.
        val note = notes.addNote("p", "")
        tasks.addTask(text = "note task", noteId = note.id)

        val general = tasks.observeGeneralTasks().first()
        assertEquals(2, general.size, "독립 할 일(날짜 유무 무관)만, 메모 소속은 제외")
        assertTrue(general.any { it.text == "dated" })
        assertTrue(general.none { it.text == "note task" })
    }

    @Test
    fun updatePreservesRemoteIdAndMarksPending() = runBlocking<Unit> {
        val n = notes.addNote("title", "body", date = 1_700_000_000_000L)
        // 동기화 완료 상태를 흉내: remoteId 부여 + SYNCED.
        db.noteDao().markSynced(n.id, "evt-123")

        notes.updateNote(n.copy(title = "title2"))

        val e = db.noteDao().getById(n.id)!!
        assertEquals("evt-123", e.remoteId, "수정해도 remoteId 는 보존돼야 한다")
        assertEquals("PENDING", e.syncStatus, "수정 시 재동기화 대상으로 PENDING")
        assertEquals("title2", e.title)
    }

    @Test
    fun syncEnabledTogglePersists() = runBlocking<Unit> {
        val settings = SettingsRepositoryImpl(db.settingDao())
        assertEquals(false, settings.observeSyncEnabled().first(), "기본값은 off")
        settings.setSyncEnabled(true)
        assertEquals(true, settings.observeSyncEnabled().first(), "저장 후 on 유지")
        settings.setSyncEnabled(false)
        assertEquals(false, settings.observeSyncEnabled().first())
    }

    @Test
    fun taskToggleFlipsDoneState() = runBlocking<Unit> {
        val note = notes.addNote("project", "")
        val t = tasks.addTask(text = "first step", noteId = note.id)
        assertEquals(false, t.isDone)

        tasks.toggleDone(t.id)
        val afterToggle = tasks.observeTasksForNote(note.id).first().first()
        assertTrue(afterToggle.isDone, "토글 후 완료 상태여야 한다")
    }
}
