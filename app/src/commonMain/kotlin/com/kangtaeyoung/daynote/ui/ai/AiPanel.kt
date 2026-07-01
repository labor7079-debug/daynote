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
import com.kangtaeyoung.daynote.core.toShortDateTimeLabel
import com.kangtaeyoung.daynote.domain.model.AiAction
import com.kangtaeyoung.daynote.domain.model.AiResult
import com.kangtaeyoung.daynote.domain.usecase.AskAiUseCase
import com.kangtaeyoung.daynote.domain.usecase.DeleteAiResultUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveAiResultsUseCase
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
    val observeAiResults = koinInject<ObserveAiResultsUseCase>()
    val deleteAiResult = koinInject<DeleteAiResultUseCase>()
    val vm = viewModel(key = "ai:${noteId ?: "new"}") {
        AiViewModel(runAiAction, askAi, observeAiResults, deleteAiResult, noteId)
    }
    val state by vm.uiState.collectAsState()
    val history by vm.history.collectAsState()
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

        // 과거 AI 결과 이력(요약·확장·교정·질문). Room 이 진실의 원천이라 오프라인·재시작에 유지된다.
        if (history.isNotEmpty()) {
            AiHistorySection(
                history = history,
                onApplyToNote = onApplyToNote,
                onDelete = vm::deleteHistory,
            )
        }
    }
}

/** 접었다 펼치는 지난 AI 결과 목록. 각 항목은 "메모에 반영"·삭제(✕)를 제공한다. */
@Composable
private fun AiHistorySection(
    history: List<AiResult>,
    onApplyToNote: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { expanded = !expanded }) {
            Text(
                if (expanded) "지난 AI 결과 숨기기" else "지난 AI 결과 ${history.size}",
                style = MaterialTheme.typography.labelMedium,
            )
        }

        if (expanded) {
            history.forEach { item ->
                AiHistoryItem(
                    item = item,
                    onApply = { onApplyToNote(item.resultText) },
                    onDelete = { onDelete(item.id) },
                )
            }
        }
    }
}

@Composable
private fun AiHistoryItem(
    item: AiResult,
    onApply: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${item.action.label} · ${item.createdAt.toShortDateTimeLabel()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // 질문(ASK)은 무엇을 물었는지 보이도록 sourceText(질문 문장)를 함께 노출.
            }
            if (item.action == AiAction.ASK && item.sourceText.isNotBlank()) {
                Text(
                    "Q. ${item.sourceText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(item.resultText, style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onApply) { Text("메모에 반영") }
                TextButton(onClick = onDelete) { Text("삭제") }
            }
        }
    }
}
