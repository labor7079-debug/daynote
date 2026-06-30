package com.kangtaeyoung.daynote.ui.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kangtaeyoung.daynote.domain.model.Task
import com.kangtaeyoung.daynote.domain.usecase.AddTaskUseCase
import com.kangtaeyoung.daynote.domain.usecase.DeleteTaskUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveGeneralTasksUseCase
import com.kangtaeyoung.daynote.domain.usecase.ToggleTaskUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 일반 할 일(메모·마감일에 묶이지 않은) 목록 화면. */
class TodoViewModel(
    observeGeneralTasks: ObserveGeneralTasksUseCase,
    private val addTask: AddTaskUseCase,
    private val toggleTask: ToggleTaskUseCase,
    private val deleteTask: DeleteTaskUseCase,
) : ViewModel() {

    val tasks: StateFlow<List<Task>> = observeGeneralTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun add(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch { addTask(text, null, null) }
    }

    fun toggle(id: String) {
        viewModelScope.launch { toggleTask(id) }
    }

    fun remove(id: String) {
        viewModelScope.launch { deleteTask(id) }
    }
}
