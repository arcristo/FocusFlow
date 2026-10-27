package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.FocusSession
import com.example.data.model.SessionType
import com.example.data.model.Task
import com.example.data.model.TaskCategory
import com.example.data.model.TaskPriority
import com.example.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "TaskViewModel"
private const val STOP_TIMEOUT_MILLIS = 5000L

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class TaskViewModel(
    private val repository: TaskRepository,
    private val currentUserId: String
) : ViewModel() {

    // Filter states
    val searchQuery = MutableStateFlow("")
    val selectedCategory = MutableStateFlow<TaskCategory?>(null)
    val selectedPriority = MutableStateFlow<TaskPriority?>(null)
    val showOnlyPending = MutableStateFlow(false)

    // Raw tasks stream
    val rawTasks: StateFlow<UiState<List<Task>>> = repository.observeTasks(currentUserId)
        .map<List<Task>, UiState<List<Task>>> { list ->
            // Sort: pending first, then by priority (URGENT > HIGH > MEDIUM > LOW)
            val sorted = list.sortedWith(
                compareBy<Task> { it.completed }
                    .thenByDescending {
                        when (it.priority) {
                            TaskPriority.URGENT.name -> 4
                            TaskPriority.HIGH.name -> 3
                            TaskPriority.MEDIUM.name -> 2
                            else -> 1
                        }
                    }
                    .thenByDescending { it.createdAt?.seconds ?: 0L }
            )
            UiState.Success(sorted)
        }
        .catch { error ->
            Log.w(TAG, "Error observing tasks", error)
            emit(UiState.Error(error.message ?: "Falha ao carregar tarefas"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    // Filtered tasks stream for UI
    val filteredTasks: StateFlow<UiState<List<Task>>> = combine(
        rawTasks,
        searchQuery,
        selectedCategory,
        selectedPriority,
        showOnlyPending
    ) { state, query, category, priority, onlyPending ->
        when (state) {
            is UiState.Success -> {
                val filtered = state.data.filter { task ->
                    val matchesQuery = query.isBlank() ||
                            task.title.contains(query, ignoreCase = true) ||
                            task.description.contains(query, ignoreCase = true)
                    val matchesCategory = category == null || task.category == category.name
                    val matchesPriority = priority == null || task.priority == priority.name
                    val matchesPending = !onlyPending || !task.completed

                    matchesQuery && matchesCategory && matchesPriority && matchesPending
                }
                UiState.Success(filtered)
            }
            is UiState.Error -> state
            is UiState.Loading -> UiState.Loading
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
    )

    // Sessions stream
    val sessions: StateFlow<UiState<List<FocusSession>>> = repository.observeSessions(currentUserId)
        .map<List<FocusSession>, UiState<List<FocusSession>>> { list ->
            UiState.Success(list.sortedByDescending { it.createdAt?.seconds ?: 0L })
        }
        .catch { error ->
            Log.w(TAG, "Error observing focus sessions", error)
            emit(UiState.Error(error.message ?: "Falha ao carregar sessões de foco"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun createTask(
        title: String,
        description: String,
        priority: TaskPriority,
        category: TaskCategory,
        dueDate: String,
        estimatedMinutes: Long
    ) {
        if (title.isBlank()) {
            _userMessage.value = "O título da tarefa não pode estar vazio"
            return
        }

        viewModelScope.launch {
            val task = Task(
                userId = currentUserId,
                title = title.trim(),
                description = description.trim(),
                priority = priority.name,
                category = category.name,
                completed = false,
                dueDate = dueDate.trim(),
                estimatedMinutes = estimatedMinutes,
                focusTimeSpentMinutes = 0L
            )
            val result = repository.createTask(currentUserId, task)
            if (result.isSuccess) {
                _userMessage.value = "Tarefa criada com sucesso!"
            } else {
                _userMessage.value = "Erro ao criar tarefa: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun toggleTaskCompleted(task: Task) {
        viewModelScope.launch {
            val updated = task.copy(completed = !task.completed)
            val result = repository.updateTask(currentUserId, updated)
            if (result.isSuccess) {
                _userMessage.value = if (updated.completed) "Tarefa concluída! Parabéns!" else "Tarefa reaberta"
            } else {
                _userMessage.value = "Erro ao atualizar status: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            val result = repository.updateTask(currentUserId, task)
            if (result.isSuccess) {
                _userMessage.value = "Tarefa atualizada!"
            } else {
                _userMessage.value = "Erro ao atualizar: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            val result = repository.deleteTask(currentUserId, taskId)
            if (result.isSuccess) {
                _userMessage.value = "Tarefa excluída"
            } else {
                _userMessage.value = "Erro ao excluir: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun recordFocusSession(
        taskId: String?,
        durationMinutes: Long,
        sessionType: SessionType,
        notes: String
    ) {
        viewModelScope.launch {
            val session = FocusSession(
                userId = currentUserId,
                taskId = taskId ?: "",
                durationMinutes = durationMinutes,
                sessionType = sessionType.name,
                completed = true,
                notes = notes
            )
            repository.createSession(currentUserId, session)

            // If linked to a task, increment the focused time on the task
            if (!taskId.isNullOrBlank()) {
                val taskResult = repository.getTask(currentUserId, taskId)
                val currentTask = taskResult.getOrNull()
                if (currentTask != null) {
                    val newSpent = currentTask.focusTimeSpentMinutes + durationMinutes
                    repository.updateTask(currentUserId, currentTask.copy(focusTimeSpentMinutes = newSpent))
                }
            }
            _userMessage.value = "Sessão de foco de $durationMinutes min registrada com sucesso!"
        }
    }
}
