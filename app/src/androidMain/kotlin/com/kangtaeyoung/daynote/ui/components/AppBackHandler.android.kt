package com.kangtaeyoung.daynote.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

/** Android: 시스템 뒤로가기(제스처·버튼)를 가로챈다. */
@Composable
actual fun AppBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}
