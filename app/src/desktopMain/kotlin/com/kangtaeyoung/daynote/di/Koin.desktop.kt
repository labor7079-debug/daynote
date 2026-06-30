package com.kangtaeyoung.daynote.di

import com.kangtaeyoung.daynote.data.local.appDatabaseBuilder
import com.kangtaeyoung.daynote.data.sync.CalendarSyncManager
import com.kangtaeyoung.daynote.data.sync.DesktopCalendarSyncManager
import org.koin.core.module.Module
import org.koin.dsl.module

/** Desktop(JVM): `~/.daynote` 경로 빌더 + (비활성) 동기화 매니저를 등록한다. */
actual fun platformModule(): Module = module {
    single { appDatabaseBuilder() }
    single<CalendarSyncManager> { DesktopCalendarSyncManager() }
}
