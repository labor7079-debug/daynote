package com.kangtaeyoung.daynote.data.local

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * v1 → v2: tasks 에 allDay 컬럼 추가(종일/시간 구분). 기존 행은 종일(1)로 본다.
 * 데이터 보존 마이그레이션 — 기기의 기존 메모/할 일은 유지된다.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE tasks ADD COLUMN allDay INTEGER NOT NULL DEFAULT 1")
    }
}

/** v2 → v3: 키-값 설정 테이블 추가. */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `settings` (`key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`))",
        )
    }
}

/** v4 → v5: tasks 에 endDate 컬럼 추가(여러 날에 걸친 기간 할 일). 기존 행은 하루짜리(null). */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE tasks ADD COLUMN endDate INTEGER")
    }
}

/** v3 → v4: AI 결과 테이블 추가(Phase 4-B). noteId·createdAt 인덱스 포함. */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `ai_results` (" +
                "`id` TEXT NOT NULL, `noteId` TEXT, `action` TEXT NOT NULL, " +
                "`sourceText` TEXT NOT NULL, `resultText` TEXT NOT NULL, " +
                "`model` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_ai_results_noteId` ON `ai_results` (`noteId`)",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_ai_results_createdAt` ON `ai_results` (`createdAt`)",
        )
    }
}
