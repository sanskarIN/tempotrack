package in.sanskar.tempotrack

import in.sanskar.tempotrack.data.JsonActiveStopwatchRepository
import in.sanskar.tempotrack.data.JsonSessionRepository
import in.sanskar.tempotrack.data.SessionCodec
import in.sanskar.tempotrack.data.SessionImportResult
import in.sanskar.tempotrack.data.SessionImporter
import in.sanskar.tempotrack.data.StringStorage
import in.sanskar.tempotrack.domain.MonotonicClock
import in.sanskar.tempotrack.domain.NANOS_PER_SECOND
import in.sanskar.tempotrack.domain.StopwatchEngine
import in.sanskar.tempotrack.domain.StopwatchSession
import in.sanskar.tempotrack.domain.StopwatchStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class StopwatchJourneyTest {
    @Test
    fun completeSessionSurvivesJsonBackupAndRestore() = runTest {
        val clock = JourneyClock()
        val engine = StopwatchEngine(clock)

        engine.start()
        clock.advanceSeconds(2)
        engine.lap()
        clock.advanceSeconds(3)
        engine.lap()
        engine.pause()

        val finished = engine.snapshot()
        assertEquals(StopwatchStatus.PAUSED, finished.status)
        assertEquals(5L * NANOS_PER_SECOND, finished.elapsedNanos)
        assertEquals(2, finished.laps.size)

        val sourceRepository = JsonSessionRepository(MemoryStringStorage())
        val session = StopwatchSession(
            id = "journey-session-1",
            name = "Five second regression journey",
            createdAtEpochMillis = 1_700_000_000_000L,
            durationNanos = finished.elapsedNanos,
            laps = finished.laps,
        )
        sourceRepository.upsert(session)

        val saved = sourceRepository.all()
        assertEquals(listOf(session), saved)

        val portableJson = SessionCodec.toJson(saved)
        val parsed = assertIs<SessionImportResult.Success>(SessionImporter.fromJson(portableJson))
        assertEquals(listOf(session), parsed.sessions)

        val restoredRepository = JsonSessionRepository(MemoryStringStorage())
        restoredRepository.replaceAll(parsed.sessions)

        val restored = restoredRepository.all()
        assertEquals(listOf(session), restored)
        assertEquals(2L * NANOS_PER_SECOND, restored.single().laps[0].splitNanos)
        assertEquals(3L * NANOS_PER_SECOND, restored.single().laps[1].splitNanos)
        assertTrue(SessionCodec.toCsv(restored).contains("Five second regression journey"))
    }

    @Test
    fun pausedCheckpointSurvivesRepositoryRestartAndResumes() = runTest {
        val clock = JourneyClock()
        val activeStorage = MemoryStringStorage()
        val activeRepository = JsonActiveStopwatchRepository(activeStorage)
        val firstEngine = StopwatchEngine(clock)

        firstEngine.start()
        clock.advanceSeconds(4)
        firstEngine.pause()
        activeRepository.save(firstEngine.checkpoint())

        val restoredCheckpoint = requireNotNull(activeRepository.load())
        val restoredEngine = StopwatchEngine(clock, restoredCheckpoint)
        assertEquals(StopwatchStatus.PAUSED, restoredEngine.snapshot().status)
        assertEquals(4L * NANOS_PER_SECOND, restoredEngine.snapshot().elapsedNanos)

        restoredEngine.resume()
        clock.advanceSeconds(2)
        restoredEngine.pause()

        assertEquals(6L * NANOS_PER_SECOND, restoredEngine.snapshot().elapsedNanos)
    }
}

private class JourneyClock(
    private var nowNanos: Long = 0L,
) : MonotonicClock {
    override fun nowNanos(): Long = nowNanos

    fun advanceSeconds(seconds: Long) {
        nowNanos += seconds * NANOS_PER_SECOND
    }
}

private class MemoryStringStorage : StringStorage {
    private var value: String? = null

    override suspend fun read(): String? = value

    override suspend fun write(content: String) {
        value = content
    }

    override suspend fun clear() {
        value = null
    }
}
