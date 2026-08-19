package in.sanskar.tempotrack.data

import in.sanskar.tempotrack.domain.StopwatchCheckpoint
import in.sanskar.tempotrack.domain.StopwatchCheckpointValidation

interface ActiveStopwatchRepository {
    suspend fun load(): StopwatchCheckpoint?
    suspend fun save(checkpoint: StopwatchCheckpoint)
    suspend fun clear()
}

class JsonActiveStopwatchRepository(
    private val storage: StringStorage,
    private val codec: ActiveStopwatchStoreCodec = ActiveStopwatchStoreCodec(),
) : ActiveStopwatchRepository {
    override suspend fun load(): StopwatchCheckpoint? {
        val raw = storage.read()?.takeIf { it.isNotBlank() } ?: return null
        val decoded = codec.decode(raw) ?: return null
        val checkpoint = decoded.checkpoint
        if (!StopwatchCheckpointValidation.isValid(checkpoint)) return null

        if (decoded.needsMigration) {
            storage.write(codec.encode(checkpoint))
        }
        return checkpoint
    }

    override suspend fun save(checkpoint: StopwatchCheckpoint) {
        val errors = StopwatchCheckpointValidation.validate(checkpoint)
        require(errors.isEmpty()) { "Invalid stopwatch checkpoint: ${errors.joinToString()}" }
        storage.write(codec.encode(checkpoint))
    }

    override suspend fun clear() {
        storage.clear()
    }
}
