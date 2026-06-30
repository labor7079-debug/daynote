package com.kangtaeyoung.daynote

import androidx.compose.runtime.Composable
import com.kangtaeyoung.daynote.ui.navigation.DayNoteNavHost
import com.kangtaeyoung.daynote.ui.theme.DayNoteTheme

/**
 * 공유 진입 컴포저블 — Android·Desktop 양쪽이 이 하나를 호출한다.
 *
 * Koin 은 진입부(Android `DayNoteApp` / Desktop `main`)에서 `initKoin()` 으로 이미 시작했고,
 * Koin 4.1 부터 `startKoin` 이 Compose 컨텍스트까지 연결하므로 `koinInject`/`viewModel` 이 바로 동작한다.
 * 화면 구성은 [DayNoteNavHost] 가 담당한다(홈=메모 목록, Phase 2에서 캘린더로 승격).
 */
@Composable
fun App() {
    DayNoteTheme {
        DayNoteNavHost()
    }
}
