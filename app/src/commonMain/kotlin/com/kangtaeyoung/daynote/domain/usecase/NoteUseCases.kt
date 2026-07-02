package com.kangtaeyoung.daynote.domain.usecase

import com.kangtaeyoung.daynote.data.repository.NoteRepository
import com.kangtaeyoung.daynote.domain.model.Note
import kotlinx.coroutines.flow.Flow

/**
 * 메모 UseCase 모음. 각 클래스는 한 가지 의도를 표현하는 얇은 래퍼로,
 * ViewModel 이 Repository 대신 의존해 도메인 어휘를 사용하게 한다(MVVM + UseCase).
 */

class ObserveNotesUseCase(private val repo: NoteRepository) {
    operator fun invoke(): Flow<List<Note>> = repo.observeNotes()
}

class ObserveNoteUseCase(private val repo: NoteRepository) {
    operator fun invoke(id: String): Flow<Note?> = repo.observeNote(id)
}

/** 특정 날짜 구간 [startOfDay, endOfDay) 의 메모 (캘린더 동선). */
class ObserveNotesByDateUseCase(private val repo: NoteRepository) {
    operator fun invoke(startOfDay: Long, endOfDay: Long): Flow<List<Note>> =
        repo.observeNotesByDate(startOfDay, endOfDay)
}

/** 날짜 없는 메모(보조 목록). */
class ObserveUndatedNotesUseCase(private val repo: NoteRepository) {
    operator fun invoke(): Flow<List<Note>> = repo.observeUndatedNotes()
}

class SearchNotesUseCase(private val repo: NoteRepository) {
    operator fun invoke(query: String): Flow<List<Note>> = repo.search(query)
}

/** 작성 중인 텍스트와 비슷한 다른 날의 메모 찾기(에디터 실시간 추천). */
class FindRelatedNotesUseCase(private val repo: NoteRepository) {
    operator fun invoke(sourceText: String, excludeNoteId: String?, excludeDate: Long?): Flow<List<Note>> =
        repo.searchRelated(sourceText, excludeNoteId, excludeDate)
}

/**
 * 새 메모 생성. 빈 메모 방지(둘 다 공백일 때 만들지 않기)는 호출부(ViewModel)가 판단한다 —
 * 할 일을 붙이려면 빈 메모라도 만들어야 하므로 UseCase 자체는 가드를 두지 않는다.
 */
class AddNoteUseCase(private val repo: NoteRepository) {
    suspend operator fun invoke(title: String, content: String, date: Long? = null): Note =
        repo.addNote(title.trim(), content, date)
}

class UpdateNoteUseCase(private val repo: NoteRepository) {
    suspend operator fun invoke(note: Note) = repo.updateNote(note)
}

class SetNotePinnedUseCase(private val repo: NoteRepository) {
    suspend operator fun invoke(id: String, pinned: Boolean) = repo.setPinned(id, pinned)
}

class DeleteNoteUseCase(private val repo: NoteRepository) {
    suspend operator fun invoke(id: String) = repo.deleteNote(id)
}
