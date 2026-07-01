package com.kangtaeyoung.daynote.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** 재부팅 후 AlarmManager 예약이 초기화되므로, 앞으로 올 할 일 알림을 다시 예약한다. */
class BootReceiver : BroadcastReceiver(), KoinComponent {
    private val coordinator: ReminderCoordinator by inject()

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        runCatching { runBlocking { coordinator.reschedule() } }
        pending.finish()
    }
}
