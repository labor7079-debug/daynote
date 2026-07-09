package com.kangtaeyoung.daynote.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import androidx.core.content.ContextCompat
import com.kangtaeyoung.daynote.R
import com.kangtaeyoung.daynote.core.dayRange
import com.kangtaeyoung.daynote.core.today
import com.kangtaeyoung.daynote.data.local.dao.NoteDao
import com.kangtaeyoung.daynote.data.local.dao.TaskDao
import com.kangtaeyoung.daynote.data.repository.toDomain
import com.kangtaeyoung.daynote.domain.model.scheduleLabel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext

/**
 * 홈 위젯의 "오늘의 메모·할 일" 목록을 **스크롤 가능한 ListView** 로 제공한다.
 *
 * 예전처럼 고정 슬롯(2~6줄)으로 자르지 않고, 위젯 높이만큼 항목을 보여주고 넘치면 스크롤한다
 * — 위젯이 크면 큰 대로 많이 보인다(사용자 요청). 작은 위젯·월 그리드 위젯이 같은 목록을 공유한다.
 */
class DayNoteWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        DayNoteItemsFactory(applicationContext)
}

/** 위젯 목록 한 줄. [isTask] 면 옅은 박스 배경, [done] 이면 흐린 색(완료). */
private data class WidgetItem(val text: String, val isTask: Boolean, val done: Boolean)

private class DayNoteItemsFactory(
    private val context: Context,
) : RemoteViewsService.RemoteViewsFactory {

    // onDataSetChanged(바인더 스레드)에서 채우고 getViewAt 에서 읽는다.
    @Volatile
    private var items: List<WidgetItem> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        items = runCatching { loadTodayItems() }.getOrDefault(emptyList())
    }

    /** 오늘의 메모(먼저)·할 일(시각 병기)을 순서대로. Koin/DAO 는 앱 프로세스에서 이미 초기화됨. */
    private fun loadTodayItems(): List<WidgetItem> {
        val koin = GlobalContext.getOrNull() ?: return emptyList()
        val (start, end) = today().dayRange()
        return runBlocking {
            val notes = koin.get<NoteDao>().observeByDateRange(start, end).first()
            val tasks = koin.get<TaskDao>().observeByDueDateRange(start, end).first()
            buildList {
                notes.forEach { add(WidgetItem("· " + it.title.ifBlank { "(제목 없음)" }, isTask = false, done = false)) }
                tasks.forEach {
                    val head = it.text.lineSequence().firstOrNull()?.trim().orEmpty()
                    // 시각이 지정된 할 일은 시작(및 종료) 시각을 앞에 병기 — 앱 캘린더 칩과 동일 규칙.
                    val time = it.toDomain().scheduleLabel()?.let { s -> "$s " }.orEmpty()
                    add(WidgetItem((if (it.isDone) "✓ " else "☐ ") + time + head, isTask = true, done = it.isDone))
                }
            }
        }
    }

    override fun onDestroy() { items = emptyList() }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_list_item)
        val item = items.getOrNull(position) ?: return views
        views.setTextViewText(R.id.widget_item_text, item.text)
        views.setInt(
            R.id.widget_item_text,
            "setBackgroundResource",
            if (item.isTask) R.drawable.widget_task_bg else 0,
        )
        views.setTextColor(
            R.id.widget_item_text,
            ContextCompat.getColor(context, if (item.done) R.color.widgetTextDim else R.color.widgetText),
        )
        // 행 탭 → 앱 열기(템플릿은 provider 가 setPendingIntentTemplate 로 지정).
        views.setOnClickFillInIntent(R.id.widget_item_text, Intent())
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = false
}
