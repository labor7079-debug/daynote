package com.kangtaeyoung.daynote.data.backup

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.kangtaeyoung.daynote.data.local.AppDatabase
import com.kangtaeyoung.daynote.data.local.entity.NoteEntity
import com.kangtaeyoung.daynote.data.local.entity.SyncStatus
import com.kangtaeyoung.daynote.data.local.entity.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * 로컬 백업/복원 스모크 — 한 DB 에서 내보낸 JSON 을 새 DB 로 복원하면 내용이 그대로 살아난다.
 */
class BackupManagerTest {

    private lateinit var dbFile1: File
    private lateinit var dbFile2: File
    private lateinit var db1: AppDatabase
    private lateinit var db2: AppDatabase

    private fun newDb(f: File) = Room.databaseBuilder<AppDatabase>(name = f.absolutePath)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()

    @BeforeTest
    fun setup() {
        dbFile1 = File.createTempFile("daynote-bk1", ".db").also { it.delete() }
        dbFile2 = File.createTempFile("daynote-bk2", ".db").also { it.delete() }
        db1 = newDb(dbFile1)
        db2 = newDb(dbFile2)
    }

    @AfterTest
    fun tearDown() {
        db1.close(); db2.close()
        dbFile1.delete(); dbFile2.delete()
    }

    @Test
    fun export_then_import_roundTrips() = runBlocking {
        db1.noteDao().upsert(
            NoteEntity(id = "n1", title = "회의", content = "슬라이드 3장", date = 123L,
                isPinned = true, createdAt = 10, updatedAt = 20, syncStatus = SyncStatus.LOCAL_ONLY),
        )
        db1.taskDao().upsert(
            TaskEntity(id = "t1", text = "장보기", dueDate = 456L, allDay = false,
                createdAt = 11, updatedAt = 21, syncStatus = SyncStatus.LOCAL_ONLY),
        )

        val json = BackupManager(db1.noteDao(), db1.taskDao()).exportJson()
        assertTrue(json.contains("daynote-backup"), "포맷 표식 포함")

        val result = BackupManager(db2.noteDao(), db2.taskDao()).importJson(json)
        assertEquals(1, result.notes)
        assertEquals(1, result.tasks)

        val notes = db2.noteDao().getAllRaw()
        assertEquals(1, notes.size)
        assertEquals("회의", notes[0].title)
        assertEquals(true, notes[0].isPinned)
        val tasks = db2.taskDao().getAllRaw()
        assertEquals("장보기", tasks[0].text)
        assertEquals(false, tasks[0].allDay)
    }

    @Test
    fun import_rejectsNonBackupJson() = runBlocking {
        assertFailsWith<Exception> {
            BackupManager(db2.noteDao(), db2.taskDao()).importJson("""{"hello":"world"}""")
        }
        Unit
    }
}
