package com.kangtaeyoung.daynote.data.local

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

/**
 * 플랫폼이 만든 [RoomDatabase.Builder] 를 받아 번들 SQLite 드라이버로 완성한다.
 * Android·Desktop 양쪽이 공유하는 DB 생성 지점 — 진실의 원천.
 *
 * [BundledSQLiteDriver] 는 각 플랫폼에 SQLite 네이티브 라이브러리를 동봉해
 * OS 버전·환경에 상관없이 동일한 SQLite(FTS 포함)를 보장한다.
 */
fun buildDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase =
    builder
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()
