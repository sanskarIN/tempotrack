package in.sanskar.tempotrack.data

import in.sanskar.tempotrack.domain.StopwatchCheckpoint
import in.sanskar.tempotrack.domain.StopwatchCheckpointValidation
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface ActiveStopwatchRepository {
    suspend fun load(): StopwatchCheckpoint?
    suspend fun save(checkpoint: StopwatchCheckpoint)
    suspend fun clear()
}

class JsonActiveStopwatchRepository(
    private val storage: StringStorage,
    private val codec: ActiveStopwatchStoreCodec = ActiveStopwatchStoreCodec(),
) : ActiveStopwatchRepository {
    private val mutex = Mutex()

    override suspend fun load(): StopwatchCheckpoint? = mutex.withLock {
        val raw = storage.read()?.takeIf { it.isNotBlank() } ?: return@withLock null
        val decoded = codec.decode(raw) ?: return@withLock null
        val checkpoint = decoded.checkpoint
        if (!StopwatchCheckpointValidation.isValid(checkpoint)) return@withLock null

        if (decoded.needsMigration) {
            storage.write(codec.encode(checkpoint))
        }
        checkpoint
    }

    override suspend fun save(checkpoint: StopwatchCheckpoint) = mutex.withLock {
        val errors = StopwatchCheckpointValidation.validate(checkpoint)
        require(errors.isEmpty()) { "Invalid stopwatch checkpoint: ${errors.joinToString()}" }
        storage.write(codec.encode(checkpoint))
    }

    override suspend fun clear() = mutex.withLock {
        storage.clear()
    }
}
