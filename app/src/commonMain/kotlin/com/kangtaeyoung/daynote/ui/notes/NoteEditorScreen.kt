package com.kangtaeyoung.daynote.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kangtaeyoung.daynote.ui.ai.AiPanel
import com.kangtaeyoung.daynote.ui.ai.rememberAiShare
import kotlinx.coroutines.launch
import com.kangtaeyoung.daynote.domain.usecase.AddNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.AddTaskUseCase
import com.kangtaeyoung.daynote.domain.usecase.DeleteNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.DeleteTaskUseCase
import com.kangtaeyoung.daynote.data.repository.SettingsRepository
import com.kangtaeyoung.daynote.core.toLocalDate
import com.kangtaeyoung.daynote.domain.model.Note
import com.kangtaeyoung.daynote.domain.usecase.FindRelatedNotesUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveNoteTasksUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.SuggestTitleUseCase
import com.kangtaeyoung.daynote.domain.usecase.ToggleTaskUseCase
import com.kangtaeyoung.daynote.domain.usecase.UpdateNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.UpdateTaskUseCase
import com.kangtaeyoung.daynote.ui.components.AppBackHandler
import com.kangtaeyoung.daynote.ui.components.MarkdownText
import com.kangtaeyoung.daynote.ui.components.TaskRow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: String?,
    initialDate: Long? = null,
    onBack: () -> Unit,
    onOpenInk: () -> Unit = {},
    onOpenNote: (String) -> Unit = {},
) {
    val observeNote = koinInject<ObserveNoteUseCase>()
    val addNote = koinInject<AddNoteUseCase>()
    val updateNote = koinInject<UpdateNoteUseCase>()
    val deleteNote = koinInject<DeleteNoteUseCase>()
    val observeNoteTasks = koinInject<ObserveNoteTasksUseCase>()
    val addTask = koinInject<AddTaskUseCase>()
    val toggleTask = koinInject<ToggleTaskUseCase>()
    val deleteTask = koinInject<DeleteTaskUseCase>()
    val updateTask = koinInject<UpdateTaskUseCase>()
    val suggestTitle = koinInject<SuggestTitleUseCase>()
    val settings = koinInject<SettingsRepository>()
    val findRelatedNotes = koinInject<FindRelatedNotesUseCase>()

    val vm = viewModel(key = "editor:${noteId ?: "new:$initialDate"}") {
        NoteEditorViewModel(
            initialNoteId = noteId,
            initialDate = initialDate,
            observeNote = observeNote,
            addNote = addNote,
            updateNote = updateNote,
            deleteNote = deleteNote,
            observeNoteTasks = observeNoteTasks,
            addTask = addTask,
            toggleTask = toggleTask,
            deleteTask = deleteTask,
            updateTask = updateTask,
            suggestTitle = suggestTitle,
            settings = settings,
            findRelatedNotes = findRelatedNotes,
        )
    }
    val tasks by vm.tasks.collectAsState()
    val savedNoteId by vm.savedId.collectAsState()
    val relatedNotes by vm.relatedNotes.collectAsState()
    var preview by remember { mutableStateOf(false) }

    val aiShare = rememberAiShare()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // 저장 — 하단 버튼과 Ctrl+S(태블릿 키보드·PC) 공용.
    val doSave: () -> Unit = {
        if (savedNoteId == null && vm.title.isBlank() && vm.content.isBlank()) {
            scope.launch { snackbarHostState.showSnackbar("저장할 내용이 없어요.") }
        } else {
            vm.save {
                scope.launch { snackbarHostState.showSnackbar("저장되었습니다 ✓") }
            }
        }
    }

    // 뒤로 나가기 — 저장 안 한 변경이 있으면 확인을 먼저 묻는다("뒤로" 버튼·시스템 뒤로 공통).
    var showExitConfirm by remember { mutableStateOf(false) }
    val requestBack: () -> Unit = {
        if (vm.isDirty()) showExitConfirm = true else onBack()
    }
    AppBackHandler(enabled = true) { requestBack() }

    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            title = { Text("저장하지 않고 나가시겠습니까?") },
            text = { Text("저장하지 않은 변경 내용은 사라집니다.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitConfirm = false
                    onBack()
                }) { Text("나가기") }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) { Text("취소") }
            },
        )
    }

    Scaffold(
        // Ctrl+S = 저장(태블릿 외장 키보드·PC). preview 단계라 본문 입력 중에도 동작한다.
        modifier = Modifier.onPreviewKeyEvent { e ->
            if (e.type == KeyEventType.KeyDown && e.isCtrlPressed && e.key == Key.S) {
                doSave()
                true
            } else {
                false
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (noteId == null) "새 메모" else "메모",
                        fontWeight = FontWeight.Medium,
                    )
                },
                navigationIcon = {
                    TextButton(onClick = requestBack) { Text("뒤로") }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val text = buildAiShareText(vm.title, vm.content)
                            if (text.isBlank()) {
                                scope.launch { snackbarHostState.showSnackbar("보낼 내용이 없어요.") }
                            } else {
                                aiShare.share(text)
                                if (aiShare.confirmMessage.isNotBlank()) {
                                    scope.launch { snackbarHostState.showSnackbar(aiShare.confirmMessage) }
                                }
                            }
                        },
                    ) { Text(aiShare.actionLabel) }
                    TextButton(onClick = onOpenInk) { Text("필기") }
                    TextButton(onClick = { preview = !preview }) {
                        Text(if (preview) "편집" else "미리보기")
                    }
                    TextButton(onClick = { vm.deleteCurrent(onBack) }) { Text("삭제") }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    // 저장 피드백: 실제로 저장됐을 때만 "저장되었습니다", 빈 메모면 안내(doSave 공용).
                    onClick = doSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding() // 시스템 내비게이션 바와 겹치지 않게
                        .imePadding()
                        .padding(16.dp),
                ) { Text("저장") }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = vm.title,
                onValueChange = vm::onTitleChange,
                label = { Text("제목") },
                singleLine = true,
                // 제목칸 옆 ✨ — 본문을 근거로 AI 제목 생성(키 없음/실패 시 본문 첫 줄 폴백).
                trailingIcon = {
                    if (vm.titleLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        TextButton(
                            onClick = { vm.suggestTitleNow() },
                            enabled = vm.content.isNotBlank(),
                        ) { Text("✨ 제목") }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()

            // 편집 = BasicTextField (마크다운 원문), 렌더 = 라이브러리 (분리)
            if (preview) {
                MarkdownText(content = vm.content, modifier = Modifier.fillMaxWidth())
            } else {
                if (vm.content.isEmpty()) {
                    Text(
                        "마크다운으로 작성하세요. (# 제목, - 목록, **굵게**)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BasicTextField(
                    value = vm.content,
                    onValueChange = vm::onContentChange,
                    textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    // 위/아래 화살표의 '포커스 이탈'만 취소 → 첫 줄에서 제목으로 점프하지 않고
                    // 필드 안에서 커서만 움직인다(텍스트 커서 이동은 그대로 동작).
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusProperties {
                            up = FocusRequester.Cancel
                            down = FocusRequester.Cancel
                        },
                )
            }

            // 실시간 유사 메모 추천 — 입력을 디바운스해 로컬 FTS 로 다른 날의 비슷한 메모를 보여준다.
            if (relatedNotes.isNotEmpty()) {
                Text(
                    "비슷한 메모",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 2.sp,
                )
                relatedNotes.forEach { note ->
                    RelatedNoteRow(note = note, onOpen = { onOpenNote(note.id) })
                }
            }

            HorizontalDivider()

            Text(
                "TO-DO",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 2.sp,
            )
            tasks.forEach { task ->
                TaskRow(
                    task = task,
                    onToggle = { vm.toggle(task.id) },
                    onDelete = { vm.removeTask(task.id) },
                    onUpdate = vm::editTask,
                )
            }
            TaskInput(onAdd = { text ->
                if (text.isBlank()) {
                    scope.launch { snackbarHostState.showSnackbar("할 일 내용을 입력하세요.") }
                } else {
                    vm.addNewTask(text)
                    scope.launch { snackbarHostState.showSnackbar("할 일이 추가되었습니다 ✓") }
                }
            })

            HorizontalDivider()

            // Phase 4-B: OpenAI 요약·확장·교정. 본문을 소스로, 결과는 본문 끝에 덧붙인다.
            AiPanel(
                sourceText = buildAiShareText(vm.title, vm.content),
                noteId = savedNoteId,
                onApplyToNote = { result ->
                    val merged = if (vm.content.isBlank()) result else "${vm.content}\n\n$result"
                    vm.onContentChange(merged)
                },
            )
        }
    }
}

/** 제목과 본문을 AI 로 보낼 한 덩어리 텍스트로 합친다. 둘 다 비면 빈 문자열. */
private fun buildAiShareText(title: String, content: String): String {
    val t = title.trim()
    val c = content.trim()
    return when {
        t.isNotEmpty() && c.isNotEmpty() -> "$t\n\n$c"
        else -> t.ifEmpty { c }
    }
}

/** 추천된 비슷한 메모 한 줄 — 날짜와 제목을 보여주고 탭하면 그 메모를 연다. */
@Composable
private fun RelatedNoteRow(note: Note, onOpen: () -> Unit) {
    val dateLabel = note.date?.toLocalDate()?.let { "${it.monthNumber}월 ${it.dayOfMonth}일" } ?: "날짜 없음"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onOpen)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                note.title.ifBlank { "(제목 없음)" },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "$dateLabel${if (note.content.isNotBlank()) " · ${note.content.trim().take(60)}" else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TaskInput(onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("할 일 추가") },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = {
            onAdd(text)
            text = ""
        }) { Text("추가") }
    }
}
