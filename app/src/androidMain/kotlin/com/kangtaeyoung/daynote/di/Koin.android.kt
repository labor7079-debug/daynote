package com.kangtaeyoung.daynote.di

import com.kangtaeyoung.daynote.data.local.appDatabaseBuilder
import com.kangtaeyoung.daynote.data.sync.AndroidCalendarSyncManager
import com.kangtaeyoung.daynote.data.sync.CalendarSyncManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/** Android: Koin 의 `androidContext` 로 앱 전용 DB 경로 빌더 + 동기화 매니저를 등록한다. */
actual fun platformModule(): Module = module {
    single { appDatabaseBuilder(androidContext()) }
    single<CalendarSyncManager> { AndroidCalendarSyncManager(get(), get()) }
}
