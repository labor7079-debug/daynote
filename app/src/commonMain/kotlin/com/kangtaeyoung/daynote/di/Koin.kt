package com.kangtaeyoung.daynote.di

import com.kangtaeyoung.daynote.data.backup.BackupManager
import com.kangtaeyoung.daynote.data.local.AppDatabase
import com.kangtaeyoung.daynote.data.local.buildDatabase
import com.kangtaeyoung.daynote.data.remote.openai.OpenAiClient
import com.kangtaeyoung.daynote.data.repository.AiRepository
import com.kangtaeyoung.daynote.data.repository.AiRepositoryImpl
import com.kangtaeyoung.daynote.data.repository.ExternalEventRepository
import com.kangtaeyoung.daynote.data.repository.ExternalEventRepositoryImpl
import com.kangtaeyoung.daynote.data.repository.NoteRepository
import com.kangtaeyoung.daynote.data.repository.NoteRepositoryImpl
import com.kangtaeyoung.daynote.data.repository.SettingsRepository
import com.kangtaeyoung.daynote.data.repository.SettingsRepositoryImpl
import com.kangtaeyoung.daynote.data.repository.TaskRepository
import com.kangtaeyoung.daynote.data.repository.TaskRepositoryImpl
import com.kangtaeyoung.daynote.data.sync.AutoSyncCoordinator
import com.kangtaeyoung.daynote.data.sync.CloudSyncManager
import com.kangtaeyoung.daynote.data.sync.LocalChangeNotifier
import com.kangtaeyoung.daynote.data.sync.SupabaseCloudSyncManager
import com.kangtaeyoung.daynote.data.sync.supabase.SupabaseSyncClient
import com.kangtaeyoung.daynote.domain.usecase.AskAiUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveAiResultsUseCase
import com.kangtaeyoung.daynote.domain.usecase.RunAiActionUseCase
import com.kangtaeyoung.daynote.domain.usecase.SuggestTitleUseCase
import com.kangtaeyoung.daynote.domain.usecase.AddNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.AddTaskUseCase
import com.kangtaeyoung.daynote.domain.usecase.DeleteNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.DeleteTaskUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveGeneralTasksUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveNoteTasksUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveNotesByDateUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveNotesUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveExternalEventsByDateUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveTasksByDateUseCase
import com.kangtaeyoung.daynote.domain.usecase.FindRelatedNotesUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveUndatedNotesUseCase
import com.kangtaeyoung.daynote.domain.usecase.SearchNotesUseCase
import com.kangtaeyoung.daynote.domain.usecase.SetNotePinnedUseCase
import com.kangtaeyoung.daynote.domain.usecase.ToggleTaskUseCase
import com.kangtaeyoung.daynote.domain.usecase.UpdateNoteUseCase
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/**
 * 플랫폼별 의존성 모듈. Android/Desktop 의 `actual` 에서
 * `RoomDatabase.Builder<AppDatabase>`(플랫폼마다 파일 경로가 다름)를 등록한다.
 */
expect fun platformModule(): Module

/**
 * DB·DAO 그래프(공유). 빌더는 [platformModule] 이 제공하고, 여기서 드라이버를 꽂아 완성한다.
 * UI·도메인은 이 DAO 들만 주입받아 데이터 출처(로컬/원격)를 모른다(설계원칙 4).
 */
val databaseModule: Module = module {
    single { buildDatabase(get()) }
    single { get<AppDatabase>().noteDao() }
    single { get<AppDatabase>().taskDao() }
    single { get<AppDatabase>().settingDao() }
    single { get<AppDatabase>().aiResultDao() }
    single { get<AppDatabase>().externalEventDao() }
}

/** Repository 계층(설계원칙 4). DAO 를 주입받아 도메인 모델을 제공한다. */
val repositoryModule: Module = module {
    // 로컬 변경 신호(자동 동기화 트리거) — 레포지토리가 발행, 코디네이터가 구독.
    single { LocalChangeNotifier() }
    single<NoteRepository> { NoteRepositoryImpl(get(), get()) }
    single<TaskRepository> { TaskRepositoryImpl(get(), get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    // 구글 캘린더 외부 일정(공유 캘린더 표시 — 읽기 전용 캐시 조회)
    single<ExternalEventRepository> { ExternalEventRepositoryImpl(get()) }
    // 로컬 백업/복원(내보내기·가져오기) — DAO 직접 사용.
    single { BackupManager(get(), get()) }
    // AI(Phase 4-B): OpenAI 단일. ApiKeyProvider 는 platformModule 이 제공.
    single { OpenAiClient() }
    single<AiRepository> { AiRepositoryImpl(get(), get(), get()) }
    // 클라우드 동기화(Phase 6, Supabase) — Ktor 라 양 플랫폼 공유. SecureStore 는 platformModule 제공.
    single { SupabaseSyncClient() }
    single<CloudSyncManager> { SupabaseCloudSyncManager(get(), get(), get(), get(), get()) }
    // 자동 동기화 조율(앱 시작 + 변경 디바운스) — App() 에서 start.
    single { AutoSyncCoordinator(get(), get(), get()) }
    // 할 일 알림 조율(앱 시작 + 변경 디바운스로 재예약). TaskReminderScheduler 는 platformModule 제공.
    single { com.kangtaeyoung.daynote.notification.ReminderCoordinator(get(), get(), get(), get()) }
}

/** UseCase 계층. 상태 없는 얇은 래퍼라 factory 로 등록한다. */
val useCaseModule: Module = module {
    factory { ObserveNotesUseCase(get()) }
    factory { ObserveNoteUseCase(get()) }
    factory { ObserveNotesByDateUseCase(get()) }
    factory { ObserveUndatedNotesUseCase(get()) }
    factory { ObserveTasksByDateUseCase(get()) }
    factory { ObserveExternalEventsByDateUseCase(get()) }
    factory { SearchNotesUseCase(get()) }
    factory { FindRelatedNotesUseCase(get()) }
    factory { AddNoteUseCase(get()) }
    factory { UpdateNoteUseCase(get()) }
    factory { SetNotePinnedUseCase(get()) }
    factory { DeleteNoteUseCase(get()) }
    factory { ObserveNoteTasksUseCase(get()) }
    factory { ObserveGeneralTasksUseCase(get()) }
    factory { AddTaskUseCase(get()) }
    factory { com.kangtaeyoung.daynote.domain.usecase.UpdateTaskUseCase(get()) }
    factory { ToggleTaskUseCase(get()) }
    factory { DeleteTaskUseCase(get()) }
    factory { RunAiActionUseCase(get()) }
    factory { AskAiUseCase(get()) }
    factory { SuggestTitleUseCase(get()) }
    factory { ObserveAiResultsUseCase(get()) }
}

/** 앱 전체 모듈 묶음(플랫폼 모듈 제외 — 그건 initKoin 이 따로 붙인다). */
val appModules: List<Module> = listOf(databaseModule, repositoryModule, useCaseModule)

/**
 * Android·Desktop 공용 Koin 시작점. 플랫폼 진입부(Application / main)에서 한 번 호출한다.
 * [appDeclaration] 으로 플랫폼 고유 설정(예: Android `androidContext`)을 주입한다.
 */
fun initKoin(appDeclaration: KoinAppDeclaration = {}): KoinApplication = startKoin {
    appDeclaration()
    modules(platformModule())
    modules(appModules)
}
