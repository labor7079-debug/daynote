package com.kangtaeyoung.daynote.ui.settings

import androidx.compose.runtime.Composable

/** Desktop: 구글 캘린더 동기화 미지원 — no-op. */
@Composable
actual fun rememberGoogleCalendarSignIn(): () -> Unit = {}
