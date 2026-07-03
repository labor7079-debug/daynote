package com.kangtaeyoung.daynote.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kangtaeyoung.daynote.core.toLocalDate
import com.kangtaeyoung.daynote.domain.model.Note
import com.kangtaeyoung.daynote.domain.usecase.AddNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.DeleteNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveNotesUseCase
import com.kangtaeyoung.daynote.domain.usecase.SetNotePinnedUseCase
import com.kangtaeyoung.daynote.domain.usecase.UpdateNoteUseCase
import com.kangtaeyoung.daynote.ui.components.DateGroupHeader
import com.kangtaeyoung.daynote.ui.components.DayNoteBottomBar
import com.kangtaeyoung.daynote.ui.components.NoteListItem
import com.kangtaeyoung.daynote.ui.components.Period
import com.kangtaeyoung.daynote.ui.components.PeriodFilterRow
import com.kangtaeyoung.daynote.ui.components.SyncFab
import com.kangtaeyoung.daynote.ui.components.TopDestination
import com.kangtaeyoung.daynote.ui.theme.SearchMagnifierIcon
import kotlinx.datetime.LocalDate
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    onOpenNote: (String) -> Unit,
    onNewNote: () -> Unit,
    onSearch: () -> Unit,
    onSelectDestination: (TopDestination) -> Unit,
    onOpenSettings: () -> Unit = {},
) {
    val observeNotes = koinInject<ObserveNotesUseCase>()
    val deleteNote = koinInject<DeleteNoteUseCase>()
    val setPinned = koinInject<SetNotePinnedUseCase>()
    val updateNote = koinInject<UpdateNoteUseCase>()
    val addNote = koinInject<AddNoteUseCase>()
    val vm = viewModel { NotesListViewModel(observeNotes, deleteNote, setPinned, updateNote, addNote) }
    val notes by vm.notes.collectAsState()
    val period by vm.period.collectAsState()

    // 일자별 그룹(최신 날짜 먼저). 기준은 캘린더 날짜(Note.date), 없으면 작성일.
    val grouped: List<Pair<LocalDate, List<Note>>> = remember(notes) {
        notes.groupBy { (it.date ?: it.createdAt).toLocalDate() }
            .entries
            .sortedByDescending { it.key }
            .map { it.key to it.value }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("DayNote") },
                actions = {
                    IconButton(onClick = onSearch) { Icon(SearchMagnifierIcon, contentDescription = "검색") }
                },
            )
        },
        bottomBar = {
            DayNoteBottomBar(
                current = TopDestination.Notes,
                onSelect = onSelectDestination,
                onOpenSettings = onOpenSettings,
            )
        },
        floatingActionButton = {
            // 우측 하단: 동기화(상시) 위에 새 메모(+) — 세로 스택.
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SyncFab(snackbarHostState)
                FloatingActionButton(onClick = onNewNote) { Text("+") }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 기간 필터 — 전체/오늘/최근 7일/최근 30일.
            PeriodFilterRow(selected = period, onSelect = vm::setPeriod)

            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (period == Period.ALL) "메모가 없습니다. + 로 첫 메모를 작성하세요."
                        else "「${period.label}」 기간에 작성된 메모가 없습니다.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    grouped.forEach { (date, dayNotes) ->
                        item(key = "header:$date") {
                            DateGroupHeader(date, dayNotes.size)
                        }
                        items(dayNotes, key = { it.id }) { note ->
                            NoteListItem(
                                note = note,
                                onClick = { onOpenNote(note.id) },
                                onMoveTo = { date -> vm.moveToDate(note, date) },
                                onCopyTo = { date -> vm.copyToDate(note, date) },
                                onDelete = { vm.delete(note.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}
