package com.kangtaeyoung.daynote.ui.navigation

import androidx.compose.ui.Modifier

/** Android: 마우스 옆버튼은 드물고 시스템 뒤로가기가 있으므로 처리하지 않는다. */
actual fun Modifier.mouseBackForward(onBack: () -> Unit, onForward: () -> Unit): Modifier = this
