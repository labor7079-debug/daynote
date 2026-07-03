package com.kangtaeyoung.daynote.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * 앱 공용 벡터 아이콘 — material-icons 의존성을 추가하지 않고 직접 정의(설계원칙: 새 의존성 0).
 * 색은 사용처의 `Icon(tint=…)` 이 덮으므로 여기서는 형상만 정의한다.
 */

/** 설정(톱니바퀴). 24x24 표준 경로. */
val SettingsGearIcon: ImageVector by lazy {
    val path = "M19.14,12.94c0.04,-0.3,0.06,-0.61,0.06,-0.94c0,-0.32,-0.02,-0.64,-0.07,-0.94l2.03,-1.58" +
        "c0.18,-0.14,0.23,-0.41,0.12,-0.61l-1.92,-3.32c-0.12,-0.22,-0.37,-0.29,-0.59,-0.22l-2.39,0.96" +
        "c-0.5,-0.38,-1.03,-0.7,-1.62,-0.94L14.4,2.81c-0.04,-0.24,-0.24,-0.41,-0.48,-0.41h-3.84" +
        "c-0.24,0,-0.43,0.17,-0.47,0.41L9.25,5.35C8.66,5.59,8.12,5.92,7.63,6.29L5.24,5.33" +
        "c-0.22,-0.08,-0.47,0,-0.59,0.22L2.74,8.87C2.62,9.08,2.66,9.34,2.86,9.48l2.03,1.58" +
        "C4.84,11.36,4.8,11.69,4.8,12s0.02,0.64,0.07,0.94l-2.03,1.58c-0.18,0.14,-0.23,0.41,-0.12,0.61" +
        "l1.92,3.32c0.12,0.22,0.37,0.29,0.59,0.22l2.39,-0.96c0.5,0.38,1.03,0.7,1.62,0.94l0.36,2.54" +
        "c0.05,0.24,0.24,0.41,0.48,0.41h3.84c0.24,0,0.44,-0.17,0.47,-0.41l0.36,-2.54" +
        "c0.59,-0.24,1.13,-0.56,1.62,-0.94l2.39,0.96c0.22,0.08,0.47,0,0.59,-0.22l1.92,-3.32" +
        "c0.12,-0.22,0.07,-0.47,-0.12,-0.61L19.14,12.94zM12,15.6c-1.98,0,-3.6,-1.62,-3.6,-3.6" +
        "s1.62,-3.6,3.6,-3.6s3.6,1.62,3.6,3.6S13.98,15.6,12,15.6z"
    ImageVector.Builder(
        name = "SettingsGear",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = PathParser().parsePathString(path).toNodes(),
        fill = SolidColor(Color.Black),
    ).build()
}

/** 공용 24x24 단일 경로 아이콘 빌더 — 아래 아이콘들이 재사용한다. */
private fun materialIcon(name: String, path: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = PathParser().parsePathString(path).toNodes(),
        fill = SolidColor(Color.Black),
    ).build()

/** 뒤로(←). 상단바 내비게이션용 — "뒤로" 텍스트 버튼 대체. */
val BackArrowIcon: ImageVector by lazy {
    materialIcon("BackArrow", "M20,11H7.83l5.59,-5.59L12,4l-8,8l8,8l1.41,-1.41L7.83,13H20v-2z")
}

/** 추가(+). "추가"/"+ 추가" 텍스트 버튼 대체. */
val AddPlusIcon: ImageVector by lazy {
    materialIcon("AddPlus", "M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z")
}

/** 삭제(휴지통). 에디터 상단바 "삭제" 텍스트 버튼 대체. */
val DeleteTrashIcon: ImageVector by lazy {
    materialIcon(
        "DeleteTrash",
        "M6,19c0,1.1 0.9,2 2,2h8c1.1,0 2,-0.9 2,-2V7H6v12zM19,4h-3.5l-1,-1h-5l-1,1H5v2h14V4z",
    )
}

/** 검색(돋보기). 메모 목록 상단바 "검색" 텍스트 버튼 대체. */
val SearchMagnifierIcon: ImageVector by lazy {
    materialIcon(
        "SearchMagnifier",
        "M15.5,14h-0.79l-0.28,-0.27C15.41,12.59 16,11.11 16,9.5 16,5.91 13.09,3 9.5,3S3,5.91 3,9.5" +
            " 5.91,16 9.5,16c1.61,0 3.09,-0.59 4.23,-1.57l0.27,0.28v0.79l5,4.99L20.49,19l-4.99,-5z" +
            "M9.5,14C7.01,14 5,11.99 5,9.5S7.01,5 9.5,5 14,7.01 14,9.5 11.99,14 9.5,14z",
    )
}

/** 필기(펜). 에디터 상단바 "필기" 텍스트 버튼 대체(S펜 잉크 캔버스 진입). */
val PenIcon: ImageVector by lazy {
    materialIcon(
        "Pen",
        "M3,17.25V21h3.75L17.81,9.94l-3.75,-3.75L3,17.25zM20.71,7.04c0.39,-0.39 0.39,-1.02 0,-1.41" +
            "l-2.34,-2.34c-0.39,-0.39,-1.02,-0.39,-1.41,0l-1.83,1.83l3.75,3.75L20.71,7.04z",
    )
}

/** 드래그 핸들(⋮⋮ 점 6개). 캘린더 상세에서 끌어서 메모↔To-Do 전환. */
val DragHandleIcon: ImageVector by lazy {
    materialIcon(
        "DragHandle",
        "M11,18c0,1.1 -0.9,2 -2,2s-2,-0.9 -2,-2 0.9,-2 2,-2 2,0.9 2,2zM9,10c-1.1,0 -2,0.9 -2,2" +
            "s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2zM9,4c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2" +
            " -0.9,-2 -2,-2zM15,8c1.1,0 2,-0.9 2,-2s-0.9,-2 -2,-2 -2,0.9 -2,2 0.9,2 2,2zM15,10" +
            "c-1.1,0 -2,0.9 -2,2s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2zM15,16c-1.1,0 -2,0.9 -2,2" +
            "s0.9,2 2,2 2,-0.9 2,-2 -0.9,-2 -2,-2z",
    )
}

/** 닫기/지우기(✕). 결과 카드 "닫기"·인라인 "지우기" 텍스트 버튼 대체. */
val CloseXIcon: ImageVector by lazy {
    materialIcon(
        "CloseX",
        "M19,6.41L17.59,5L12,10.59L6.41,5L5,6.41L10.59,12L5,17.59L6.41,19L12,13.41L17.59,19L19,17.59L13.41,12z",
    )
}

/** 동기화(원형 화살표 2개). 24x24 표준 sync 경로. */
val SyncArrowsIcon: ImageVector by lazy {
    val path = "M12,4V1L8,5l4,4V6c3.31,0 6,2.69 6,6c0,1.01,-0.25,1.97,-0.7,2.8l1.46,1.46" +
        "C19.54,15.03 20,13.57 20,12c0,-4.42,-3.58,-8,-8,-8zM12,18c-3.31,0,-6,-2.69,-6,-6" +
        "c0,-1.01 0.25,-1.97 0.7,-2.8L5.24,7.74C4.46,8.97 4,10.43 4,12c0,4.42 3.58,8 8,8v3" +
        "l4,-4l-4,-4V18z"
    ImageVector.Builder(
        name = "SyncArrows",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = PathParser().parsePathString(path).toNodes(),
        fill = SolidColor(Color.Black),
    ).build()
}
