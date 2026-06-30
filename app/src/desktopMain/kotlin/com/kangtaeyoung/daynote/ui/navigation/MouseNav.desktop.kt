package com.kangtaeyoung.daynote.ui.navigation

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput

/** Desktop: 마우스 옆 버튼(뒤로/앞으로) press 를 감지해 내비게이션에 연결한다. */
@OptIn(ExperimentalComposeUiApi::class)
actual fun Modifier.mouseBackForward(onBack: () -> Unit, onForward: () -> Unit): Modifier =
    this.pointerInput(onBack, onForward) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.type == PointerEventType.Press) {
                    when (event.button) {
                        PointerButton.Back -> onBack()
                        PointerButton.Forward -> onForward()
                        else -> {}
                    }
                }
            }
        }
    }
