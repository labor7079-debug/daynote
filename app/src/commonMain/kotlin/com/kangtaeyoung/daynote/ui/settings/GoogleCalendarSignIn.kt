package com.kangtaeyoung.daynote.ui.settings

import androidx.compose.runtime.Composable

/**
 * 구글 캘린더 로그인(인가) 트리거를 제공한다. 동의 UI는 Activity·ActivityResult 런처가 필요해
 * 플랫폼 Compose 쪽에서 처리한다. 반환된 람다를 버튼 onClick 에 연결하면 인가 플로우가 시작된다.
 *
 * - Android: Authorization API(`Identity.getAuthorizationClient`)로 calendar 범위 액세스 토큰 요청.
 * - Desktop: 미지원 — no-op.
 */
@Composable
expect fun rememberGoogleCalendarSignIn(): () -> Unit
