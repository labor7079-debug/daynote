package com.kangtaeyoung.daynote.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.kangtaeyoung.daynote.data.sync.CalendarSyncManager
import com.kangtaeyoung.daynote.data.sync.DesktopCalendarSyncManager
import org.koin.compose.koinInject

/**
 * Desktop: 시스템 브라우저 PKCE 동의 플로우를 시작한다([DesktopCalendarSyncManager.startSignIn]).
 * 동의 후 브라우저가 로컬 콜백으로 돌아오면 매니저가 토큰을 저장하고 상태를 갱신한다.
 */
@Composable
actual fun rememberGoogleCalendarSignIn(): () -> Unit {
    val manager = koinInject<CalendarSyncManager>() as? DesktopCalendarSyncManager
    return remember(manager) { { manager?.startSignIn() } }
}
