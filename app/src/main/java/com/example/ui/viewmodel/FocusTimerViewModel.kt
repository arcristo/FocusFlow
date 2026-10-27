package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.SessionType
import com.example.data.model.Task
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FocusTimerViewModel : ViewModel() {

    private val _currentType = MutableStateFlow(SessionType.POMODORO)
    val currentType = _currentType.asStateFlow()

    private val _totalSeconds = MutableStateFlow(25 * 60L)
    val totalSeconds = _totalSeconds.asStateFlow()

    private val _secondsRemaining = MutableStateFlow(25 * 60L)
    val secondsRemaining = _secondsRemaining.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()

    private val _selectedTask = MutableStateFlow<Task?>(null)
    val selectedTask = _selectedTask.asStateFlow()

    private val _completedPomodoros = MutableStateFlow(0)
    val completedPomodoros = _completedPomodoros.asStateFlow()

    private var timerJob: Job? = null

    var onSessionCompleted: ((SessionType, Long, Task?) -> Unit)? = null

    fun selectTask(task: Task?) {
        _selectedTask.value = task
    }

    fun switchSessionType(type: SessionType) {
        pause()
        _currentType.value = type
        val secs = type.defaultMinutes * 60L
        _totalSeconds.value = secs
        _secondsRemaining.value = secs
    }

    fun start() {
        if (_isRunning.value) return
        _isRunning.value = true
        timerJob = viewModelScope.launch {
            while (_isRunning.value && _secondsRemaining.value > 0) {
                delay(1000L)
                _secondsRemaining.value -= 1
            }
            if (_secondsRemaining.value <= 0) {
                _isRunning.value = false
                val finishedType = _currentType.value
                val durationMins = _totalSeconds.value / 60L
                if (finishedType == SessionType.POMODORO) {
                    _completedPomodoros.value += 1
                }
                onSessionCompleted?.invoke(finishedType, durationMins, _selectedTask.value)

                // Auto switch to break if Pomodoro finished
                if (finishedType == SessionType.POMODORO) {
                    if (_completedPomodoros.value % 4 == 0) {
                        switchSessionType(SessionType.LONG_BREAK)
                    } else {
                        switchSessionType(SessionType.SHORT_BREAK)
                    }
                } else {
                    switchSessionType(SessionType.POMODORO)
                }
            }
        }
    }

    fun pause() {
        _isRunning.value = false
        timerJob?.cancel()
        timerJob = null
    }

    fun togglePlayPause() {
        if (_isRunning.value) pause() else start()
    }

    fun reset() {
        pause()
        _secondsRemaining.value = _totalSeconds.value
    }

    fun addFiveMinutes() {
        _secondsRemaining.value += 5 * 60L
        _totalSeconds.value += 5 * 60L
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
