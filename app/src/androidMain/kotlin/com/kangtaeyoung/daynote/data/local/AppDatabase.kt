package com.kangtaeyoung.daynote.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kangtaeyoung.daynote.data.local.dao.NoteDao
import com.kangtaeyoung.daynote.data.local.dao.TaskDao
import com.kangtaeyoung.daynote.data.local.entity.NoteEntity
import com.kangtaeyoung.daynote.data.local.entity.NoteFtsEntity
import com.kangtaeyoung.daynote.data.local.entity.TaskEntity

/**
 * 앱 로컬 DB — 오프라인 우선, 진실의 원천.
 *
 * 모든 엔티티가 Room 기본 지원 타입(String/Long/Boolean/Int)만 사용하므로
 * 타입 컨버터는 아직 필요 없다.
 */
@Database(
    entities = [
        NoteEntity::class,
        TaskEntity::class,
        NoteFtsEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    abstract fun taskDao(): TaskDao

    companion object {
        const val DATABASE_NAME = "daynote.db"
    }
}
