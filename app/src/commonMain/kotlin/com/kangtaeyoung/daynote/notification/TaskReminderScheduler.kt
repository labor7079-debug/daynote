package com.kangtaeyoung.daynote.notification

/**
 * 할 일 마감 시각 알림 예약(플랫폼 의존 — Android=AlarmManager, Desktop=no-op).
 * 설계원칙 3·6: 본체는 이 인터페이스만 알고, 플랫폼 구현은 platformModule 이 주입한다.
 */
interface TaskReminderScheduler {
    /** [taskId] 할 일의 알림을 [triggerAtMillis] 에 예약(같은 id 는 교체). */
    fun schedule(taskId: String, text: String, triggerAtMillis: Long)

    /** 예약 취소. */
    fun cancel(taskId: String)
}
