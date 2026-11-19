package com.example.data.cloud

import com.example.data.local.ProgressionSummary
import java.security.MessageDigest
import kotlin.math.abs

enum class ConflictKeepChoice {
    LOCAL,
    CLOUD,
    TIE,
}

enum class SaveConflictSource {
    AUTO_DIVERGENCE,
    MANUAL_BACKUP,
    MANUAL_RESTORE,
    MANUAL_INSPECT,
}

data class SaveVersionSnapshot(
    val title: String,
    val subtitle: String,
    val timestampMs: Long,
    val starCoins: Int,
    val eloPeak: Int,
    val achievementsCount: Int,
    val gamesPlayed: Int,
    val storedKeysCount: Int,
    val snapshotsCount: Int = 0,
    val entries: Map<String, String>,
)

data class SaveConflictState(
    val conflictFingerprint: String,
    val source: SaveConflictSource,
    val cloudBundle: CloudSaveBundle,
    val local: SaveVersionSnapshot,
    val cloud: SaveVersionSnapshot,
    val recommendedChoice: ConflictKeepChoice,
) {
    val isLocalNewer: Boolean
        get() = local.timestampMs > 0L &&
            (cloud.timestampMs <= 0L || local.timestampMs > cloud.timestampMs + TIMESTAMP_TOLERANCE_MS)

    val isCloudNewer: Boolean
        get() = cloud.timestampMs > 0L &&
            (local.timestampMs <= 0L || cloud.timestampMs > local.timestampMs + TIMESTAMP_TOLERANCE_MS)

    private companion object {
        const val TIMESTAMP_TOLERANCE_MS = 2_000L
    }
}

object SaveConflictDetector {

    private val IGNORED_DIFF_KEYS = setOf(
        "starchess.lang",
        "starchess.theme",
    )

    private const val SYNC_TIMESTAMP_TOLERANCE_MS = 2_000L

    fun canonicalEntries(entries: Map<String, String>): Map<String, String> {
        return entries
            .filterKeys { key ->
                key.startsWith("starchess.") && key !in IGNORED_DIFF_KEYS && entries[key] != null
            }
            .toSortedMap()
    }

    fun payloadDigest(entries: Map<String, String>): String {
        val canonical = canonicalEntries(entries)
        if (canonical.isEmpty()) return "empty"
        val md = MessageDigest.getInstance("SHA-256")
        canonical.forEach { (k, v) ->
            md.update(k.toByteArray(Charsets.UTF_8))
            md.update(0.toByte())
            md.update(v.toByteArray(Charsets.UTF_8))
            md.update(1.toByte())
        }
        return md.digest().joinToString("") { "%02x".format(it) }.take(24)
    }

    fun arePayloadsDifferent(
        localEntries: Map<String, String>,
        cloudEntries: Map<String, String>,
    ): Boolean {
        return canonicalEntries(localEntries) != canonicalEntries(cloudEntries)
    }

    fun hasMeaningfulSaveData(
        entries: Map<String, String>,
        summary: ProgressionSummary = ProgressionSummary.fromEntries(entries),
    ): Boolean {
        return summary.starCoins > 0 ||
            summary.gamesPlayed > 0 ||
            summary.achievementsCount > 0 ||
            summary.eloPeak > 1000 ||
            !entries["starchess.records"].isNullOrBlank() ||
            !entries["starchess.v7.statsByGame"].isNullOrBlank() ||
            !entries["starchess.ach"].isNullOrBlank() ||
            !entries["starchess.elo"].isNullOrBlank()
    }

    fun cloudTimestampMs(bundle: CloudSaveBundle): Long {
        return bundle.updatedAt?.toDate()?.time ?: bundle.clientSavedAtMs
    }

