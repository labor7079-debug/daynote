package com.kangtaeyoung.daynote.ui.ink

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** 한 획의 한 점. [pressure] 는 필압(S펜은 실제값, 마우스/손가락은 대략 1.0). */
data class InkPoint(val x: Float, val y: Float, val pressure: Float)

/** 완성된 한 획. 색·기본 굵기(px)를 함께 들고, 필압으로 굵기를 변조한다. */
data class InkStroke(val points: List<InkPoint>, val color: Color, val widthPx: Float)

/**
 * 필기 상태(획 목록). UI 는 이 홀더만 보고 그린다 — 실행취소/전체지우기는 여기로.
 * (아직 영속화하지 않는다 — 5-C 후속에서 ML Kit 텍스트 변환 또는 저장 계층을 얹는다.)
 */
@Stable
class InkState {
    val strokes = mutableStateListOf<InkStroke>()
    fun add(stroke: InkStroke) { strokes.add(stroke) }
    fun undo() { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) }
    fun clear() { strokes.clear() }
    val isEmpty: Boolean get() = strokes.isEmpty()
}

@Composable
fun rememberInkState(): InkState = remember { InkState() }

/** 필압 → 굵기 배율(0.35~1.0). 필압을 못 얻으면 거의 기본 굵기. */
private fun pressureFactor(p: Float): Float = 0.35f + 0.65f * p.coerceIn(0f, 1f)

private fun DrawScope.drawInk(stroke: InkStroke) {
    val pts = stroke.points
    if (pts.isEmpty()) return
    if (pts.size == 1) {
        val p = pts[0]
        drawCircle(stroke.color, radius = stroke.widthPx * 0.5f * pressureFactor(p.pressure), center = Offset(p.x, p.y))
        return
    }
    for (i in 1 until pts.size) {
        val a = pts[i - 1]; val b = pts[i]
        drawLine(
            color = stroke.color,
            start = Offset(a.x, a.y),
            end = Offset(b.x, b.y),
            strokeWidth = stroke.widthPx * pressureFactor((a.pressure + b.pressure) * 0.5f),
            cap = StrokeCap.Round,
        )
    }
}

private fun eraseAt(state: InkState, pos: Offset, radius: Float) {
    val r2 = radius * radius
    state.strokes.removeAll { s ->
        s.points.any { val dx = it.x - pos.x; val dy = it.y - pos.y; dx * dx + dy * dy <= r2 }
    }
}

/**
 * 자유 필기 표면(commonMain 공유). 포인터(마우스·손가락·S펜)로 획을 그린다.
 * - **필압**: S펜 필압으로 굵기 변조(마우스/손가락은 균일).
 * - **S펜 지우개**: 펜을 뒤집으면 [PointerType.Eraser] 로 들어와 [eraser] 토글과 무관하게 지운다.
 */
@Composable
fun InkCanvas(
    state: InkState,
    penColor: Color,
    strokeWidthDp: Float,
    eraser: Boolean,
    modifier: Modifier = Modifier,
) {
    val live = remember { mutableStateListOf<InkPoint>() }
    val colorS = rememberUpdatedState(penColor)
    val widthS = rememberUpdatedState(strokeWidthDp)
    val eraserS = rememberUpdatedState(eraser)
    val density = LocalDensity.current

    Canvas(
        modifier = modifier.pointerInput(Unit) {
            val eraseRadius = with(density) { 18.dp.toPx() }
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val isErase = eraserS.value || down.type == PointerType.Eraser
                if (isErase) {
                    eraseAt(state, down.position, eraseRadius)
                } else {
                    live.clear()
                    live.add(InkPoint(down.position.x, down.position.y, down.pressure))
                }
                down.consume()
                val id = down.id
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == id } ?: break
                    if (!change.pressed) break
                    if (isErase) {
                        eraseAt(state, change.position, eraseRadius)
                    } else {
                        live.add(InkPoint(change.position.x, change.position.y, change.pressure))
                    }
                    change.consume()
                }
                if (!isErase && live.isNotEmpty()) {
                    val widthPx = with(density) { widthS.value.dp.toPx() }
                    state.add(InkStroke(live.toList(), colorS.value, widthPx))
                    live.clear()
                }
            }
        },
    ) {
        state.strokes.forEach { drawInk(it) }
        if (live.isNotEmpty()) {
            val widthPx = with(density) { widthS.value.dp.toPx() }
            drawInk(InkStroke(live.toList(), colorS.value, widthPx))
        }
    }
}
