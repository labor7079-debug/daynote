package com.kangtaeyoung.daynote.ui.ai

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/**
 * Desktop: Sharesheet 가 없으므로 메모 텍스트를 시스템 클립보드에 복사한다.
 * 사용자가 ChatGPT 웹/앱에 붙여넣어 사용한다. (AWT 클립보드 — 추가 의존성 불필요)
 */
@Composable
actual fun rememberAiShare(): AiShare = remember {
    AiShare(
        actionLabel = "AI용으로 복사",
        confirmMessage = "클립보드에 복사했어요. ChatGPT에 붙여넣으세요.",
        share = { text -> copyToClipboard(text) },
    )
}

private fun copyToClipboard(text: String) {
    if (text.isBlank()) return
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    clipboard.setContents(StringSelection(text), null)
}