    fun buildConflict(
        localEntries: Map<String, String>,
        localUpdatedAtMs: Long,
        snapshotsCount: Int,
        cloudBundle: CloudSaveBundle,
        source: SaveConflictSource,
    ): SaveConflictState {
        val localSummary = ProgressionSummary.fromEntries(localEntries)
        val cloudSummaryFromEntries = ProgressionSummary.fromEntries(cloudBundle.entries)

        val cloudCoins = maxOf(cloudBundle.starCoins.toInt(), cloudSummaryFromEntries.starCoins)
        val cloudElo = maxOf(cloudBundle.eloPeak.toInt(), cloudSummaryFromEntries.eloPeak)
        val cloudAchievements = maxOf(cloudBundle.achievementsCount.toInt(), cloudSummaryFromEntries.achievementsCount)
        val cloudGames = maxOf(cloudBundle.gamesPlayed.toInt(), cloudSummaryFromEntries.gamesPlayed)
        val cloudTs = cloudTimestampMs(cloudBundle)

        val localSnapshot = SaveVersionSnapshot(
            title = "Local Room",
            subtitle = "Room Database",
            timestampMs = localUpdatedAtMs,
            starCoins = localSummary.starCoins,
            eloPeak = localSummary.eloPeak,
            achievementsCount = localSummary.achievementsCount,
            gamesPlayed = localSummary.gamesPlayed,
            storedKeysCount = localEntries.size,
            snapshotsCount = snapshotsCount,
            entries = localEntries,
        )

        val cloudSnapshot = SaveVersionSnapshot(
            title = cloudBundle.label,
            subtitle = cloudBundle.deviceName,
            timestampMs = cloudTs,
            starCoins = cloudCoins,
            eloPeak = cloudElo,
            achievementsCount = cloudAchievements,
            gamesPlayed = cloudGames,
            storedKeysCount = cloudBundle.entries.size,
            snapshotsCount = 0,
            entries = cloudBundle.entries,
        )

        val recommended = computeRecommendation(localSnapshot, cloudSnapshot)
        val fingerprint = "${payloadDigest(localEntries)}:${cloudBundle.id}:${payloadDigest(cloudBundle.entries)}"

        return SaveConflictState(
            conflictFingerprint = fingerprint,
            source = source,
            cloudBundle = cloudBundle,
            local = localSnapshot,
            cloud = cloudSnapshot,
            recommendedChoice = recommended,
        )
    }

    /**
     * Automatically detects divergence between Local Room saves and Firestore cloud saves
     * when a signed-in user has not established a clean sync baseline OR when Firestore was
     * updated from another device/session after the last local sync.
     */
    fun detectAutomaticConflict(
        localEntries: Map<String, String>,
        localUpdatedAtMs: Long,
        snapshotsCount: Int,
        cloudSaves: List<CloudSaveBundle>,
        lastCloudSyncMs: Long,
        snoozedFingerprints: Set<String>,
    ): SaveConflictState? {
        val targetBundle = selectPrimaryCloudBundle(cloudSaves) ?: return null
        if (!arePayloadsDifferent(localEntries, targetBundle.entries)) return null

        val localHasData = hasMeaningfulSaveData(localEntries)
        val cloudSummary = ProgressionSummary.fromEntries(targetBundle.entries)
        val cloudHasData = hasMeaningfulSaveData(targetBundle.entries, cloudSummary) ||
            targetBundle.starCoins > 0 ||
            targetBundle.gamesPlayed > 0 ||
            targetBundle.achievementsCount > 0 ||
            targetBundle.eloPeak > 1000

        if (!localHasData || !cloudHasData) return null

        val cloudTs = cloudTimestampMs(targetBundle)
        val cloudUpdatedAfterLastSync = lastCloudSyncMs <= 0L || cloudTs > lastCloudSyncMs + SYNC_TIMESTAMP_TOLERANCE_MS
        if (!cloudUpdatedAfterLastSync) return null

        val conflict = buildConflict(
            localEntries = localEntries,
            localUpdatedAtMs = localUpdatedAtMs,
            snapshotsCount = snapshotsCount,
            cloudBundle = targetBundle,
            source = SaveConflictSource.AUTO_DIVERGENCE,
        )

        return if (conflict.conflictFingerprint in snoozedFingerprints) null else conflict
    }

    /**
     * Checks whether overwriting the cloud primary save would discard newer or higher cloud progress.
     */
    fun checkManualBackupConflict(
        localEntries: Map<String, String>,
        localUpdatedAtMs: Long,
        snapshotsCount: Int,
        cloudSaves: List<CloudSaveBundle>,
        lastCloudSyncMs: Long,
    ): SaveConflictState? {
        val primary = cloudSaves.firstOrNull { it.id == "primary" } ?: selectPrimaryCloudBundle(cloudSaves) ?: return null
        if (!arePayloadsDifferent(localEntries, primary.entries)) return null

        val cloudSummary = ProgressionSummary.fromEntries(primary.entries)
        val cloudHasData = hasMeaningfulSaveData(primary.entries, cloudSummary) ||
            primary.starCoins > 0 ||
            primary.gamesPlayed > 0 ||
            primary.achievementsCount > 0 ||
            primary.eloPeak > 1000
        if (!cloudHasData) return null

        val localSummary = ProgressionSummary.fromEntries(localEntries)
        val cloudTs = cloudTimestampMs(primary)
        val cloudHasHigherMetric =
            primary.gamesPlayed > localSummary.gamesPlayed ||
                primary.starCoins > localSummary.starCoins ||
                primary.eloPeak > localSummary.eloPeak ||
                primary.achievementsCount > localSummary.achievementsCount
        val cloudDiverged = lastCloudSyncMs <= 0L || cloudTs > lastCloudSyncMs + SYNC_TIMESTAMP_TOLERANCE_MS

        if (!cloudDiverged && !cloudHasHigherMetric) return null

        return buildConflict(
            localEntries = localEntries,
            localUpdatedAtMs = localUpdatedAtMs,
            snapshotsCount = snapshotsCount,
            cloudBundle = primary,
            source = SaveConflictSource.MANUAL_BACKUP,
        )
    }

