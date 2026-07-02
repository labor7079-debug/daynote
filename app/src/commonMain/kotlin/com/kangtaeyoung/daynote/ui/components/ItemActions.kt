package com.kangtaeyoung.daynote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kangtaeyoung.daynote.core.firstOfMonthPlusMonths
import com.kangtaeyoung.daynote.core.monthGridDays
import com.kangtaeyoung.daynote.core.today
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber

/**
 * 목록 항목(메모·할 일)의 공용 액션 —
 * 탭: 해당 일자를 강조한 **미니 캘린더 팝업**([MiniCalendarDialog] 보기 모드),
 * 길게 누름: **이동/복사/삭제** 드롭다운 메뉴(이동·복사는 미니 캘린더에서 날짜 선택).
 */

private val dowLabels = listOf("월", "화", "수", "목", "금", "토", "일")
private fun koreanDow(dow: DayOfWeek): String = dowLabels[dow.isoDayNumber - 1]
private fun LocalDate.fullLabel(): String = "${year}년 ${monthNumber}월 ${dayOfMonth}일 (${koreanDow(dayOfWeek)})"

private enum class ActionDialog { NONE, VIEW, PICK_MOVE, PICK_COPY }

/**
 * 항목을 감싸 메뉴·팝업 상태를 들고 있는 호스트. [content] 는 두 콜백을 받아
 * 원하는 제스처(탭=openCalendar, 길게 누름=openMenu)에 연결한다.
 * 콜백이 null 인 액션은 메뉴에 나타나지 않는다(화면별로 지원 범위가 다름).
 */
@Composable
fun WithItemActions(
    calendarDate: LocalDate,
    onMoveTo: ((LocalDate) -> Unit)? = null,
    onCopyTo: ((LocalDate) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    content: @Composable (openCalendar: () -> Unit, openMenu: () -> Unit) -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf(ActionDialog.NONE) }

    Box {
        content({ dialog = ActionDialog.VIEW }, { menuOpen = true })
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("캘린더 보기") },
                onClick = { menuOpen = false; dialog = ActionDialog.VIEW },
            )
            if (onMoveTo != null) {
                DropdownMenuItem(
                    text = { Text("다른 날짜로 이동") },
                    onClick = { menuOpen = false; dialog = ActionDialog.PICK_MOVE },
                )
            }
            if (onCopyTo != null) {
                DropdownMenuItem(
                    text = { Text("다른 날짜로 복사") },
                    onClick = { menuOpen = false; dialog = ActionDialog.PICK_COPY },
                )
            }
            if (onDelete != null) {
                DropdownMenuItem(
                    text = { Text("삭제", color = MaterialTheme.colorScheme.tertiary) },
                    onClick = { menuOpen = false; onDelete() },
                )
            }
        }
    }

    when (dialog) {
        ActionDialog.NONE -> Unit
        ActionDialog.VIEW -> MiniCalendarDialog(
            initial = calendarDate,
            title = calendarDate.fullLabel(),
            pickMode = false,
            onDismiss = { dialog = ActionDialog.NONE },
        )
        ActionDialog.PICK_MOVE -> MiniCalendarDialog(
            initial = calendarDate,
            title = "이동할 날짜 선택",
            pickMode = true,
            onPick = { picked -> onMoveTo?.invoke(picked); dialog = ActionDialog.NONE },
            onDismiss = { dialog = ActionDialog.NONE },
        )
        ActionDialog.PICK_COPY -> MiniCalendarDialog(
            initial = calendarDate,
            title = "복사할 날짜 선택",
            pickMode = true,
            onPick = { picked -> onCopyTo?.invoke(picked); dialog = ActionDialog.NONE },
            onDismiss = { dialog = ActionDialog.NONE },
        )
    }
}

/**
 * 미니 월 캘린더 팝업. [pickMode]=false 면 [initial] 을 강조해 "이 항목이 어느 날에 있는지" 보여주고,
 * true 면 날짜를 탭해 [onPick] 으로 돌려준다(이동/복사 대상 선택). ‹ › 로 달을 넘긴다.
 */
@Composable
fun MiniCalendarDialog(
    initial: LocalDate,
    title: String,
    pickMode: Boolean,
    onPick: (LocalDate) -> Unit = {},
    onDismiss: () -> Unit,
) {
    var anchor by remember { mutableStateOf(initial) }
    val today = today()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Medium,
                )
                if (pickMode) {
                    Text(
                        "날짜를 탭하세요.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "${anchor.year}년 ${anchor.monthNumber}월",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row {
                        TextButton(onClick = { anchor = anchor.firstOfMonthPlusMonths(-1) }) { Text("‹") }
                        TextButton(onClick = { anchor = anchor.firstOfMonthPlusMonths(1) }) { Text("›") }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth()) {
                    dowLabels.forEachIndexed { i, d ->
                        Text(
                            text = d,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (i == 6) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f).padding(2.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )
                    }
                }

                monthGridDays(anchor).chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth().height(38.dp)) {
                        week.forEach { day ->
                            val inMonth = day.monthNumber == anchor.monthNumber && day.year == anchor.year
                            val isTarget = day == initial
                            MiniDayCell(
                                day = day,
                                inMonth = inMonth,
                                isTarget = isTarget,
                                isToday = day == today,
                                onClick = if (pickMode) ({ onPick(day) }) else null,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("닫기") }
                }
            }
        }
    }
}

@Composable
private fun MiniDayCell(
    day: LocalDate,
    inMonth: Boolean,
    isTarget: Boolean,
    isToday: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .then(
                    when {
                        isTarget -> Modifier.background(MaterialTheme.colorScheme.primary)
                        isToday -> Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        else -> Modifier
                    },
                )
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = day.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    isTarget -> MaterialTheme.colorScheme.onPrimary
                    !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    day.dayOfWeek == DayOfWeek.SUNDAY -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}
