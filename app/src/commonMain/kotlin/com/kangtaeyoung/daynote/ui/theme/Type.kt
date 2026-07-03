package com.kangtaeyoung.daynote.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.kangtaeyoung.daynote.resources.Res
import com.kangtaeyoung.daynote.resources.noto_serif_kr_bold
import com.kangtaeyoung.daynote.resources.noto_serif_kr_medium
import com.kangtaeyoung.daynote.resources.noto_serif_kr_regular
import org.jetbrains.compose.resources.Font

/**
 * 앱 전체 글씨체 — 초기 「Warm Journal」 디자인의 세리프(명조)로 전부 통일.
 * Noto Serif KR(OFL) 3종을 내장해 Android·PC 어디서나 동일하게 렌더링된다.
 * (시스템 세리프에 기대면 Windows 는 한글 세리프가 없어 맑은 고딕으로 폴백된다.)
 */
@Composable
fun dayNoteFontFamily(): FontFamily = FontFamily(
    Font(Res.font.noto_serif_kr_regular, weight = FontWeight.Normal),
    Font(Res.font.noto_serif_kr_medium, weight = FontWeight.Medium),
    Font(Res.font.noto_serif_kr_bold, weight = FontWeight.Bold),
)

/** Material3 타입 스케일 전체에 세리프체를 입힌다(크기·행간·자간은 기본값 유지). */
@Composable
fun dayNoteTypography(): Typography {
    val serif = dayNoteFontFamily()
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = serif),
        displayMedium = base.displayMedium.copy(fontFamily = serif),
        displaySmall = base.displaySmall.copy(fontFamily = serif),
        headlineLarge = base.headlineLarge.copy(fontFamily = serif),
        headlineMedium = base.headlineMedium.copy(fontFamily = serif),
        headlineSmall = base.headlineSmall.copy(fontFamily = serif),
        titleLarge = base.titleLarge.copy(fontFamily = serif),
        titleMedium = base.titleMedium.copy(fontFamily = serif),
        titleSmall = base.titleSmall.copy(fontFamily = serif),
        bodyLarge = base.bodyLarge.copy(fontFamily = serif),
        bodyMedium = base.bodyMedium.copy(fontFamily = serif),
        bodySmall = base.bodySmall.copy(fontFamily = serif),
        labelLarge = base.labelLarge.copy(fontFamily = serif),
        labelMedium = base.labelMedium.copy(fontFamily = serif),
        labelSmall = base.labelSmall.copy(fontFamily = serif),
    )
}
