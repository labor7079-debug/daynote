package com.kangtaeyoung.daynote.ui.components

import androidx.compose.runtime.Composable

/** Desktop: 시스템 뒤로가기가 없으므로 no-op. */
@Composable
actual fun AppBackHandler(enabled: Boolean, onBack: () -> Unit) {
}