    /**
     * Checks whether restoring a cloud save bundle would overwrite divergent local Room progress.
     */
    fun checkManualRestoreConflict(
        localEntries: Map<String, String>,
        localUpdatedAtMs: Long,
        snapshotsCount: Int,
        targetBundle: CloudSaveBundle,
        lastCloudSyncMs: Long,
    ): SaveConflictState? {
        if (!hasMeaningfulSaveData(localEntries)) return null
        if (!arePayloadsDifferent(localEntries, targetBundle.entries)) return null

        val localSummary = ProgressionSummary.fromEntries(localEntries)
        val localHasHigherMetric =
            localSummary.gamesPlayed > targetBundle.gamesPlayed ||
                localSummary.starCoins > targetBundle.starCoins ||
                localSummary.eloPeak > targetBundle.eloPeak ||
                localSummary.achievementsCount > targetBundle.achievementsCount
        val localModifiedAfterSync = lastCloudSyncMs <= 0L || localUpdatedAtMs > lastCloudSyncMs + SYNC_TIMESTAMP_TOLERANCE_MS

        if (!localModifiedAfterSync && !localHasHigherMetric) return null

        return buildConflict(
            localEntries = localEntries,
            localUpdatedAtMs = localUpdatedAtMs,
            snapshotsCount = snapshotsCount,
            cloudBundle = targetBundle,
            source = SaveConflictSource.MANUAL_RESTORE,
        )
    }

    /**
     * Returns an inspection conflict state for displaying a persistent "Out of sync / Resolve"
     * banner in the Cloud tab whenever local Room and primary Firestore data differ.
     */
    fun findDivergenceForBanner(
        localEntries: Map<String, String>,
        localUpdatedAtMs: Long,
        snapshotsCount: Int,
        cloudSaves: List<CloudSaveBundle>,
    ): SaveConflictState? {
        val primary = selectPrimaryCloudBundle(cloudSaves) ?: return null
        if (!arePayloadsDifferent(localEntries, primary.entries)) return null

        val localHasData = hasMeaningfulSaveData(localEntries)
        val cloudHasData = hasMeaningfulSaveData(primary.entries) ||
            primary.starCoins > 0 ||
            primary.gamesPlayed > 0
        if (!localHasData || !cloudHasData) return null

        return buildConflict(
            localEntries = localEntries,
            localUpdatedAtMs = localUpdatedAtMs,
            snapshotsCount = snapshotsCount,
            cloudBundle = primary,
            source = SaveConflictSource.MANUAL_INSPECT,
        )
    }

    private fun selectPrimaryCloudBundle(cloudSaves: List<CloudSaveBundle>): CloudSaveBundle? {
        return cloudSaves.firstOrNull { it.id == "primary" } ?: cloudSaves.firstOrNull()
    }

    private fun computeRecommendation(
        local: SaveVersionSnapshot,
        cloud: SaveVersionSnapshot,
    ): ConflictKeepChoice {
        var localVotes = 0
        var cloudVotes = 0

        if (local.gamesPlayed > cloud.gamesPlayed) localVotes += 2
        else if (cloud.gamesPlayed > local.gamesPlayed) cloudVotes += 2

        if (local.starCoins > cloud.starCoins) localVotes += 1
        else if (cloud.starCoins > local.starCoins) cloudVotes += 1

        if (local.achievementsCount > cloud.achievementsCount) localVotes += 1
        else if (cloud.achievementsCount > local.achievementsCount) cloudVotes += 1

        if (local.eloPeak > cloud.eloPeak) localVotes += 1
        else if (cloud.eloPeak > local.eloPeak) cloudVotes += 1

        if (localVotes != cloudVotes) {
            return if (localVotes > cloudVotes) ConflictKeepChoice.LOCAL else ConflictKeepChoice.CLOUD
        }

        if (local.timestampMs > 0L && cloud.timestampMs > 0L &&
            abs(local.timestampMs - cloud.timestampMs) > SYNC_TIMESTAMP_TOLERANCE_MS
        ) {
            return if (local.timestampMs > cloud.timestampMs) ConflictKeepChoice.LOCAL else ConflictKeepChoice.CLOUD
        }

        return ConflictKeepChoice.TIE
    }
}
