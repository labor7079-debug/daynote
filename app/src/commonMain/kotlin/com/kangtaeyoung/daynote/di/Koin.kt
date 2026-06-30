package com.kangtaeyoung.daynote.di

import com.kangtaeyoung.daynote.data.local.AppDatabase
import com.kangtaeyoung.daynote.data.local.buildDatabase
import com.kangtaeyoung.daynote.data.repository.NoteRepository
import com.kangtaeyoung.daynote.data.repository.NoteRepositoryImpl
import com.kangtaeyoung.daynote.data.repository.SettingsRepository
import com.kangtaeyoung.daynote.data.repository.SettingsRepositoryImpl
import com.kangtaeyoung.daynote.data.repository.TaskRepository
import com.kangtaeyoung.daynote.data.repository.TaskRepositoryImpl
import com.kangtaeyoung.daynote.domain.usecase.AddNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.AddTaskUseCase
import com.kangtaeyoung.daynote.domain.usecase.DeleteNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.DeleteTaskUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveGeneralTasksUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveNoteTasksUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveNotesByDateUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveNotesUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveTasksByDateUseCase
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
}

/** Repository 계층(설계원칙 4). DAO 를 주입받아 도메인 모델을 제공한다. */
val repositoryModule: Module = module {
    single<NoteRepository> { NoteRepositoryImpl(get()) }
    single<TaskRepository> { TaskRepositoryImpl(get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
}

/** UseCase 계층. 상태 없는 얇은 래퍼라 factory 로 등록한다. */
val useCaseModule: Module = module {
    factory { ObserveNotesUseCase(get()) }
    factory { ObserveNoteUseCase(get()) }
    factory { ObserveNotesByDateUseCase(get()) }
    factory { ObserveUndatedNotesUseCase(get()) }
    factory { ObserveTasksByDateUseCase(get()) }
    factory { SearchNotesUseCase(get()) }
    factory { AddNoteUseCase(get()) }
    factory { UpdateNoteUseCase(get()) }
    factory { SetNotePinnedUseCase(get()) }
    factory { DeleteNoteUseCase(get()) }
    factory { ObserveNoteTasksUseCase(get()) }
    factory { ObserveGeneralTasksUseCase(get()) }
    factory { AddTaskUseCase(get()) }
    factory { ToggleTaskUseCase(get()) }
    factory { DeleteTaskUseCase(get()) }
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
