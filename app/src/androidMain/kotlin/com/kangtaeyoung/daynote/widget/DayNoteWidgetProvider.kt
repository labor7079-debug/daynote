package com.kangtaeyoung.daynote.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.kangtaeyoung.daynote.MainActivity
import com.kangtaeyoung.daynote.R
import com.kangtaeyoung.daynote.core.today
import com.kangtaeyoung.daynote.domain.holiday.KoreanHolidays
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.isoDayNumber

/**
 * 홈 화면 위젯 — 오늘 날짜(+공휴일명)와 그날의 메모·할 일을 보여준다(삼성 캘린더 위젯 컨셉).
 *
 * - 목록: 고정 슬롯이 아니라 **스크롤 ListView**([DayNoteWidgetService]) — 위젯 높이만큼 항목을
 *   보여주고 넘치면 스크롤한다(공간 낭비 없음).
 * - 갱신: ① 시스템 주기(30분) ② 앱에서 메모/할 일이 바뀔 때([com.kangtaeyoung.daynote.DayNoteApp]
 *   이 [updateAll] 호출) ③ 시간/시간대 변경 브로드캐스트.
 * - 탭 → 앱 열기(캘린더 홈). 헤더·＋·목록 행 모두 동일.
 */
class DayNoteWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { render(context, appWidgetManager, it) }
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

        /** 배치된 모든 DayNote 위젯을 다시 그린다(앱 내 데이터 변경 시 호출). */
        fun updateAll(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, DayNoteWidgetProvider::class.java))
            ids.forEach { render(context, mgr, it) }
        }

        private fun render(context: Context, mgr: AppWidgetManager, widgetId: Int) {
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

            // 오늘 항목 목록 → 스크롤 ListView(RemoteViewsService). 위젯마다 고유 intent(appWidgetId).
            val svc = Intent(context, DayNoteWidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_list, svc)
            views.setEmptyView(R.id.widget_list, R.id.widget_empty)

            // 탭 → 앱 열기(캘린더 홈이 시작 화면). 헤더·＋ 는 직접, 목록 행은 템플릿+빈 fill-in.
            val open = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            views.setOnClickPendingIntent(R.id.widget_root, open)
            views.setOnClickPendingIntent(R.id.widget_add, open)
            views.setPendingIntentTemplate(R.id.widget_list, openTemplate(context))

            mgr.updateAppWidget(widgetId, views)
            // 목록 데이터 재조회(추가/삭제/체크 반영).
            mgr.notifyAppWidgetViewDataChanged(widgetId, R.id.widget_list)
        }
    }
}

/** 목록 행 탭용 템플릿(앱 열기). API 31+ 는 템플릿에 MUTABLE 필요. */
internal fun openTemplate(context: Context): PendingIntent {
    val mutable = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        PendingIntent.FLAG_MUTABLE
    } else {
        0
    }
    return PendingIntent.getActivity(
        context, 1, Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or mutable,
    )
}
