package in.sanskar.tempotrack.ios

import in.sanskar.tempotrack.data.StringStorage
import in.sanskar.tempotrack.domain.MonotonicClock
import in.sanskar.tempotrack.domain.WallClock
import kotlin.math.roundToLong
import platform.Foundation.NSDate
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSUserDefaults

class IosStringStorage(
    private val key: String,
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : StringStorage {
    override suspend fun read(): String? = defaults.stringForKey(key)

    override suspend fun write(content: String) {
        defaults.setObject(content, forKey = key)
    }

    override suspend fun clear() {
        defaults.removeObjectForKey(key)
    }
}

fun iosMonotonicClock(): MonotonicClock = MonotonicClock {
    (NSProcessInfo.processInfo.systemUptime * NANOS_PER_SECOND_DOUBLE).roundToLong()
}

fun iosWallClock(): WallClock = WallClock {
    (NSDate().timeIntervalSince1970 * MILLIS_PER_SECOND_DOUBLE).roundToLong()
}

private const val NANOS_PER_SECOND_DOUBLE = 1_000_000_000.0
private const val MILLIS_PER_SECOND_DOUBLE = 1_000.0
