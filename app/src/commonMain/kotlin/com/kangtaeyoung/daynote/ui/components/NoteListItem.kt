package com.kangtaeyoung.daynote.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kangtaeyoung.daynote.core.toLocalDate
import com.kangtaeyoung.daynote.domain.model.Note
import kotlinx.datetime.LocalDate

/**
 * 메모 목록 한 칸. 제목 + 날짜(캘린더 날짜, 없으면 작성일) + 본문 미리보기(2줄).
 * 고정(핀)된 메모는 앞에 표시를 둔다.
 *
 * 제스처: 탭=[onClick](메모 열기), 날짜 라벨 탭=해당 일자 미니 캘린더 팝업,
 * **길게 누름**=이동/복사/삭제 메뉴([WithItemActions] — null 인 액션은 숨김).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteListItem(
    note: Note,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onMoveTo: ((LocalDate) -> Unit)? = null,
    onCopyTo: ((LocalDate) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val noteDate = (note.date ?: note.createdAt).toLocalDate()
    WithItemActions(
        calendarDate = noteDate,
        onMoveTo = onMoveTo,
        onCopyTo = onCopyTo,
        onDelete = onDelete,
    ) { openCalendar, openMenu ->
        Card(modifier = modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .combinedClickable(onClick = onClick, onLongClick = openMenu)
                    .padding(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.isPinned) {
                        Text(
                            text = "고정",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                    Text(
                        text = note.title.ifBlank { "(제목 없음)" },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    // 이 메모가 놓인 날짜 — 탭하면 해당 일자를 강조한 미니 캘린더 팝업.
                    Text(
                        text = "${noteDate.year}.${noteDate.monthNumber}.${noteDate.dayOfMonth}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clickable(onClick = openCalendar)
                            .padding(4.dp),
                    )
                }
                val preview = note.content.trim()
                if (preview.isNotEmpty()) {
                    Text(
                        text = preview,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}
