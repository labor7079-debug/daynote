package com.kangtaeyoung.daynote.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kangtaeyoung.daynote.domain.usecase.SearchNotesUseCase
import com.kangtaeyoung.daynote.ui.components.NoteListItem
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onOpenNote: (String) -> Unit,
    onBack: () -> Unit,
) {
    val searchNotes = koinInject<SearchNotesUseCase>()
    val vm = viewModel { SearchViewModel(searchNotes) }
    val query by vm.query.collectAsState()
    val results by vm.results.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("검색") },
                navigationIcon = { TextButton(onClick = onBack) { Text("뒤로") } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = vm::onQueryChange,
                    label = { Text("메모 본문·제목 검색") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (query.isNotBlank() && results.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "검색 결과가 없습니다.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(results, key = { it.id }) { note ->
                NoteListItem(note = note, onClick = { onOpenNote(note.id) })
            }
        }
    }
}
