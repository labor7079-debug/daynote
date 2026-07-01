package com.kangtaeyoung.daynote.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kangtaeyoung.daynote.domain.model.AiAction
import com.kangtaeyoung.daynote.domain.usecase.AskAiUseCase
import com.kangtaeyoung.daynote.domain.usecase.RunAiActionUseCase
import org.koin.compose.koinInject

/**
 * AI 패널 — 동작 칩(요약·확장·교정) → Loading → Success/Error.
 * "메모에 반영"은 결과를 노트 본문에 쓰는 별도 콜백([onApplyToNote]).
 *
 * [sourceText] 가 비면 칩은 비활성. ViewModel 은 noteId 별로 분리해 메모마다 독립 상태를 갖는다.
 */
@Composable
fun AiPanel(
    sourceText: String,
    noteId: String?,
    onApplyToNote: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val runAiAction = koinInject<RunAiActionUseCase>()
    val askAi = koinInject<AskAiUseCase>()
    val vm = viewModel(key = "ai:${noteId ?: "new"}") { AiViewModel(runAiAction, askAi) }
    val state by vm.uiState.collectAsState()
    var question by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("AI", style = MaterialTheme.typography.titleSmall)

        // 변환 동작(요약·확장·교정) — ASK 는 아래 질문 입력창에서 쓰므로 칩에서 제외.
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AiAction.entries.filter { it != AiAction.ASK }.forEach { action ->
                AssistChip(
                    onClick = { vm.run(action, sourceText, noteId) },
                    enabled = sourceText.isNotBlank() && state !is AiUiState.Loading,
                    label = { Text(action.label) },
                )
            }
        }

        // 자유 질문 — 메모를 맥락으로 OpenAI 에 묻고 답을 아래에 인라인 표시(앱 전환 없음).
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("AI에게 질문") },
                singleLine = true,
                enabled = state !is AiUiState.Loading,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    vm.ask(question, sourceText, noteId)
                    question = ""
                },
                enabled = question.isNotBlank() && state !is AiUiState.Loading,
            ) { Text("질문") }
        }

        when (val s = state) {
            AiUiState.Idle -> Unit

            is AiUiState.Loading -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(4.dp))
                Text("${s.action.label} 중…", style = MaterialTheme.typography.bodySmall)
            }

            is AiUiState.Success -> Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(s.result.action.label + " 결과", style = MaterialTheme.typography.labelMedium)
                    Text(s.result.resultText, style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onApplyToNote(s.result.resultText) }) {
                            Text("메모에 반영")
                        }
                        TextButton(onClick = vm::reset) { Text("닫기") }
                    }
                }
            }

            is AiUiState.Error -> Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        s.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = vm::reset) { Text("닫기") }
                }
            }
        }
    }
}
