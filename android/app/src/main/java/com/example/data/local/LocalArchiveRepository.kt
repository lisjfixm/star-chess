package com.example.data.local

import com.example.data.cloud.StarChessGameCatalog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale

data class DailyBonusOffer(
    val todayDate: String,
    val streakDay: Int,
    val rewardCoins: Int,
    val totalClaims: Int,
) {
    val cycleDayIndex: Int
        get() = ((streakDay - 1) % 7) + 1
}

class LocalArchiveRepository(
    private val dao: StarChessDao,
    private val prefsStore: PreferencesStore,
) {
    val entriesMapFlow: Flow<Map<String, String>> = dao.observeEntries().map { list ->
        list.associate { it.key to it.value }
    }

    val localSaveTimestampMsFlow: Flow<Long> = dao.observeEntries().map { list ->
        list.filter { it.key != "starchess.lang" && it.key != "starchess.theme" }
            .maxOfOrNull { it.updatedAtMs } ?: 0L
    }

    val summaryFlow: Flow<ProgressionSummary> = entriesMapFlow.map { map ->
        ProgressionSummary.fromEntries(map)
    }

    val snapshotsFlow: Flow<List<LocalSnapshotEntity>> = dao.observeSnapshots()

    val preferencesFlow: Flow<ShellPreferences> = prefsStore.preferencesFlow

    val dailyRewardFlow: Flow<DailyLoginRewardEntity?> = dao.observeDailyReward()

    suspend fun getAllEntriesMap(): Map<String, String> {
        return dao.getAllEntries().associate { it.key to it.value }
    }

    suspend fun upsertStorageKey(key: String, value: String) {
        if (!key.startsWith("starchess.")) return
        dao.upsertEntry(StorageEntryEntity(key = key, value = value))
        if (key == "starchess.lang") {
            prefsStore.setLanguage(value)
        }
    }

    suspend fun removeStorageKey(key: String) {
        dao.deleteEntry(key)
    }

    suspend fun clearStorageKeys() {
        dao.clearAllEntries()
    }

    suspend fun replaceAllStorageEntries(entries: Map<String, String>) {
        val now = System.currentTimeMillis()
        val entities = entries
            .filterKeys { it.startsWith("starchess.") }
            .map { (k, v) -> StorageEntryEntity(key = k, value = v, updatedAtMs = now) }
        dao.replaceAllEntries(entities)
        entries["starchess.lang"]?.let { lang ->
            prefsStore.setLanguage(lang)
        }
    }

    suspend fun recordPlayDurationDelta(gameId: String, deltaSeconds: Long) {
        if (gameId !in StarChessGameCatalog.SUPPORTED_IDS || deltaSeconds <= 0L) return
        val existingRaw = dao.getEntryByKey(KEY_PLAY_DURATION_BY_GAME)?.value
        val obj = runCatching {
            if (existingRaw.isNullOrBlank()) JSONObject() else JSONObject(existingRaw)
        }.getOrElse { JSONObject() }
        val prev = obj.optLong(gameId, 0L).coerceAtLeast(0L)
        obj.put(gameId, (prev + deltaSeconds).coerceAtMost(100_000_000L))
        dao.upsertEntry(StorageEntryEntity(key = KEY_PLAY_DURATION_BY_GAME, value = obj.toString()))
    }

    suspend fun checkPendingDailyBonus(): DailyBonusOffer? {
        val today = formatCalendarDate(Calendar.getInstance())
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterday = formatCalendarDate(yesterdayCal)

        val record = dao.getDailyReward()
        if (record != null && record.lastLoginDate == today) {
            return null
        }

        val nextStreak = if (record != null && record.lastLoginDate == yesterday) {
            record.consecutiveDays + 1
        } else {
            1
        }
        val nextTotal = (record?.totalClaims ?: 0) + 1
        val rewardCoins = rewardCoinsForStreak(nextStreak)

        return DailyBonusOffer(
            todayDate = today,
            streakDay = nextStreak,
            rewardCoins = rewardCoins,
            totalClaims = nextTotal,
        )
    }

    suspend fun claimDailyBonus(offer: DailyBonusOffer): Int {
        dao.upsertDailyReward(
            DailyLoginRewardEntity(
                profileKey = "default",
                lastLoginDate = offer.todayDate,
                consecutiveDays = offer.streakDay,
                totalClaims = offer.totalClaims,
                lastRewardCoins = offer.rewardCoins,
                updatedAtMs = System.currentTimeMillis(),
            )
        )

        val currentCoins = dao.getEntryByKey("starchess.starCoins")
            ?.value?.trim()?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val newCoins = (currentCoins + offer.rewardCoins).coerceAtMost(1_000_000_000)
        dao.upsertEntry(StorageEntryEntity(key = "starchess.starCoins", value = newCoins.toString()))

        val currentEarned = dao.getEntryByKey("starchess.totalCoinsEarned")
            ?.value?.trim()?.toIntOrNull()?.coerceAtLeast(0) ?: currentCoins
        val newEarned = (currentEarned + offer.rewardCoins).coerceAtMost(1_000_000_000)
        dao.upsertEntry(StorageEntryEntity(key = "starchess.totalCoinsEarned", value = newEarned.toString()))

        return newCoins
    }

    suspend fun createLocalCheckpoint(label: String): LocalSnapshotEntity {
        val entries = getAllEntriesMap()
        val summary = ProgressionSummary.fromEntries(entries)
        val payload = JSONObject().apply {
            entries.forEach { (k, v) -> put(k, v) }
        }.toString()
        val entity = LocalSnapshotEntity(
            label = label.ifBlank { "Checkpoint" }.take(64),
            schemaVersion = entries["starchess.v7.schemaVer"] ?: "v7",
            payloadJson = payload,
            starCoins = summary.starCoins,
            eloPeak = summary.eloPeak,
            gamesPlayed = summary.gamesPlayed,
        )
        dao.insertSnapshot(entity)
        return entity
    }

    suspend fun createSafetyCheckpointIfNeeded(label: String): LocalSnapshotEntity? {
        val entries = getAllEntriesMap()
        if (!com.example.data.cloud.SaveConflictDetector.hasMeaningfulSaveData(entries)) {
            return null
        }
        return createLocalCheckpoint(label)
    }

    suspend fun loadCheckpointMap(snapshotId: Long): Map<String, String>? {
        val snapshot = dao.getSnapshotById(snapshotId) ?: return null
        return runCatching {
            val obj = JSONObject(snapshot.payloadJson)
            val result = linkedMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key.startsWith("starchess.")) {
                    result[key] = obj.optString(key, "")
                }
            }
            result
        }.getOrNull()
    }

    suspend fun deleteCheckpoint(snapshotId: Long) {
        dao.deleteSnapshot(snapshotId)
    }

    suspend fun setLanguage(languageCode: String) {
        prefsStore.setLanguage(languageCode)
        dao.upsertEntry(StorageEntryEntity(key = "starchess.lang", value = PreferencesStore.normalizeLanguage(languageCode)))
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        prefsStore.setSoundEnabled(enabled)
    }

    suspend fun setBgmEnabled(enabled: Boolean) {
        prefsStore.setBgmEnabled(enabled)
    }

    suspend fun setCosmicTheme(themeKey: String) {
        prefsStore.setCosmicTheme(themeKey)
    }

    suspend fun markCloudSynced(timestampMs: Long = System.currentTimeMillis()) {
        prefsStore.setLastCloudSyncMs(timestampMs)
    }

    companion object {
        const val KEY_PLAY_DURATION_BY_GAME = "starchess.android.playDurationByGame"

        val DAILY_REWARD_SCHEDULE = listOf(50, 75, 100, 125, 150, 200, 300)

        fun rewardCoinsForStreak(streakDay: Int): Int {
            val idx = ((streakDay - 1).coerceAtLeast(0)) % DAILY_REWARD_SCHEDULE.size
            return DAILY_REWARD_SCHEDULE[idx]
        }

        fun formatCalendarDate(calendar: Calendar): String {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            return String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
        }
    }
}
