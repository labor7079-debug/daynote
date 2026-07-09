package com.kangtaeyoung.daynote.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.kangtaeyoung.daynote.MainActivity
import com.kangtaeyoung.daynote.R
import com.kangtaeyoung.daynote.core.dayRange
import com.kangtaeyoung.daynote.core.today
import com.kangtaeyoung.daynote.data.local.dao.NoteDao
import com.kangtaeyoung.daynote.data.local.dao.TaskDao
import com.kangtaeyoung.daynote.data.repository.toDomain
import com.kangtaeyoung.daynote.domain.holiday.KoreanHolidays
import com.kangtaeyoung.daynote.domain.model.scheduleLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber
import org.koin.core.context.GlobalContext

/**
 * 홈 화면 위젯 — 오늘 날짜(+공휴일명)와 그날의 메모·할 일을 보여준다(삼성 캘린더 위젯 컨셉).
 *
 * - 갱신: ① 시스템 주기(30분, `updatePeriodMillis`) ② 앱에서 메모/할 일이 바뀔 때
 *   ([com.kangtaeyoung.daynote.DayNoteApp] 이 LocalChangeNotifier 를 구독해 [updateAll] 호출)
 *   ③ 시간/시간대 변경 브로드캐스트.
 * - 데이터: Koin(GlobalContext)의 DAO 를 직접 읽는다(위젯은 앱 프로세스에서 실행되고
 *   Application.onCreate 가 Koin 을 먼저 초기화한다).
 * - 탭 → 앱 열기(캘린더 홈). ＋ 버튼도 동일(캘린더 홈에서 바로 추가).
 */
class DayNoteWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                appWidgetIds.forEach { render(context, appWidgetManager, it) }
            } finally {
                pending.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> updateAll(context)
        }
    }

    companion object {

        private val dowLabels = listOf("월", "화", "수", "목", "금", "토", "일")
        private fun koreanDow(dow: DayOfWeek): String = dowLabels[dow.isoDayNumber - 1]

        private val lineIds = intArrayOf(
            R.id.widget_line1, R.id.widget_line2, R.id.widget_line3,
            R.id.widget_line4, R.id.widget_line5, R.id.widget_line6,
        )

        /** 배치된 모든 DayNote 위젯을 다시 그린다(앱 내 데이터 변경 시 호출). */
        fun updateAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, DayNoteWidgetProvider::class.java))
            if (ids.isEmpty()) return
            CoroutineScope(Dispatchers.IO).launch {
                ids.forEach { render(context, mgr, it) }
            }
        }

        private suspend fun render(context: Context, mgr: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_daynote)
            val today = today()

            views.setTextViewText(
                R.id.widget_date,
                "${today.monthNumber}월 ${today.dayOfMonth}일 (${koreanDow(today.dayOfWeek)})",
            )
            val holiday = KoreanHolidays.nameOf(today)
            if (holiday != null) {
                views.setViewVisibility(R.id.widget_holiday, View.VISIBLE)
                views.setTextViewText(R.id.widget_holiday, holiday)
                views.setTextColor(R.id.widget_date, ContextCompat.getColor(context, R.color.widgetAccent))
            } else {
                views.setViewVisibility(R.id.widget_holiday, View.GONE)
                views.setTextColor(R.id.widget_date, ContextCompat.getColor(context, R.color.widgetText))
            }

            // 오늘의 메모·할 일 — Koin 이 아직 없으면(이론상 프로세스 초기화 직후) 빈 목록으로 그린다.
            val koin = GlobalContext.getOrNull()
            val (start, end) = today.dayRange()
            val notes = runCatching { koin?.get<NoteDao>()?.observeByDateRange(start, end)?.first() }
                .getOrNull().orEmpty()
            val tasks = runCatching { koin?.get<TaskDao>()?.observeByDueDateRange(start, end)?.first() }
                .getOrNull().orEmpty()

            data class Line(val text: String, val isTask: Boolean, val done: Boolean)

            val lines = buildList {
                notes.take(3).forEach { add(Line("· " + it.title.ifBlank { "(제목 없음)" }, isTask = false, done = false)) }
                tasks.take(3).forEach {
                    val head = it.text.lineSequence().firstOrNull()?.trim().orEmpty()
                    // 시각이 지정된 할 일은 시작(및 종료) 시각을 앞에 병기 — 앱 캘린더 칩과 동일 규칙.
                    val time = it.toDomain().scheduleLabel()?.let { s -> "$s " }.orEmpty()
                    add(Line((if (it.isDone) "✓ " else "☐ ") + time + head, isTask = true, done = it.isDone))
                }
            }

            lineIds.forEachIndexed { i, id ->
                if (i < lines.size) {
                    val line = lines[i]
                    views.setViewVisibility(id, View.VISIBLE)
                    views.setTextViewText(id, line.text)
                    // 할 일은 옅은 박스(앱 달력 칸의 TaskLineChip 과 같은 컨셉), 메모는 맨글자.
                    views.setInt(id, "setBackgroundResource", if (line.isTask) R.drawable.widget_task_bg else 0)
                    views.setTextColor(
                        id,
                        ContextCompat.getColor(context, if (line.done) R.color.widgetTextDim else R.color.widgetText),
                    )
                } else {
                    views.setViewVisibility(id, View.GONE)
                }
            }

            val remaining = (notes.size - 3).coerceAtLeast(0) + (tasks.size - 3).coerceAtLeast(0)
            if (remaining > 0) {
                views.setViewVisibility(R.id.widget_more, View.VISIBLE)
                views.setTextViewText(R.id.widget_more, "+${remaining}개 더")
            } else {
                views.setViewVisibility(R.id.widget_more, View.GONE)
            }
            views.setViewVisibility(R.id.widget_empty, if (lines.isEmpty()) View.VISIBLE else View.GONE)

            // 탭 → 앱 열기(캘린더 홈이 시작 화면이라 오늘이 바로 보인다). ＋ 도 동일.
            val open = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            views.setOnClickPendingIntent(R.id.widget_root, open)
            views.setOnClickPendingIntent(R.id.widget_add, open)

            mgr.updateAppWidget(widgetId, views)
        }
    }
}
