package com.kangtaeyoung.daynote

import android.app.Application
import com.kangtaeyoung.daynote.di.initKoin
import org.koin.android.ext.koin.androidContext

/** Android 진입부 — 프로세스 시작 시 Koin 을 한 번 초기화한다. */
class DayNoteApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@DayNoteApp)
        }
    }
}
