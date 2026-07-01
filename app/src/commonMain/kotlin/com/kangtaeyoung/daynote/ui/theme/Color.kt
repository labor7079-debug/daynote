package com.kangtaeyoung.daynote.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 「Quiet Cadence」 팔레트 — 정제된 웜 모노크롬 + 절제된 흙빛 강조.
 *
 * 설계 원칙: **강조색은 장식이 아니라 표시**다. 차분한 슬레이트(slate)가 상호작용 전반(primary)을 맡고,
 * 흙빛 클레이(clay)는 진짜 강조(tertiary — 오늘·중요·핀)에만 드물게 쓴다. 나머지는 여백과 헤어라인.
 */

// --- 코어 톤 ---
val Paper      = Color(0xFFF0ECE3) // 웜 본(배경) — 여백이 주재료
val PaperHi    = Color(0xFFF5F2EA) // 살짝 밝은 카드 표면
val Ink        = Color(0xFF211E1A) // 웜 잉크(본문·제목)
val InkSoft    = Color(0xFF575046) // 보조 텍스트
val SoftGrey   = Color(0xFF8B8477) // 헤어라인·아웃라인
val FaintLine  = Color(0xFFC7C0B2) // 아주 옅은 분할선
val Slate      = Color(0xFF3A4E62) // 상호작용(primary)·선택
val Clay       = Color(0xFFAA422D) // 강조(tertiary)·오늘·중요

// --- 라이트 스킴 롤 ---
val LightPrimary            = Slate
val LightOnPrimary          = Color(0xFFF5F2EA)
val LightPrimaryContainer   = Color(0xFFDCE2E8)
val LightOnPrimaryContainer = Color(0xFF22303D)
val LightSecondary            = Color(0xFF6E675B)
val LightOnSecondary          = Color(0xFFF5F2EA)
val LightSecondaryContainer   = Color(0xFFE4DFD4)
val LightOnSecondaryContainer = Color(0xFF2A251E)
val LightTertiary            = Clay
val LightOnTertiary          = Color(0xFFF5F2EA)
val LightTertiaryContainer   = Color(0xFFF0D9CF)
val LightOnTertiaryContainer = Color(0xFF431407)
val LightBackground     = Paper
val LightOnBackground   = Ink
val LightSurface        = PaperHi
val LightOnSurface      = Ink
val LightSurfaceVariant  = Color(0xFFE3DED2)
val LightOnSurfaceVariant = InkSoft
val LightOutline        = SoftGrey
val LightOutlineVariant = FaintLine
val LightError            = Color(0xFF9C4232)
val LightOnError          = Color(0xFFF5F2EA)
val LightErrorContainer   = Color(0xFFF3D9D2)
val LightOnErrorContainer = Color(0xFF410E05)

// --- 다크 스킴 롤 (웜 차콜 — 순수 검정 아님) ---
val DarkPrimary            = Color(0xFF9DB2C6)
val DarkOnPrimary          = Color(0xFF1E2C38)
val DarkPrimaryContainer   = Color(0xFF33475A)
val DarkOnPrimaryContainer = Color(0xFFCFE0EE)
val DarkSecondary            = Color(0xFFCFC6B6)
val DarkOnSecondary          = Color(0xFF322C22)
val DarkSecondaryContainer   = Color(0xFF4A4437)
val DarkOnSecondaryContainer = Color(0xFFECE2D0)
val DarkTertiary            = Color(0xFFD98366)
val DarkOnTertiary          = Color(0xFF491A0C)
val DarkTertiaryContainer   = Color(0xFF7A3320)
val DarkOnTertiaryContainer = Color(0xFFFFD9CC)
val DarkBackground     = Color(0xFF1A1815)
val DarkOnBackground   = Color(0xFFECE7DC)
val DarkSurface        = Color(0xFF211E1A)
val DarkOnSurface      = Color(0xFFECE7DC)
val DarkSurfaceVariant  = Color(0xFF302C26)
val DarkOnSurfaceVariant = Color(0xFFC7C0B2)
val DarkOutline        = SoftGrey
val DarkOutlineVariant = Color(0xFF4A443B)
val DarkError            = Color(0xFFE39383)
val DarkOnError          = Color(0xFF5C1A0E)
val DarkErrorContainer   = Color(0xFF7A2D1D)
val DarkOnErrorContainer = Color(0xFFFFDAD1)
