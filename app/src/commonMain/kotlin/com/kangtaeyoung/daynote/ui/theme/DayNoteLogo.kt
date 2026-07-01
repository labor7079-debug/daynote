package com.kangtaeyoung.daynote.ui.theme

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * DayNote 「리본 저널」 로고 — 노트 페이지 + 슬레이트 텍스트 선 + 클레이 북마크.
 * Android 적응형 아이콘(`res/drawable/ic_launcher_foreground.xml`)과 같은 형상.
 * 데스크톱 창 아이콘 등 공용. 108x108 뷰포트, Quiet Cadence 팔레트([Color.kt]).
 */
val DayNoteLogo: ImageVector by lazy {
    ImageVector.Builder(
        name = "DayNoteLogo",
        defaultWidth = 108.dp,
        defaultHeight = 108.dp,
        viewportWidth = 108f,
        viewportHeight = 108f,
    ).apply {
        // 배경 — 웜 본 라운드 스퀘어
        path(fill = SolidColor(Paper)) { roundRect(4f, 4f, 104f, 104f, 24f) }
        // 노트 페이지(살짝 밝은 본 + 옅은 헤어라인)
        path(fill = SolidColor(PaperHi), stroke = SolidColor(FaintLine), strokeLineWidth = 1.2f) {
            roundRect(34f, 30f, 74f, 82f, 6f)
        }
        // 본문을 암시하는 슬레이트 선 3줄(아래로 옅게)
        path(stroke = SolidColor(Slate), strokeLineWidth = 2.6f, strokeLineCap = StrokeCap.Round) {
            moveTo(42f, 53f); lineTo(66f, 53f)
        }
        path(stroke = SolidColor(Slate), strokeAlpha = 0.55f, strokeLineWidth = 2.6f, strokeLineCap = StrokeCap.Round) {
            moveTo(42f, 62f); lineTo(66f, 62f)
        }
        path(stroke = SolidColor(Slate), strokeAlpha = 0.32f, strokeLineWidth = 2.6f, strokeLineCap = StrokeCap.Round) {
            moveTo(42f, 71f); lineTo(58f, 71f)
        }
        // 클레이 북마크 리본(절제된 강조)
        path(fill = SolidColor(Clay)) {
            moveTo(52f, 27f); lineTo(64f, 27f); lineTo(64f, 48f); lineTo(58f, 43f); lineTo(52f, 48f); close()
        }
    }.build()
}

/** 라운드 사각형을 path 로 그린다(l,t,r,b + 반경). */
private fun PathBuilder.roundRect(l: Float, t: Float, r: Float, b: Float, rad: Float) {
    moveTo(l + rad, t)
    lineTo(r - rad, t)
    arcToRelative(rad, rad, 0f, false, true, rad, rad)
    lineTo(r, b - rad)
    arcToRelative(rad, rad, 0f, false, true, -rad, rad)
    lineTo(l + rad, b)
    arcToRelative(rad, rad, 0f, false, true, -rad, -rad)
    lineTo(l, t + rad)
    arcToRelative(rad, rad, 0f, false, true, rad, -rad)
    close()
}
