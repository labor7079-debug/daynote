package com.kangtaeyoung.daynote.data.local

import androidx.room.Room
import com.kangtaeyoung.daynote.data.repository.TaskRepositoryImpl
import com.kangtaeyoung.daynote.data.sync.LocalChangeNotifier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * 기간 할 일(v5, endDate) 검증 — 날짜 범위 조회가 **겹침** 의미로 동작해
 * 기간 중간 날짜에서도 잡히고, 하루짜리(endDate=null)는 기존 의미를 유지한다.
 */
class RangedTaskQueryTest {

    private val dbFile: File = File.createTempFile("daynote-ranged", ".db").also { it.delete() }
    private val db: AppDatabase = buildDatabase(
        Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath),
    )
    private val tasks = TaskRepositoryImpl(db.taskDao(), LocalChangeNotifier())

    private val day = 86_400_000L
    private fun dayRangeOf(startOfDay: Long) = startOfDay to startOfDay + day

    @AfterTest
    fun tearDown() {
        db.close()
        dbFile.delete()
    }

    @Test
    fun rangedTaskAppearsOnEveryDayOfSpan() = runBlocking<Unit> {
        val d1 = 0L
        val d2 = day
        val d3 = 2 * day
        val d4 = 3 * day
        tasks.addTask("3일짜리", null, d1, allDay = true, endDate = d3)
        tasks.addTask("하루짜리", null, d2, allDay = true)

        val (s2, e2) = dayRangeOf(d2) // 기간 중간 날짜
        assertEquals(
            setOf("3일짜리", "하루짜리"),
            tasks.observeTasksByDueDate(s2, e2).first().map { it.text }.toSet(),
        )
        val (s3, e3) = dayRangeOf(d3) // 기간 마지막 날짜(포함)
        assertEquals(listOf("3일짜리"), tasks.observeTasksByDueDate(s3, e3).first().map { it.text })
        val (s4, e4) = dayRangeOf(d4) // 기간 종료 다음날(제외)
        assertEquals(emptyList(), tasks.observeTasksByDueDate(s4, e4).first().map { it.text })
    }

    @Test
    fun endDateBeforeStartIsNormalizedToSingleDay() = runBlocking<Unit> {
        val d2 = day
        val created = tasks.addTask("역전 기간", null, d2, allDay = true, endDate = 0L)
        assertEquals(null, created.endDate, "종료일이 시작 이전이면 하루짜리로 정규화")
    }
}
