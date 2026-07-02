package com.kangtaeyoung.daynote.notification

import com.kangtaeyoung.daynote.core.nowMillis
import com.kangtaeyoung.daynote.core.startOfDayMillis
import com.kangtaeyoung.daynote.core.today
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

    /**
     * 앞으로 올 알림을 (재)예약한다 — 시간 지정 할 일은 마감 시각 정시에,
     * **종일 할 일은 그 날 기본시각(오전 9시)에** 알린다(이미 지난 시각이면 건너뜀).
     */
    suspend fun reschedule() {
        if (!settings.isRemindersEnabled()) return
        val now = nowMillis()
        taskDao.getTimedUpcoming(now).forEach { task ->
            task.dueDate?.let { due -> scheduler.schedule(task.id, task.text, due) }
        }
        val startOfToday = today().startOfDayMillis()
        taskDao.getAllDayUpcoming(startOfToday).forEach { task ->
            val due = task.dueDate ?: return@forEach
            val trigger = due + ALL_DAY_REMINDER_OFFSET_MS // 자정 + 9시간
            if (trigger > now) scheduler.schedule(task.id, task.text, trigger)
        }
    }

    private companion object {
        const val DEBOUNCE_MS = 1_500L

        /** 종일 할 일의 기본 알림 시각 — 마감일 자정 기준 +9시간(오전 9시). */
        const val ALL_DAY_REMINDER_OFFSET_MS = 9 * 60 * 60 * 1_000L
    }
}
