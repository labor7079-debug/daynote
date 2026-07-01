package com.kangtaeyoung.daynote.data.sync

import com.kangtaeyoung.daynote.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/**
 * 자동 동기화 배선(Phase 6 후속). 두 시점에 [CloudSyncManager.syncNow] 를 조율한다:
 *  1) **앱 시작 시 1회** — 다른 기기에서 바뀐 내용을 즉시 받아온다.
 *  2) **로컬 변경 시(디바운스)** — 연속 편집을 한 번의 동기화로 묶어 올린다.
 *
 * 토글/설정/세션 게이팅은 모두 [CloudSyncManager.syncNow] 내부가 처리하므로 여기선 호출만 조율한다.
 * [start] 는 UI 수명에 묶인 스코프([App])에서 부르며, 스코프가 취소되면 두 코루틴도 함께 정리된다.
 */
class AutoSyncCoordinator(
    private val cloud: CloudSyncManager,
    private val settings: SettingsRepository,
    private val changes: LocalChangeNotifier,
) {

    @OptIn(FlowPreview::class)
    fun start(scope: CoroutineScope) {
        // 앱 시작: 상태를 정리한 뒤, 켜져 있으면 1회 동기화(꺼져 있으면 syncNow 가 알아서 무시).
        scope.launch {
            cloud.refreshState()
            if (settings.isCloudSyncEnabled()) cloud.syncNow()
        }
        // 로컬 변경: 디바운스로 연속 편집을 묶은 뒤 동기화.
        scope.launch {
            changes.changes
                .debounce(DEBOUNCE_MS)
                .collect {
                    if (settings.isCloudSyncEnabled()) cloud.syncNow()
                }
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 2_500L
    }
}
