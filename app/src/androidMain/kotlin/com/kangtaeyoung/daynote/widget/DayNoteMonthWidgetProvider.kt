package com.kangtaeyoung.daynote.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.kangtaeyoung.daynote.MainActivity
import com.kangtaeyoung.daynote.R
import com.kangtaeyoung.daynote.core.monthGridDays
import com.kangtaeyoung.daynote.core.toLocalDate
import com.kangtaeyoung.daynote.core.toMillisRange
import com.kangtaeyoung.daynote.core.today
import com.kangtaeyoung.daynote.data.local.dao.NoteDao
import com.kangtaeyoung.daynote.data.local.dao.TaskDao
import com.kangtaeyoung.daynote.domain.holiday.KoreanHolidays
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import org.koin.core.context.GlobalContext

/**
 * 월 그리드 홈 위젯 — 좌측 미니 월 달력(오늘=슬레이트 마커, 일요일·공휴일=클레이,
 * 메모/할 일 있는 날=밑줄+굵게) + 우측 오늘의 메모·할 일(삼성 캘린더 위젯 컨셉).
 *
 * 우측 목록은 고정 슬롯이 아니라 **스크롤 ListView**([DayNoteWidgetService]) — 위젯 높이만큼
 * 보여주고 넘치면 스크롤한다. 갱신 경로는 [DayNoteWidgetProvider] 와 동일.
 */
class DayNoteMonthWidgetProvider : AppWidgetProvider() {

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

        /** 배치된 모든 월 그리드 위젯을 다시 그린다(앱 내 데이터 변경 시 호출). */
        fun updateAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, DayNoteMonthWidgetProvider::class.java))
            if (ids.isEmpty()) return
            CoroutineScope(Dispatchers.IO).launch {
                ids.forEach { render(context, mgr, it) }
            }
        }

        private suspend fun render(context: Context, mgr: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_month)
            val today = today()
            val gridDays = monthGridDays(today)

            views.setTextViewText(R.id.widget_month_title, "${today.year}년 ${today.monthNumber}월")
            views.setTextViewText(
                R.id.widget_month_date,
                "${today.monthNumber}월 ${today.dayOfMonth}일 (${koreanDow(today.dayOfWeek)})",
            )
            val holiday = KoreanHolidays.nameOf(today)
            if (holiday != null) {
                views.setViewVisibility(R.id.widget_month_holiday, View.VISIBLE)
                views.setTextViewText(R.id.widget_month_holiday, holiday)
            } else {
                views.setViewVisibility(R.id.widget_month_holiday, View.GONE)
            }

            // 그리드 범위의 메모·할 일 → 날짜별 유무(마커)에 사용(우측 목록은 별도 ListView 어댑터가 담당).
            val koin = GlobalContext.getOrNull()
            val (gridStart, gridEnd) = gridDays.toMillisRange()
            val notes = runCatching { koin?.get<NoteDao>()?.observeByDateRange(gridStart, gridEnd)?.first() }
                .getOrNull().orEmpty()
            val tasks = runCatching { koin?.get<TaskDao>()?.observeByDueDateRange(gridStart, gridEnd)?.first() }
                .getOrNull().orEmpty()
            val busyDays: Set<LocalDate> = buildSet {
                notes.forEach { n -> n.date?.let { add(it.toLocalDate()) } }
                tasks.forEach { t ->
                    val startMillis = t.dueDate ?: return@forEach
                    val start = startMillis.toLocalDate()
                    // 기간 할 일은 걸치는 모든 날짜를 표시(상한으로 폭주 방지).
                    val end = t.endDate?.toLocalDate()?.takeIf { it > start } ?: start
                    var d = start
                    var guard = 0
                    while (d <= end && guard < 62) {
                        add(d)
                        d = d.plus(1, DateTimeUnit.DAY)
                        guard++
                    }
                }
            }

            // 미니 월 그리드(6주 x 7일)를 코드에서 채운다.
            views.removeAllViews(R.id.widget_month_grid)
            gridDays.chunked(7).forEach { week ->
                val row = RemoteViews(context.packageName, R.layout.widget_month_row)
                week.forEach { day ->
                    val cell = RemoteViews(context.packageName, R.layout.widget_month_cell)
                    val inMonth = day.monthNumber == today.monthNumber && day.year == today.year
                    val isToday = day == today
                    val isRed = day.dayOfWeek == DayOfWeek.SUNDAY || KoreanHolidays.isHoliday(day)

                    // 항목 있는 날은 밑줄+굵게(스팬은 RemoteViews 에 그대로 전달된다).
                    val label: CharSequence = if (day in busyDays) {
                        SpannableString(day.dayOfMonth.toString()).apply {
                            setSpan(UnderlineSpan(), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                            setSpan(StyleSpan(Typeface.BOLD), 0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                    } else {
                        day.dayOfMonth.toString()
                    }
                    cell.setTextViewText(R.id.widget_month_day, label)
                    cell.setInt(
                        R.id.widget_month_day,
                        "setBackgroundResource",
                        if (isToday) R.drawable.widget_today_bg else 0,
                    )
                    cell.setTextColor(
                        R.id.widget_month_day,
                        ContextCompat.getColor(
                            context,
                            when {
                                isToday -> R.color.widgetOnToday
                                !inMonth -> R.color.widgetTextFaint
                                isRed -> R.color.widgetAccent
                                else -> R.color.widgetText
                            },
                        ),
                    )
                    row.addView(R.id.widget_month_row, cell)
                }
                views.addView(R.id.widget_month_grid, row)
            }

            // 우측 오늘 항목 → 스크롤 ListView(작은 위젯과 같은 서비스·같은 목록).
            val svc = Intent(context, DayNoteWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_month_list, svc)
            views.setEmptyView(R.id.widget_month_list, R.id.widget_month_empty)

            val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            views.setOnClickPendingIntent(R.id.widget_month_root, open)
            views.setOnClickPendingIntent(R.id.widget_month_add, open)
            views.setPendingIntentTemplate(R.id.widget_month_list, openTemplate(context))

            mgr.updateAppWidget(widgetId, views)
            mgr.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_month_list)
        }
    }
}
