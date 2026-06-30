package com.kangtaeyoung.daynote.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Android: 앱 전용 데이터베이스 파일 경로로 [RoomDatabase.Builder] 를 만든다.
 * 실제 빌드(드라이버 연결)는 공통 [buildDatabase] 가 마무리한다 — Koin 모듈(0.5-C)에서 호출.
 */
fun appDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath(AppDatabase.DATABASE_NAME)
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = dbFile.absolutePath,
    )
}
