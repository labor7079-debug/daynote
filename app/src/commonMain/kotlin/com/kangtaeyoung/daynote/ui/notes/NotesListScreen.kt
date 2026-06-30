package com.kangtaeyoung.daynote.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kangtaeyoung.daynote.domain.usecase.DeleteNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveNotesUseCase
import com.kangtaeyoung.daynote.domain.usecase.SetNotePinnedUseCase
import com.kangtaeyoung.daynote.ui.components.DayNoteBottomBar
import com.kangtaeyoung.daynote.ui.components.NoteListItem
import com.kangtaeyoung.daynote.ui.components.TopDestination
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    onOpenNote: (String) -> Unit,
    onNewNote: () -> Unit,
    onSearch: () -> Unit,
    onSelectDestination: (TopDestination) -> Unit,
) {
    val observeNotes = koinInject<ObserveNotesUseCase>()
    val deleteNote = koinInject<DeleteNoteUseCase>()
    val setPinned = koinInject<SetNotePinnedUseCase>()
    val vm = viewModel { NotesListViewModel(observeNotes, deleteNote, setPinned) }
    val notes by vm.notes.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DayNote") },
                actions = { TextButton(onClick = onSearch) { Text("검색") } },
            )
        },
        bottomBar = {
            DayNoteBottomBar(current = TopDestination.Notes, onSelect = onSelectDestination)
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewNote) { Text("+") }
        },
    ) { padding ->
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "메모가 없습니다. + 로 첫 메모를 작성하세요.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(notes, key = { it.id }) { note ->
                    NoteListItem(note = note, onClick = { onOpenNote(note.id) })
                }
            }
        }
    }
}
