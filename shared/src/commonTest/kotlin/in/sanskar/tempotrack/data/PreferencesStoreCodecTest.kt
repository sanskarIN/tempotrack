package in.sanskar.tempotrack.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json

class PreferencesStoreCodecTest {
    private val codec = PreferencesStoreCodec()

    @Test
    fun versionedPreferencesRoundTrip() {
        val preferences = AppPreferences(
            theme = ThemePreference.DARK,
            largeControls = true,
            reducedMotion = true,
            onboardingCompleted = true,
            miniStopwatchVisible = true,
            keyboardShortcutsEnabled = false,
        )

        val decoded = requireNotNull(codec.decode(codec.encode(preferences)))

        assertEquals(preferences, decoded.preferences)
        assertFalse(decoded.needsMigration)
    }

    @Test
    fun legacyPreferencesAreDetectedForMigration() {
        val preferences = AppPreferences(theme = ThemePreference.LIGHT, onboardingCompleted = true)
        val legacy = Json.encodeToString(AppPreferences.serializer(), preferences)
        val decoded = requireNotNull(codec.decode(legacy))

        assertEquals(preferences, decoded.preferences)
        assertTrue(decoded.needsMigration)
    }

    @Test
    fun olderPreferencesDefaultShortcutsToEnabled() {
        val legacy = """{"theme":"SYSTEM","onboardingCompleted":true}"""
        val decoded = requireNotNull(codec.decode(legacy))

        assertTrue(decoded.preferences.keyboardShortcutsEnabled)
    }

    @Test
    fun unsupportedFutureSchemaFailsClosed() {
        val future = """{"schemaVersion":999,"preferences":{"theme":"SYSTEM"}}"""

        assertNull(codec.decode(future))
    }
}
