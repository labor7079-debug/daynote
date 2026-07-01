package com.kangtaeyoung.daynote.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Android: AlarmManager 로 마감 시각에 [ReminderReceiver] 를 깨운다.
 * 같은 할 일은 같은 요청 코드(id 해시)로 교체되므로 시간 변경도 자연 반영된다.
 * 정확 알람이 허용되면 exact, 아니면 while-idle 근사 예약으로 폴백한다.
 */
class AndroidTaskReminderScheduler(private val context: Context) : TaskReminderScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun schedule(taskId: String, text: String, triggerAtMillis: Long) {
        val pi = pendingIntent(taskId, text)
        val canExact = if (Build.VERSION.SDK_INT >= 31) alarmManager.canScheduleExactAlarms() else true
        runCatching {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
        }
    }

    override fun cancel(taskId: String) {
        alarmManager.cancel(pendingIntent(taskId, ""))
    }

    private fun pendingIntent(taskId: String, text: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_FIRE
            putExtra(ReminderReceiver.EXTRA_TASK_ID, taskId)
            putExtra(ReminderReceiver.EXTRA_TEXT, text)
        }
        return PendingIntent.getBroadcast(
            context,
            taskId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
