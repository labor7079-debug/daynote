package com.kangtaeyoung.daynote.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.kangtaeyoung.daynote.core.toLocalDate
import com.kangtaeyoung.daynote.domain.model.Task
import com.kangtaeyoung.daynote.domain.model.scheduleLabel
import kotlinx.datetime.LocalDate

/**
 * 체크리스트 한 줄. 완료된 항목은 취소선 + 흐린 색으로 구분한다.
 * 상태를 갖지 않는 순수 표현 컴포저블 — 토글/삭제/수정은 콜백으로 위임한다.
 *
 * 제스처: 텍스트 탭=해당 일자 미니 캘린더 팝업, **길게 누름 또는 마우스 우클릭(PC)**=
 * 수정/이동/복사/삭제 메뉴([WithItemActions] — null 콜백 항목은 메뉴에서 숨김).
 * [onUpdate] 가 있으면 메뉴에 "수정"이 생기고 [EditTaskDialog] 로 내용·날짜·시각을 고친다.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskRow(
    task: Task,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onMoveTo: ((LocalDate) -> Unit)? = null,
    onCopyTo: ((LocalDate) -> Unit)? = null,
    onUpdate: ((Task) -> Unit)? = null,
) {
    var showEdit by remember { mutableStateOf(false) }

    WithItemActions(
        calendarDate = (task.dueDate ?: task.createdAt).toLocalDate(),
        onEdit = onUpdate?.let { { showEdit = true } },
        onMoveTo = onMoveTo,
        onCopyTo = onCopyTo,
        onDelete = onDelete,
    ) { openCalendar, openMenu ->
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Checkbox(checked = task.isDone, onCheckedChange = { onToggle() })
            scheduleLabel(task)?.let { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 6.dp),
                )
            }
            Text(
                text = task.text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (task.isDone) {
                    LocalContentColor.current.copy(alpha = 0.5f)
                } else {
                    LocalContentColor.current
                },
                textDecoration = if (task.isDone) TextDecoration.LineThrough else null,
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(onClick = openCalendar, onLongClick = openMenu)
                    .onRightClick(openMenu)
                    .padding(end = 4.dp, top = 6.dp, bottom = 6.dp),
            )
            Text(
                text = "✕",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onDelete).padding(8.dp),
            )
        }
    }

    if (showEdit && onUpdate != null) {
        EditTaskDialog(
            task = task,
            onSave = { updated ->
                onUpdate(updated)
                showEdit = false
            },
            onDismiss = { showEdit = false },
        )
    }
}

/**
 * 일정 라벨 — 공용 규칙([Task.scheduleLabel])에 위임한다. 캘린더 칩·홈 위젯과 표기가 항상 일치.
 */
internal fun scheduleLabel(task: Task): String? = task.scheduleLabel()
