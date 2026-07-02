package com.kangtaeyoung.daynote.ui.todo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kangtaeyoung.daynote.core.toLocalDate
import com.kangtaeyoung.daynote.domain.model.Task
import com.kangtaeyoung.daynote.domain.usecase.AddTaskUseCase
import com.kangtaeyoung.daynote.domain.usecase.DeleteTaskUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveGeneralTasksUseCase
import com.kangtaeyoung.daynote.domain.usecase.ToggleTaskUseCase
import com.kangtaeyoung.daynote.domain.usecase.UpdateTaskUseCase
import com.kangtaeyoung.daynote.ui.components.DateGroupHeader
import com.kangtaeyoung.daynote.ui.components.DayNoteBottomBar
import com.kangtaeyoung.daynote.ui.components.Period
import com.kangtaeyoung.daynote.ui.components.PeriodFilterRow
import com.kangtaeyoung.daynote.ui.components.TaskRow
import com.kangtaeyoung.daynote.ui.components.TopDestination
import kotlinx.datetime.LocalDate
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen(
    onSelectDestination: (TopDestination) -> Unit,
) {
    val observeGeneralTasks = koinInject<ObserveGeneralTasksUseCase>()
    val addTask = koinInject<AddTaskUseCase>()
    val toggleTask = koinInject<ToggleTaskUseCase>()
    val deleteTask = koinInject<DeleteTaskUseCase>()
    val updateTask = koinInject<UpdateTaskUseCase>()
    val vm = viewModel { TodoViewModel(observeGeneralTasks, addTask, toggleTask, deleteTask, updateTask) }
    val tasks by vm.tasks.collectAsState()
    val period by vm.period.collectAsState()
    var text by remember { mutableStateOf("") }

    // 일자별 그룹(최신 날짜 먼저). 기준은 마감일(Task.dueDate), 없으면 작성일.
    val grouped: List<Pair<LocalDate, List<Task>>> = remember(tasks) {
        tasks.groupBy { (it.dueDate ?: it.createdAt).toLocalDate() }
            .entries
            .sortedByDescending { it.key }
            .map { it.key to it.value }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("To-Do") }) },
        bottomBar = {
            DayNoteBottomBar(current = TopDestination.Todo, onSelect = onSelectDestination)
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
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
                    vm.add(text)
                    text = ""
                }) { Text("추가") }
            }

            // 기간 필터 — 전체/오늘/최근 7일/최근 30일(메모 목록과 동일).
            PeriodFilterRow(selected = period, onSelect = vm::setPeriod)

            if (tasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (period == Period.ALL) "할 일이 없습니다."
                        else "「${period.label}」 기간의 할 일이 없습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    grouped.forEach { (date, dayTasks) ->
                        item(key = "header:$date") {
                            DateGroupHeader(date, dayTasks.size)
                        }
                        items(dayTasks, key = { it.id }) { task ->
                            TaskRow(
                                task = task,
                                onToggle = { vm.toggle(task.id) },
                                onDelete = { vm.remove(task.id) },
                                onMoveTo = { date -> vm.moveToDate(task, date) },
                                onCopyTo = { date -> vm.copyToDate(task, date) },
                            )
                        }
                    }
                }
            }
        }
    }
}
