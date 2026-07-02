package com.kangtaeyoung.daynote.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput

/**
 * 마우스 우클릭(보조 버튼) 감지 — PC(데스크톱)와 마우스 연결된 태블릿에서 길게 누름과 같은
 * 메뉴를 열 때 쓴다. 터치에는 보조 버튼이 없어 자연히 no-op(플랫폼 분기 불필요, 공용 코드).
 */
fun Modifier.onRightClick(onClick: () -> Unit): Modifier = pointerInput(onClick) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                event.changes.forEach { it.consume() }
                onClick()
            }
        }
    }
}
