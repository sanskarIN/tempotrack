package in.sanskar.tempotrack.data

import in.sanskar.tempotrack.domain.Lap
import in.sanskar.tempotrack.domain.StopwatchSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

class SessionRepositoryTest {
    @Test
    fun upsertPersistsAndSortsNewestFirst() = runTest {
        val storage = InMemoryStringStorage()
        val repository = JsonSessionRepository(storage)

        repository.upsert(session(id = "old", createdAt = 10L))
        repository.upsert(session(id = "new", createdAt = 20L))

        assertEquals(listOf("new", "old"), repository.all().map(StopwatchSession::id))
    }

    @Test
    fun identicalUpsertDoesNotRewriteStorage() = runTest {
        val storage = InMemoryStringStorage()
        val repository = JsonSessionRepository(storage)
        val saved = session(id = "one", createdAt = 10L)
        repository.upsert(saved)
        val writesAfterSave = storage.writeCount

        repository.upsert(saved)

        assertEquals(writesAfterSave, storage.writeCount)
        assertEquals(listOf(saved), repository.all())
    }

    @Test
    fun concurrentUpsertsPreserveAllSessionsAndSerializeWrites() = runTest {
        val storage = ConcurrentWriteDetectingStorage()
        val repository = JsonSessionRepository(storage)

        List(20) { index ->
            launch {
                repository.upsert(session(id = "session-$index", createdAt = index.toLong()))
            }
        }.joinAll()

        val stored = repository.all()
        assertEquals(20, stored.size)
        assertEquals((0 until 20).map { "session-$it" }.toSet(), stored.map(StopwatchSession::id).toSet())
    }

    @Test
    fun renameUpdatesOnlyRequestedSession() = runTest {
        val repository = JsonSessionRepository(InMemoryStringStorage())
        repository.upsert(session(id = "one", createdAt = 10L))
        repository.upsert(session(id = "two", createdAt = 20L))

        assertTrue(repository.rename("one", "  Focus sprint  "))

        val sessions = repository.all()
        assertEquals("Focus sprint", sessions.first { it.id == "one" }.name)
        assertEquals("Session two", sessions.first { it.id == "two" }.name)
    }

    @Test
    fun renameSameNameDoesNotRewriteStorage() = runTest {
        val storage = InMemoryStringStorage()
        val repository = JsonSessionRepository(storage)
        repository.upsert(session(id = "one", createdAt = 10L))
        val writesAfterSave = storage.writeCount

        assertTrue(repository.rename("one", "  Session one  "))

        assertEquals(writesAfterSave, storage.writeCount)
    }

    @Test
    fun renameReturnsFalseForMissingSession() = runTest {
        val repository = JsonSessionRepository(InMemoryStringStorage())

        assertFalse(repository.rename("missing", "New name"))
    }

    @Test
    fun renameRejectsBlankName() = runTest {
        val repository = JsonSessionRepository(InMemoryStringStorage())
        repository.upsert(session(id = "one", createdAt = 10L))

        assertFailsWith<IllegalArgumentException> {
            repository.rename("one", "   ")
        }
    }

    @Test
    fun deletingMissingSessionDoesNotRewriteStorage() = runTest {
        val storage = InMemoryStringStorage()
        val repository = JsonSessionRepository(storage)
        repository.upsert(session(id = "one", createdAt = 10L))
        val writesAfterSave = storage.writeCount

        repository.delete("missing")

        assertEquals(writesAfterSave, storage.writeCount)
        assertEquals(listOf("one"), repository.all().map(StopwatchSession::id))
    }

    @Test
    fun identicalReplaceAllDoesNotRewriteStorage() = runTest {
        val storage = InMemoryStringStorage()
        val repository = JsonSessionRepository(storage)
        val old = session(id = "old", createdAt = 10L)
        val latest = session(id = "latest", createdAt = 20L)
        repository.replaceAll(listOf(old, latest))
        val writesAfterReplace = storage.writeCount

        repository.replaceAll(listOf(old, latest))

        assertEquals(writesAfterReplace, storage.writeCount)
        assertEquals(listOf(latest, old), repository.all())
    }

    @Test
    fun replaceAllRejectsDuplicateIds() = runTest {
        val repository = JsonSessionRepository(InMemoryStringStorage())
        val duplicate = session(id = "same", createdAt = 10L)

        assertFailsWith<IllegalArgumentException> {
            repository.replaceAll(listOf(duplicate, duplicate.copy(createdAtEpochMillis = 20L)))
        }
    }

    @Test
    fun malformedStorageFailsClosedWithoutRewrite() = runTest {
        val original = "{not-json"
        val storage = InMemoryStringStorage(original)
        val repository = JsonSessionRepository(storage)

        assertFailsWith<SessionStoreCorruptionException> {
            repository.all()
        }
        assertEquals(original, storage.value)
    }

    @Test
    fun invalidStoredSessionFailsClosedWithoutDroppingOnlyBadRecord() = runTest {
        val valid = session(id = "valid", createdAt = 10L)
        val invalid = session(id = "invalid", createdAt = 20L).copy(
            laps = listOf(Lap(index = 2, splitNanos = 1_000L, totalNanos = 1_000L)),
        )
        val original = SessionStoreCodec().encode(listOf(valid, invalid))
        val storage = InMemoryStringStorage(original)
        val repository = JsonSessionRepository(storage)

        assertFailsWith<SessionStoreCorruptionException> {
            repository.all()
        }
        assertEquals(original, storage.value)
    }

    private fun session(id: String, createdAt: Long): StopwatchSession = StopwatchSession(
        id = id,
        name = "Session $id",
        createdAtEpochMillis = createdAt,
        durationNanos = 1_000L,
        laps = listOf(Lap(index = 1, splitNanos = 1_000L, totalNanos = 1_000L)),
    )
}

private class InMemoryStringStorage(
    var value: String? = null,
) : StringStorage {
    var writeCount: Int = 0
        private set

    override suspend fun read(): String? = value

    override suspend fun write(content: String) {
        writeCount += 1
        value = content
    }

    override suspend fun clear() {
        value = null
    }
}
