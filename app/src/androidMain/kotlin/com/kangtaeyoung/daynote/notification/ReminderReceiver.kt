package com.kangtaeyoung.daynote.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kangtaeyoung.daynote.MainActivity
import com.kangtaeyoung.daynote.R
import com.kangtaeyoung.daynote.data.local.dao.TaskDao
import com.kangtaeyoung.daynote.data.repository.SettingsRepository
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * 예약된 시각에 발화 → 할 일이 여전히 유효하면 알림을 띄운다.
 * 완료·삭제된 할 일의 (미취소) 알람은 여기서 걸러진다(발화 시점 재검증).
 */
class ReminderReceiver : BroadcastReceiver(), KoinComponent {
    private val taskDao: TaskDao by inject()
    private val settings: SettingsRepository by inject()

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
        val fallbackText = intent.getStringExtra(EXTRA_TEXT).orEmpty()

        val task = runBlocking {
            if (!settings.isRemindersEnabled()) return@runBlocking null
            taskDao.getByIdRaw(taskId)
        } ?: return

        if (task.deletedAt != null || task.isDone) return

        showNotification(context, taskId, task.text.ifBlank { fallbackText })
    }

    private fun showNotification(context: Context, taskId: String, text: String) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "할 일 알림", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "마감 시각에 할 일을 알려줍니다."
            },
        )
        val open = PendingIntent.getActivity(
            context,
            taskId.hashCode(),
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle("할 일")
            .setContentText(text.ifBlank { "예정된 할 일이 있어요." })
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        // 알림 권한(POST_NOTIFICATIONS, API 33+)이 없으면 조용히 실패.
        runCatching { nm.notify(taskId.hashCode(), notif) }
    }

    companion object {
        const val ACTION_FIRE = "com.kangtaeyoung.daynote.REMINDER_FIRE"
        const val EXTRA_TASK_ID = "taskId"
        const val EXTRA_TEXT = "text"
        const val CHANNEL_ID = "task_reminders"
    }
}
