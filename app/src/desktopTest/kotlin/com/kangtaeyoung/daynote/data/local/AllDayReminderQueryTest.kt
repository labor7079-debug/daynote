package com.kangtaeyoung.daynote.data.local

import androidx.room.Room
import com.kangtaeyoung.daynote.data.repository.TaskRepositoryImpl
import com.kangtaeyoung.daynote.data.sync.LocalChangeNotifier
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 종일 할 일 기본시각 알림용 쿼리([TaskDao.getAllDayUpcoming]) 의미 검증 —
 * 오늘 이후 마감·종일·미완료·활성만 잡히고, 시간 지정/과거/완료는 빠진다.
 */
class AllDayReminderQueryTest {

    private val dbFile: File = File.createTempFile("daynote-allday", ".db").also { it.delete() }
    private val db: AppDatabase = buildDatabase(
        Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath),
    )
    private val tasks = TaskRepositoryImpl(db.taskDao(), LocalChangeNotifier())

    @AfterTest
    fun tearDown() {
        db.close()
        dbFile.delete()
    }

    @Test
    fun allDayUpcomingPicksOnlyActiveAllDayFromToday() = runBlocking<Unit> {
        val startOfToday = 1_000_000L
        val yesterday = startOfToday - 86_400_000L
        val tomorrow = startOfToday + 86_400_000L

        val todayAllDay = tasks.addTask("오늘 종일", null, startOfToday, allDay = true, sortOrder = 0)
        tasks.addTask("내일 종일", null, tomorrow, allDay = true, sortOrder = 0)
        tasks.addTask("어제 종일(과거)", null, yesterday, allDay = true, sortOrder = 0)
        tasks.addTask("시간 지정(제외)", null, tomorrow + 3_600_000L, allDay = false, sortOrder = 0)
        val done = tasks.addTask("완료된 종일(제외)", null, tomorrow, allDay = true, sortOrder = 0)
        tasks.toggleDone(done.id)

        val upcoming = db.taskDao().getAllDayUpcoming(startOfToday)
        assertEquals(
            setOf(todayAllDay.text, "내일 종일"),
            upcoming.map { it.text }.toSet(),
            "오늘·내일 종일 미완료만 잡혀야 한다",
        )
    }
}
