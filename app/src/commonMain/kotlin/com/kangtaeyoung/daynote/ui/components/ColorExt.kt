package com.kangtaeyoung.daynote.ui.components

import androidx.compose.ui.graphics.Color

/**
 * 구글 캘린더가 주는 "#RRGGBB" 색 문자열 → Compose [Color]. 형식이 다르면 null.
 * (설정의 캘린더 체크 목록·달력의 외부 일정 점이 공용.)
 */
fun parseHexColor(hex: String?): Color? {
    val h = hex?.trim()?.removePrefix("#") ?: return null
    if (h.length != 6) return null
    val rgb = h.toLongOrNull(16) ?: return null
    return Color(0xFF000000L or rgb)
}
