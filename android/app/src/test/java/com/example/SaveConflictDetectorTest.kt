package com.example

import com.example.data.cloud.CloudSaveBundle
import com.example.data.cloud.ConflictKeepChoice
import com.example.data.cloud.SaveConflictDetector
import com.example.data.cloud.SaveConflictSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SaveConflictDetectorTest {

    private fun sampleEntries(coins: Int, gamesPlayedJson: String = """{"ttt":{"played":3,"won":2}}"""): Map<String, String> =
        mapOf(
            "starchess.starCoins" to coins.toString(),
            "starchess.v7.statsByGame" to gamesPlayedJson,
            "starchess.lang" to "en",
        )

    private fun sampleCloudBundle(
        id: String = "primary",
        coins: Int,
        games: Long,
        clientSavedAtMs: Long,
        lang: String = "zh-Hant",
    ): CloudSaveBundle {
        val entries = mapOf(
            "starchess.starCoins" to coins.toString(),
            "starchess.v7.statsByGame" to """{"ttt":{"played":$games,"won":$games}}""",
            "starchess.lang" to lang,
        )
        return CloudSaveBundle(
            id = id,
            userId = "user_1",
            label = "Primary Sync",
            entries = entries,
            starCoins = coins.toLong(),
            eloPeak = 1200L,
            achievementsCount = 2L,
            gamesPlayed = games,
            preferredLanguage = lang,
            deviceName = "Pixel Tablet",
            clientSavedAtMs = clientSavedAtMs,
        )
    }

    @Test
    fun `canonical comparison ignores UI language and theme keys`() {
        val local = sampleEntries(coins = 250) + mapOf("starchess.lang" to "en", "starchess.theme" to "nebula")
        val cloud = sampleEntries(coins = 250) + mapOf("starchess.lang" to "ja", "starchess.theme" to "solar")
        assertFalse(SaveConflictDetector.arePayloadsDifferent(local, cloud))
    }

    @Test
    fun `detectAutomaticConflict triggers when both Room and Firestore have divergent data without baseline sync`() {
        val local = sampleEntries(coins = 200, gamesPlayedJson = """{"ttt":{"played":4,"won":3}}""")
        val cloud = sampleCloudBundle(coins = 450, games = 8L, clientSavedAtMs = 5_000L)

        val conflict = SaveConflictDetector.detectAutomaticConflict(
            localEntries = local,
            localUpdatedAtMs = 4_000L,
            snapshotsCount = 2,
            cloudSaves = listOf(cloud),
            lastCloudSyncMs = 0L,
            snoozedFingerprints = emptySet(),
        )

        assertNotNull(conflict)
        assertEquals(SaveConflictSource.AUTO_DIVERGENCE, conflict!!.source)
        assertEquals(ConflictKeepChoice.CLOUD, conflict.recommendedChoice)
        assertEquals(200, conflict.local.starCoins)
        assertEquals(450, conflict.cloud.starCoins)
        assertEquals(2, conflict.local.snapshotsCount)
    }

    @Test
    fun `detectAutomaticConflict respects snoozed conflict fingerprints`() {
        val local = sampleEntries(coins = 200)
        val cloud = sampleCloudBundle(coins = 450, games = 8L, clientSavedAtMs = 5_000L)

        val first = SaveConflictDetector.detectAutomaticConflict(
            localEntries = local,
            localUpdatedAtMs = 4_000L,
            snapshotsCount = 1,
            cloudSaves = listOf(cloud),
            lastCloudSyncMs = 0L,
            snoozedFingerprints = emptySet(),
        )!!

        val second = SaveConflictDetector.detectAutomaticConflict(
            localEntries = local,
            localUpdatedAtMs = 4_000L,
            snapshotsCount = 1,
            cloudSaves = listOf(cloud),
            lastCloudSyncMs = 0L,
            snoozedFingerprints = setOf(first.conflictFingerprint),
        )
        assertNull(second)
    }

    @Test
    fun `detectAutomaticConflict does not fire when device advanced locally after synced cloud timestamp`() {
        val local = sampleEntries(coins = 600, gamesPlayedJson = """{"ttt":{"played":10,"won":8}}""")
        val cloud = sampleCloudBundle(coins = 450, games = 8L, clientSavedAtMs = 5_000L)

        val conflict = SaveConflictDetector.detectAutomaticConflict(
            localEntries = local,
            localUpdatedAtMs = 9_000L,
            snapshotsCount = 1,
            cloudSaves = listOf(cloud),
            lastCloudSyncMs = 5_500L,
            snoozedFingerprints = emptySet(),
        )
        assertNull(conflict)
    }

    @Test
    fun `checkManualBackupConflict intercepts overwriting higher or newer cloud progress`() {
        val local = sampleEntries(coins = 120, gamesPlayedJson = """{"ttt":{"played":2,"won":1}}""")
        val cloud = sampleCloudBundle(coins = 500, games = 12L, clientSavedAtMs = 10_000L)

        val conflict = SaveConflictDetector.checkManualBackupConflict(
            localEntries = local,
            localUpdatedAtMs = 8_000L,
            snapshotsCount = 0,
            cloudSaves = listOf(cloud),
            lastCloudSyncMs = 6_000L,
        )

        assertNotNull(conflict)
        assertEquals(SaveConflictSource.MANUAL_BACKUP, conflict!!.source)
        assertEquals(ConflictKeepChoice.CLOUD, conflict.recommendedChoice)
    }

    @Test
    fun `checkManualRestoreConflict intercepts overwriting newer local Room progress`() {
        val local = sampleEntries(coins = 900, gamesPlayedJson = """{"ttt":{"played":15,"won":12}}""")
        val cloud = sampleCloudBundle(coins = 300, games = 5L, clientSavedAtMs = 2_000L)

        val conflict = SaveConflictDetector.checkManualRestoreConflict(
            localEntries = local,
            localUpdatedAtMs = 12_000L,
            snapshotsCount = 3,
            targetBundle = cloud,
            lastCloudSyncMs = 5_000L,
        )

        assertNotNull(conflict)
        assertEquals(SaveConflictSource.MANUAL_RESTORE, conflict!!.source)
        assertEquals(15, conflict.local.gamesPlayed)
        assertEquals(ConflictKeepChoice.LOCAL, conflict.recommendedChoice)
        assertTrue(conflict.isLocalNewer)
    }
}
