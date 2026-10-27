package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.FocusSession
import com.example.data.model.SessionType
import com.example.data.model.Task
import com.example.data.model.TaskCategory
import com.example.data.model.TaskPriority
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class TaskRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun createTask_validPayload_createsDocumentAndReturnsId() {
    runBlocking {
      val uid = signInTestUser(ALICE_EMAIL)
      val repository = TaskRepository(firestore)

      val task = Task(
        id = "task_" + UUID.randomUUID().toString().replace("-", ""),
        userId = uid,
        title = "Minha Tarefa de Teste",
        description = "Descricao detalhada",
        priority = TaskPriority.HIGH.name,
        category = TaskCategory.TRABALHO.name,
        completed = false,
        estimatedMinutes = 45L
      )

      val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.createTask(uid, task)
      }
      assertTrue(createResult.isSuccess)
      val taskId = createResult.getOrThrow()
      assertTrue(taskId.isNotEmpty())
    }
  }

  @Test
  fun getUserTasks_authenticatedOwner_returnsMatchingTasks() {
    runBlocking {
      val uid = signInTestUser(ALICE_EMAIL)
      val repository = TaskRepository(firestore)

      val taskId = "task_" + UUID.randomUUID().toString().replace("-", "")
      val task = Task(
        id = taskId,
        userId = uid,
        title = "Tarefa de Observacao",
        priority = TaskPriority.MEDIUM.name,
        category = TaskCategory.ESTUDO.name,
        completed = false
      )

      val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.createTask(uid, task)
      }
      assertTrue(createResult.isSuccess)

      val fetched = withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.getTask(uid, taskId).getOrThrow()
      }
      assertNotNull(fetched)
      assertEquals(taskId, fetched?.id)
      assertEquals("Tarefa de Observacao", fetched?.title)
    }
  }

  @Test
  fun observeTasks_authenticatedOwner_emitsRealtimeUpdates() {
    runBlocking {
      val uid = signInTestUser(ALICE_EMAIL)
      val repository = TaskRepository(firestore)

      val taskId = "task_" + UUID.randomUUID().toString().replace("-", "")
      val task = Task(
        id = taskId,
        userId = uid,
        title = "Realtime Task",
        priority = TaskPriority.URGENT.name,
        category = TaskCategory.SAUDE.name,
        completed = false
      )

      withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.createTask(uid, task).getOrThrow()
      }

      val emittedList = withTimeout(FLOW_TIMEOUT_MS) {
        repository.observeTasks(uid).first { list -> list.any { it.id == taskId } }
      }
      assertTrue(emittedList.any { it.id == taskId })
    }
  }

  @Test
  fun getTask_crossUserAccess_failsWithPermissionDenied() {
    runBlocking {
      val aliceUid = signInTestUser(ALICE_EMAIL)
      val aliceRepo = TaskRepository(firestore)

      val taskId = "task_" + UUID.randomUUID().toString().replace("-", "")
      val task = Task(
        id = taskId,
        userId = aliceUid,
        title = "Alice Secret Task",
        priority = TaskPriority.LOW.name,
        category = TaskCategory.PESSOAL.name,
        completed = false
      )
      withTimeout(DEFAULT_TIMEOUT_MS) {
        aliceRepo.createTask(aliceUid, task).getOrThrow()
      }

      // Sign in as Bob and attempt to read Alice's task
      signInTestUser(BOB_EMAIL)
      val bobRepo = TaskRepository(firestore)

      val result = withTimeout(DEFAULT_TIMEOUT_MS) {
        bobRepo.getTask(aliceUid, taskId)
      }
      assertTrue(result.isFailure)
      val exception = result.exceptionOrNull()
      assertTrue(exception is FirebaseFirestoreException)
      val fError = exception as FirebaseFirestoreException
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fError.code)
    }
  }

  @Test
  fun observeTasks_unauthenticatedUser_failsWithPermissionDenied() {
    runBlocking {
      auth.signOut()
      val repository = TaskRepository(firestore)

      try {
        withTimeout(FLOW_TIMEOUT_MS) {
          repository.observeTasks("some_random_user_123").first()
        }
        fail("Expected exception for unauthenticated read")
      } catch (e: Exception) {
        val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
          "Expected nested FirebaseFirestoreException, got $e"
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
      }
    }
  }

  @Test
  fun createSession_validPayload_succeeds() {
    runBlocking {
      val uid = signInTestUser(ALICE_EMAIL)
      val repository = TaskRepository(firestore)

      val sessionId = "session_" + UUID.randomUUID().toString().replace("-", "")
      val session = FocusSession(
        id = sessionId,
        userId = uid,
        durationMinutes = 25L,
        sessionType = SessionType.POMODORO.name,
        completed = true,
        notes = "Sessao produtiva de foco"
      )

      val result = withTimeout(DEFAULT_TIMEOUT_MS) {
        repository.createSession(uid, session)
      }
      assertTrue(result.isSuccess)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice@focusflow.test"
    const val BOB_EMAIL = "bob@focusflow.test"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 4000L
  }
}
