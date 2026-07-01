package com.kangtaeyoung.daynote.data.local

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.kangtaeyoung.daynote.data.local.entity.SyncStatus
import com.kangtaeyoung.daynote.data.local.entity.TaskEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * getTimedUpcoming 은 '시간 지정(allDay=0) · 미래 · 미완료 · 미삭제' 할 일만 반환한다(알림 대상).
 */
class ReminderQueryTest {

    private lateinit var db: AppDatabase
    private lateinit var dbFile: File

    private fun task(id: String, due: Long?, allDay: Boolean, done: Boolean = false, deleted: Long? = null) =
        TaskEntity(
            id = id, text = id, dueDate = due, allDay = allDay, isDone = done,
            createdAt = 1, updatedAt = 1, syncStatus = SyncStatus.LOCAL_ONLY, deletedAt = deleted,
        )

    @BeforeTest
    fun setup() {
        dbFile = File.createTempFile("daynote-rem", ".db").also { it.delete() }
        db = Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.Default)
            .build()
    }

    @AfterTest
    fun tearDown() {
        db.close(); dbFile.delete()
    }

    @Test
    fun getTimedUpcoming_onlyTimedFutureActive() = runBlocking {
        val now = 1_000L
        val dao = db.taskDao()
        dao.upsert(task("timed-future", due = 2_000L, allDay = false)) // ✅ 대상
        dao.upsert(task("allday-future", due = 2_000L, allDay = true)) // ✖ 종일
        dao.upsert(task("timed-past", due = 500L, allDay = false)) // ✖ 과거
        dao.upsert(task("timed-done", due = 2_000L, allDay = false, done = true)) // ✖ 완료
        dao.upsert(task("timed-deleted", due = 2_000L, allDay = false, deleted = 900L)) // ✖ 삭제
        dao.upsert(task("no-due", due = null, allDay = false)) // ✖ 마감 없음

        val upcoming = dao.getTimedUpcoming(now)
        assertEquals(listOf("timed-future"), upcoming.map { it.id })
    }
}
