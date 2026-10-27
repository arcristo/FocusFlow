package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.FocusSession
import com.example.data.model.Task
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class TaskRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    fun observeTasks(userId: String): Flow<List<Task>> {
        val path = "users/$userId/tasks"
        return db.collection("users").document(userId).collection("tasks")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Task::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun createTask(userId: String, task: Task): Result<String> {
        val collection = db.collection("users").document(userId).collection("tasks")
        val docRef = if (task.id.isNotBlank()) collection.document(task.id) else collection.document()
        val payload = mutableMapOf<String, Any>(
            "userId" to userId,
            "title" to task.title,
            "priority" to task.priority,
            "category" to task.category,
            "completed" to task.completed,
            "estimatedMinutes" to task.estimatedMinutes,
            "focusTimeSpentMinutes" to task.focusTimeSpentMinutes,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (task.description.isNotBlank()) {
            payload["description"] = task.description
        }
        if (task.dueDate.isNotBlank()) {
            payload["dueDate"] = task.dueDate
        }

        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateTask(userId: String, task: Task): Result<Unit> {
        require(task.id.isNotBlank()) { "Task ID cannot be blank for update" }
        val docRef = db.collection("users").document(userId).collection("tasks").document(task.id)
        val updatePayload = mutableMapOf<String, Any>(
            "title" to task.title,
            "priority" to task.priority,
            "category" to task.category,
            "completed" to task.completed,
            "estimatedMinutes" to task.estimatedMinutes,
            "focusTimeSpentMinutes" to task.focusTimeSpentMinutes,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (task.description.isNotBlank()) {
            updatePayload["description"] = task.description
        }
        if (task.dueDate.isNotBlank()) {
            updatePayload["dueDate"] = task.dueDate
        }

        return try {
            docRef.update(updatePayload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteTask(userId: String, taskId: String): Result<Unit> {
        val docRef = db.collection("users").document(userId).collection("tasks").document(taskId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getTask(userId: String, taskId: String): Result<Task?> {
        val docRef = db.collection("users").document(userId).collection("tasks").document(taskId)
        return try {
            val snapshot = docRef.get().await()
            Result.success(snapshot.toObject(Task::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    fun observeSessions(userId: String): Flow<List<FocusSession>> {
        val path = "users/$userId/sessions"
        return db.collection("users").document(userId).collection("sessions")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(FocusSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                throw error
            }
    }

    suspend fun createSession(userId: String, session: FocusSession): Result<String> {
        val collection = db.collection("users").document(userId).collection("sessions")
        val docRef = if (session.id.isNotBlank()) collection.document(session.id) else collection.document()
        val payload = mutableMapOf<String, Any>(
            "userId" to userId,
            "durationMinutes" to session.durationMinutes,
            "sessionType" to session.sessionType,
            "completed" to session.completed,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (session.taskId.isNotBlank()) {
            payload["taskId"] = session.taskId
        }
        if (session.notes.isNotBlank()) {
            payload["notes"] = session.notes
        }

        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }
}
