package com.kangtaeyoung.daynote.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kangtaeyoung.daynote.data.repository.SettingsRepository
import com.kangtaeyoung.daynote.data.sync.CalendarSyncManager
import com.kangtaeyoung.daynote.data.sync.CloudSyncManager
import com.kangtaeyoung.daynote.data.sync.CloudSyncState
import com.kangtaeyoung.daynote.ui.theme.SyncArrowsIcon
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * 우측 하단 상시 동기화 버튼(동그란 화살표 FAB) — 탭 화면(캘린더·메모·To-Do)의 Scaffold
 * `floatingActionButton` 슬롯에 꽂는다. 누르면 당겨서-새로고침과 동일하게
 * 클라우드(Supabase) syncNow + (토글 ON 이면) 구글 캘린더 push 를 실행하고 결과를 스낵바로 알린다.
 * 진행 중엔 스피너로 바뀌고 중복 실행은 무시한다.
 */
@Composable
fun SyncFab(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val cloudSync = koinInject<CloudSyncManager>()
    val calendarSync = koinInject<CalendarSyncManager>()
    val settings = koinInject<SettingsRepository>()
    val scope = rememberCoroutineScope()
    var syncing by remember { mutableStateOf(false) }

    FloatingActionButton(
        onClick = {
            if (!syncing) {
                syncing = true
                scope.launch {
                    cloudSync.syncNow()
                    if (calendarSync.isAvailable && settings.observeSyncEnabled().first()) {
                        calendarSync.syncNow()
                    }
                    syncing = false
                    cloudSyncResultMessage(cloudSync.state.value)?.let { snackbarHostState.showSnackbar(it) }
                }
            }
        },
        shape = CircleShape,
        modifier = modifier,
    ) {
        if (syncing) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        } else {
            Icon(imageVector = SyncArrowsIcon, contentDescription = "동기화")
        }
    }
}

/** 동기화 결과 → 사용자 안내 문구(당겨서 새로고침과 공용). null 이면 조용히 넘어간다. */
fun cloudSyncResultMessage(state: CloudSyncState): String? = when (state) {
    is CloudSyncState.Synced -> "동기화 완료 ✓"
    is CloudSyncState.Error -> "동기화 실패: ${state.message.lineSequence().first()}"
    CloudSyncState.Disabled -> "클라우드 동기화가 꺼져 있어요. 설정에서 켤 수 있어요."
    CloudSyncState.NeedsConfig -> "동기화 설정(설정 화면)이 필요해요."
    CloudSyncState.SignedOut -> "동기화 로그인이 필요해요(설정 화면)."
    else -> null
}
