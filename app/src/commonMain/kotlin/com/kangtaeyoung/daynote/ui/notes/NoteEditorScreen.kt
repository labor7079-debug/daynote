package com.kangtaeyoung.daynote.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kangtaeyoung.daynote.domain.usecase.AddNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.AddTaskUseCase
import com.kangtaeyoung.daynote.domain.usecase.DeleteNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.DeleteTaskUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveNoteTasksUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.ToggleTaskUseCase
import com.kangtaeyoung.daynote.domain.usecase.UpdateNoteUseCase
import com.kangtaeyoung.daynote.ui.components.MarkdownText
import com.kangtaeyoung.daynote.ui.components.TaskRow
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: String?,
    initialDate: Long? = null,
    onBack: () -> Unit,
) {
    val observeNote = koinInject<ObserveNoteUseCase>()
    val addNote = koinInject<AddNoteUseCase>()
    val updateNote = koinInject<UpdateNoteUseCase>()
    val deleteNote = koinInject<DeleteNoteUseCase>()
    val observeNoteTasks = koinInject<ObserveNoteTasksUseCase>()
    val addTask = koinInject<AddTaskUseCase>()
    val toggleTask = koinInject<ToggleTaskUseCase>()
    val deleteTask = koinInject<DeleteTaskUseCase>()

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
        )
    }
    val tasks by vm.tasks.collectAsState()
    var preview by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId == null) "새 메모" else "메모") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("뒤로") }
                },
                actions = {
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
                    onClick = { vm.save() },
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
                onValueChange = { vm.title = it },
                label = { Text("제목") },
                singleLine = true,
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
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            HorizontalDivider()

            Text("할 일", style = MaterialTheme.typography.titleSmall)
            tasks.forEach { task ->
                TaskRow(
                    task = task,
                    onToggle = { vm.toggle(task.id) },
                    onDelete = { vm.removeTask(task.id) },
                )
            }
            TaskInput(onAdd = vm::addNewTask)
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
