package com.kangtaeyoung.daynote.ui.components

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.kangtaeyoung.daynote.core.toHourMinuteLabel
import com.kangtaeyoung.daynote.domain.model.Task

/**
 * 체크리스트 한 줄. 완료된 항목은 취소선 + 흐린 색으로 구분한다.
 * 상태를 갖지 않는 순수 표현 컴포저블 — 토글/삭제는 콜백으로 위임한다.
 */
@Composable
fun TaskRow(
    task: Task,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Checkbox(checked = task.isDone, onCheckedChange = { onToggle() })
        val due = task.dueDate
        if (!task.allDay && due != null) {
            Text(
                text = due.toHourMinuteLabel(),
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
            modifier = Modifier.weight(1f).padding(end = 4.dp),
        )
        Text(
            text = "✕",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clip(CircleShape).clickable(onClick = onDelete).padding(8.dp),
        )
    }
}
