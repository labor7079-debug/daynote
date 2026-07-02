package com.kangtaeyoung.daynote

import android.app.Application
import com.kangtaeyoung.daynote.data.sync.LocalChangeNotifier
import com.kangtaeyoung.daynote.di.initKoin
import com.kangtaeyoung.daynote.widget.DayNoteWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext

/** Android 진입부 — 프로세스 시작 시 Koin 을 한 번 초기화하고, 홈 위젯 자동 갱신을 배선한다. */
class DayNoteApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        val koin = initKoin {
            androidContext(this@DayNoteApp)
        }.koin

        // 메모/할 일 변경 → 홈 위젯 즉시 갱신(디바운스로 연속 편집 묶음). 위젯 미배치면 no-op.
        val changes = koin.get<LocalChangeNotifier>()
        appScope.launch {
            changes.changes
                .debounce(WIDGET_REFRESH_DEBOUNCE_MS)
                .collect { DayNoteWidgetProvider.updateAll(this@DayNoteApp) }
        }
    }

    private companion object {
        const val WIDGET_REFRESH_DEBOUNCE_MS = 1_500L
    }
}
