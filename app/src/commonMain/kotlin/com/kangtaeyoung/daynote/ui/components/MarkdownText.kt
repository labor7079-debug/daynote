package com.kangtaeyoung.daynote.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mikepenz.markdown.m3.Markdown

/**
 * 마크다운 렌더링 전용 컴포저블(Step 4: 편집과 렌더 분리 — 편집은 `BasicTextField`, 렌더는 라이브러리).
 *
 * 렌더는 CMP 호환 라이브러리(mikepenz multiplatform-markdown-renderer, Material3 변형)에 위임한다.
 * 빈 본문은 안내 문구를 보여준다.
 */
@Composable
fun MarkdownText(
    content: String,
    modifier: Modifier = Modifier,
) {
    if (content.isBlank()) {
        Text(
            text = "표시할 내용이 없습니다.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }
    Box(modifier = modifier.verticalScroll(rememberScrollState())) {
        Markdown(content = content)
    }
}
