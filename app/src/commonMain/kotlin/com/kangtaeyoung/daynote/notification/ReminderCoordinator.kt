package com.kangtaeyoung.daynote.notification

import com.kangtaeyoung.daynote.core.nowMillis
import com.kangtaeyoung.daynote.data.local.dao.TaskDao
import com.kangtaeyoung.daynote.data.repository.SettingsRepository
import com.kangtaeyoung.daynote.data.sync.LocalChangeNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/**
 * 시간 지정 할 일의 마감 알림을 예약한다. 두 시점에 "앞으로 올 알림"을 다시 건다:
 *  1) 앱 시작 시 1회, 2) 로컬 변경 시(디바운스).
 *
 * 같은 할 일은 같은 요청 코드로 교체되므로 시간 변경도 자연 반영된다. 완료·삭제된 할 일은
 * 재예약 대상에서 빠지고(기존 알람은 남을 수 있음), 알림 수신 측이 발화 시점에 유효성을 재확인한다.
 * (설계원칙 4: 레포지토리는 알림의 존재를 모르고 [LocalChangeNotifier] 신호만 발행한다.)
 */
class ReminderCoordinator(
    private val taskDao: TaskDao,
    private val settings: SettingsRepository,
    private val scheduler: TaskReminderScheduler,
    private val changes: LocalChangeNotifier,
) {
    @OptIn(FlowPreview::class)
    fun start(scope: CoroutineScope) {
        scope.launch { reschedule() }
        scope.launch {
            changes.changes
                .debounce(DEBOUNCE_MS)
                .collect { reschedule() }
        }
    }

    /** 앞으로 마감인 '시간 지정' 할 일들의 알림을 (재)예약한다. */
    suspend fun reschedule() {
        if (!settings.isRemindersEnabled()) return
        val now = nowMillis()
        taskDao.getTimedUpcoming(now).forEach { task ->
            task.dueDate?.let { due -> scheduler.schedule(task.id, task.text, due) }
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 1_500L
    }
}
