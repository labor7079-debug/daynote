package com.kangtaeyoung.daynote.di

import androidx.room.Room
import com.kangtaeyoung.daynote.data.local.AppDatabase
import com.kangtaeyoung.daynote.data.local.dao.NoteDao
import com.kangtaeyoung.daynote.data.local.dao.TaskDao
import com.kangtaeyoung.daynote.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Phase 0.5-C 스모크 테스트 — Koin DI 그래프가 실제로 해석되는지 검증한다.
 *
 * 전역 `startKoin` 대신 격리된 [koinApplication] 을 쓰고, 플랫폼 빌더(`~/.daynote`) 대신
 * 임시 파일 빌더를 등록해 부수효과 없이 [databaseModule] 의 배선(AppDatabase → DAO)만 확인한다.
 */
class KoinGraphTest {

    @Test
    fun databaseGraphResolvesAndWorks() = runBlocking<Unit> {
        val dbFile = File.createTempFile("daynote-koin-graph", ".db").also { it.delete() }
        val app = koinApplication {
            modules(
                module { single { Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath) } },
                databaseModule,
            )
        }
        val koin = app.koin

        val db = koin.get<AppDatabase>()
        val noteDao = koin.get<NoteDao>()
        val taskDao = koin.get<TaskDao>()
        assertNotNull(db)
        assertNotNull(noteDao)
        assertNotNull(taskDao)

        // 주입받은 DAO 가 실제 같은 DB 에 동작하는지 왕복 확인.
        val now = System.currentTimeMillis()
        noteDao.upsert(NoteEntity(id = "koin-1", title = "DI", content = "wired", createdAt = now, updatedAt = now))
        val loaded = noteDao.getById("koin-1")
        assertEquals("wired", loaded?.content)

        db.close()
        app.close()
        dbFile.delete()
    }
}
