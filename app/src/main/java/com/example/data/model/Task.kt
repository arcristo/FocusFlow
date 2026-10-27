package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class TaskPriority(val displayName: String) {
    LOW("Baixa"),
    MEDIUM("Média"),
    HIGH("Alta"),
    URGENT("Urgente")
}

enum class TaskCategory(val displayName: String) {
    TRABALHO("Trabalho"),
    ESTUDO("Estudo"),
    PESSOAL("Pessoal"),
    SAUDE("Saúde"),
    FINANCAS("Finanças"),
    OUTRO("Geral")
}

@IgnoreExtraProperties
data class Task(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val priority: String = TaskPriority.MEDIUM.name,
    val category: String = TaskCategory.TRABALHO.name,
    val completed: Boolean = false,
    val dueDate: String = "",
    val estimatedMinutes: Long = 25L,
    val focusTimeSpentMinutes: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
