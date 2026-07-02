package com.kangtaeyoung.daynote.ui.notes

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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kangtaeyoung.daynote.core.toLocalDate
import com.kangtaeyoung.daynote.domain.model.Note
import com.kangtaeyoung.daynote.domain.usecase.DeleteNoteUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveNotesUseCase
import com.kangtaeyoung.daynote.domain.usecase.SetNotePinnedUseCase
import com.kangtaeyoung.daynote.ui.components.DayNoteBottomBar
import com.kangtaeyoung.daynote.ui.components.NoteListItem
import com.kangtaeyoung.daynote.ui.components.TopDestination
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import org.koin.compose.koinInject

private val dowLabels = listOf("월", "화", "수", "목", "금", "토", "일")
private fun koreanDow(dow: DayOfWeek): String = dowLabels[dow.isoDayNumber - 1]

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
    val period by vm.period.collectAsState()

    // 일자별 그룹(최신 날짜 먼저). 기준은 캘린더 날짜(Note.date), 없으면 작성일.
    val grouped: List<Pair<LocalDate, List<Note>>> = remember(notes) {
        notes.groupBy { (it.date ?: it.createdAt).toLocalDate() }
            .entries
            .sortedByDescending { it.key }
            .map { it.key to it.value }
    }

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
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 기간 필터 — 전체/오늘/최근 7일/최근 30일.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NotesPeriod.entries.forEach { p ->
                    FilterChip(
                        selected = period == p,
                        onClick = { vm.setPeriod(p) },
                        label = { Text(p.label) },
                    )
                }
            }

            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (period == NotesPeriod.ALL) "메모가 없습니다. + 로 첫 메모를 작성하세요."
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
                            DateHeader(date, dayNotes.size)
                        }
                        items(dayNotes, key = { it.id }) { note ->
                            NoteListItem(note = note, onClick = { onOpenNote(note.id) })
                        }
                    }
                }
            }
        }
    }
}

/** 일자별 그룹 헤더 — "2026년 7월 2일 (수) · N개". 캘린더 상세와 같은 클리니컬 캡션 톤. */
@Composable
private fun DateHeader(date: LocalDate, count: Int) {
    Text(
        text = "${date.year}년 ${date.monthNumber}월 ${date.dayOfMonth}일 (${koreanDow(date.dayOfWeek)}) · ${count}개",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}
