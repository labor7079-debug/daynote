package com.kangtaeyoung.daynote.ui.ai

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Android: 표준 Sharesheet(`ACTION_SEND`)로 메모 텍스트를 공유한다. 사용자가 시트에서 ChatGPT 등
 * 원하는 앱을 고른다. API 키·추가 의존성 불필요(설계원칙 — 4-A 는 가장 쉬운 경로).
 */
@Composable
actual fun rememberAiShare(): AiShare {
    val context = LocalContext.current
    return remember(context) {
        AiShare(
            actionLabel = "AI로 보내기",
            confirmMessage = "", // 시트가 떠서 별도 확인 불필요
            share = { text -> shareText(context, text) },
        )
    }
}

private fun shareText(context: Context, text: String) {
    if (text.isBlank()) return
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooser = Intent.createChooser(send, "AI로 보내기").apply {
        // Activity 컨텍스트가 아닐 수 있어 안전하게 새 태스크 플래그를 단다.
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}
