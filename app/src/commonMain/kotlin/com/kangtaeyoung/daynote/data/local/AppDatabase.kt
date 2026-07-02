package com.kangtaeyoung.daynote.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.kangtaeyoung.daynote.data.local.dao.AiResultDao
import com.kangtaeyoung.daynote.data.local.dao.NoteDao
import com.kangtaeyoung.daynote.data.local.dao.SettingDao
import com.kangtaeyoung.daynote.data.local.dao.TaskDao
import com.kangtaeyoung.daynote.data.local.entity.AiResultEntity
import com.kangtaeyoung.daynote.data.local.entity.AppSettingEntity
import com.kangtaeyoung.daynote.data.local.entity.NoteEntity
import com.kangtaeyoung.daynote.data.local.entity.NoteFtsEntity
import com.kangtaeyoung.daynote.data.local.entity.TaskEntity

/**
 * 앱 로컬 DB — 오프라인 우선, 진실의 원천. Room KMP(`commonMain`)로 Android·Desktop 공유.
 *
 * 모든 엔티티가 Room 기본 지원 타입(String/Long/Boolean/Int)만 사용하므로
 * 타입 컨버터는 아직 필요 없다.
 */
@Database(
    entities = [
        NoteEntity::class,
        TaskEntity::class,
        NoteFtsEntity::class,
        AppSettingEntity::class,
        AiResultEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    abstract fun taskDao(): TaskDao

    abstract fun settingDao(): SettingDao

    abstract fun aiResultDao(): AiResultDao

    companion object {
        const val DATABASE_NAME = "daynote.db"
    }
}

/**
 * Room KMP 필수 — 각 플랫폼의 `actual` 구현을 Room 컴파일러가 생성한다.
 * 직접 구현하지 않으며, 컴파일러가 채우므로 `expect` 의 미구현 경고를 억제한다.
 */
@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect", "EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
