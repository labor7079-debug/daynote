package com.kangtaeyoung.daynote.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign

/**
 * 시/분 선택 — 드롭다운 목록에서 고르거나 숫자를 직접 입력. (시계 다이얼 대체)
 * 할 일 추가란(캘린더 상세)과 할 일 수정 다이얼로그가 공용.
 *
 * 컴팩트 박스: 화살표 아이콘 없이 숫자를 가운데 정렬해 좁은 폭(64dp)에서도 값이
 * 잘리지 않는다 — "종료 시각" 줄처럼 라벨+토글+시:분이 한 줄에 서는 곳 기준.
 * 탭하면 드롭다운이 열리고, 키보드 직접 입력도 그대로 된다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumberDropdown(
    label: String,
    value: Int,
    range: IntRange,
    onValue: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var text by remember(value) { mutableStateOf(value.toString().padStart(2, '0')) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { input ->
                val digits = input.filter { it.isDigit() }.take(2)
                text = digits
                digits.toIntOrNull()?.let { if (it in range) onValue(it) }
            },
            label = { Text(label) },
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            range.forEach { n ->
                DropdownMenuItem(
                    text = { Text(n.toString().padStart(2, '0')) },
                    onClick = {
                        onValue(n)
                        text = n.toString().padStart(2, '0')
                        expanded = false
                    },
                )
            }
        }
    }
}
