package com.kangtaeyoung.daynote

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.kangtaeyoung.daynote.data.repository.SettingsRepository
import com.kangtaeyoung.daynote.data.sync.AutoSyncCoordinator
import com.kangtaeyoung.daynote.domain.model.ThemeMode
import com.kangtaeyoung.daynote.ui.navigation.DayNoteNavHost
import com.kangtaeyoung.daynote.ui.theme.DayNoteTheme
import org.koin.compose.koinInject

/**
 * 공유 진입 컴포저블 — Android·Desktop 양쪽이 이 하나를 호출한다.
 *
 * Koin 은 진입부(Android `DayNoteApp` / Desktop `main`)에서 `initKoin()` 으로 이미 시작했고,
 * Koin 4.1 부터 `startKoin` 이 Compose 컨텍스트까지 연결하므로 `koinInject`/`viewModel` 이 바로 동작한다.
 * 화면 구성은 [DayNoteNavHost] 가 담당한다(홈=메모 목록, Phase 2에서 캘린더로 승격).
 */
@Composable
fun App() {
    // 자동 동기화 시작(앱 시작 1회 + 로컬 변경 디바운스). 스코프는 이 컴포지션(=UI 수명)에 묶인다.
    val autoSync = koinInject<AutoSyncCoordinator>()
    LaunchedEffect(Unit) { autoSync.start(this) }

    // 화면 테마(영속): SYSTEM=기기 설정, LIGHT/DARK=강제.
    val settings = koinInject<SettingsRepository>()
    val themeMode by settings.observeThemeMode().collectAsState(ThemeMode.SYSTEM)
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    DayNoteTheme(darkTheme = darkTheme) {
        DayNoteNavHost()
    }
}
