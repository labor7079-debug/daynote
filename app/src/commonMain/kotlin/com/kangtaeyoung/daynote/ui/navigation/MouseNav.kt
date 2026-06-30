package com.kangtaeyoung.daynote.ui.navigation

import androidx.compose.ui.Modifier

/**
 * 마우스 옆 버튼(뒤로/앞으로) 처리. `PointerButton`/`PointerEvent.button` 은 데스크톱 전용 API라
 * `commonMain` 에 둘 수 없어 플랫폼별로 격리한다. Android 는 시스템 뒤로가기가 있어 no-op.
 */
expect fun Modifier.mouseBackForward(onBack: () -> Unit, onForward: () -> Unit): Modifier
