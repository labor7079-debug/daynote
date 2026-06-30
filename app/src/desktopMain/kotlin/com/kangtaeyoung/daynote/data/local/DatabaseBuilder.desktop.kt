package com.kangtaeyoung.daynote.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

/**
 * Desktop(JVM): 사용자 홈의 `~/.daynote` 폴더에 데이터베이스 파일을 둔다.
 * 실제 빌드(드라이버 연결)는 공통 [buildDatabase] 가 마무리한다 — Koin 모듈(0.5-C)에서 호출.
 */
fun appDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val appDir = File(System.getProperty("user.home"), ".daynote").apply { mkdirs() }
    val dbFile = File(appDir, AppDatabase.DATABASE_NAME)
    return Room.databaseBuilder<AppDatabase>(name = dbFile.absolutePath)
}
