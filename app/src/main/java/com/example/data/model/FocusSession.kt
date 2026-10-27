package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class SessionType(val displayName: String, val defaultMinutes: Long) {
    POMODORO("Foco Pomodoro", 25L),
    SHORT_BREAK("Pausa Curta", 5L),
    LONG_BREAK("Pausa Longa", 15L)
}

@IgnoreExtraProperties
data class FocusSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val taskId: String = "",
    val durationMinutes: Long = 25L,
    val sessionType: String = SessionType.POMODORO.name,
    val completed: Boolean = true,
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
