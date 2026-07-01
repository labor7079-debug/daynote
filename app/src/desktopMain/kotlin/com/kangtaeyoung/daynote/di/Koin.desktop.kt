package com.kangtaeyoung.daynote.di

import com.kangtaeyoung.daynote.data.local.appDatabaseBuilder
import com.kangtaeyoung.daynote.data.security.ApiKeyProvider
import com.kangtaeyoung.daynote.data.security.DesktopApiKeyProvider
import com.kangtaeyoung.daynote.data.security.DesktopSecureStore
import com.kangtaeyoung.daynote.data.security.SecureStore
import com.kangtaeyoung.daynote.data.sync.CalendarSyncManager
import com.kangtaeyoung.daynote.data.sync.DesktopCalendarSyncManager
import com.kangtaeyoung.daynote.notification.DesktopTaskReminderScheduler
import com.kangtaeyoung.daynote.notification.TaskReminderScheduler
import org.koin.core.module.Module
import org.koin.dsl.module

/** Desktop(JVM): `~/.daynote` 경로 빌더 + (비활성) 동기화 매니저 + 키 저장소 + (no-op) 알림 예약기를 등록한다. */
actual fun platformModule(): Module = module {
    single { appDatabaseBuilder() }
    single<CalendarSyncManager> { DesktopCalendarSyncManager() }
    single<ApiKeyProvider> { DesktopApiKeyProvider() }
    single<SecureStore> { DesktopSecureStore() }
    single<TaskReminderScheduler> { DesktopTaskReminderScheduler() }
}
