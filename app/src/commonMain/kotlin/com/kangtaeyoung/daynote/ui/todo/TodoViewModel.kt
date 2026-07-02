package com.kangtaeyoung.daynote.ui.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kangtaeyoung.daynote.domain.model.Task
import com.kangtaeyoung.daynote.domain.usecase.AddTaskUseCase
import com.kangtaeyoung.daynote.domain.usecase.DeleteTaskUseCase
import com.kangtaeyoung.daynote.domain.usecase.ObserveGeneralTasksUseCase
import com.kangtaeyoung.daynote.domain.usecase.ToggleTaskUseCase
import com.kangtaeyoung.daynote.ui.components.Period
import com.kangtaeyoung.daynote.ui.components.cutoffMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 일반 할 일(메모에 묶이지 않은) 목록 화면. 기간 필터는 메모 목록과 같은 [Period] 를 쓴다. */
class TodoViewModel(
    observeGeneralTasks: ObserveGeneralTasksUseCase,
    private val addTask: AddTaskUseCase,
    private val toggleTask: ToggleTaskUseCase,
    private val deleteTask: DeleteTaskUseCase,
) : ViewModel() {

    private val _period = MutableStateFlow(Period.ALL)
    val period: StateFlow<Period> = _period

    fun setPeriod(period: Period) {
        _period.value = period
    }

    /** 기간 필터 적용된 할 일. 기준 날짜는 마감일([Task.dueDate]), 없으면 작성일. */
    val tasks: StateFlow<List<Task>> = combine(observeGeneralTasks(), _period) { list, period ->
        val cutoff = period.cutoffMillis() ?: return@combine list
        list.filter { (it.dueDate ?: it.createdAt) >= cutoff }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
