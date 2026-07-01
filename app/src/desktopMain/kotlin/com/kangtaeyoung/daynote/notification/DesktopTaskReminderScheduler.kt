package com.kangtaeyoung.daynote.notification

/**
 * Desktop: 예약 알림 미지원(no-op). 데스크톱 앱은 상시 백그라운드 실행이 아니라
 * OS 예약 알림의 의미가 약하므로 비워 둔다(설계원칙 3 — 플랫폼 특화는 격리).
 */
class DesktopTaskReminderScheduler : TaskReminderScheduler {
    override fun schedule(taskId: String, text: String, triggerAtMillis: Long) {}
    override fun cancel(taskId: String) {}
}
