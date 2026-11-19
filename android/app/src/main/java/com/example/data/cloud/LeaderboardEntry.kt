package com.example.data.cloud

import androidx.annotation.Keep
import com.example.ui.theme.CosmicPalettes
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Star Commander",
    val totalWins: Long = 0L,
    val totalLosses: Long = 0L,
    val totalDraws: Long = 0L,
    val totalPlayDurationSeconds: Long = 0L,
    val gamesPlayedCount: Long = 0L,
    val favoriteTheme: String = "default",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    val totalMatches: Long
        get() = totalWins + totalLosses + totalDraws

    val winRatePercent: Int
        get() = if (totalMatches > 0L) ((totalWins * 100L) / totalMatches).toInt().coerceIn(0, 100) else 0

    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "displayName" to sanitizePublicDisplayName(displayName, userId),
        "totalWins" to totalWins.coerceIn(0L, 26_000_000L),
        "totalLosses" to totalLosses.coerceIn(0L, 26_000_000L),
        "totalDraws" to totalDraws.coerceIn(0L, 26_000_000L),
        "totalPlayDurationSeconds" to totalPlayDurationSeconds.coerceIn(0L, 1_000_000_000L),
        "gamesPlayedCount" to gamesPlayedCount.coerceIn(0L, 26L),
        "favoriteTheme" to CosmicPalettes.normalizeKey(favoriteTheme),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "displayName" to sanitizePublicDisplayName(displayName, userId),
        "totalWins" to totalWins.coerceIn(0L, 26_000_000L),
        "totalLosses" to totalLosses.coerceIn(0L, 26_000_000L),
        "totalDraws" to totalDraws.coerceIn(0L, 26_000_000L),
        "totalPlayDurationSeconds" to totalPlayDurationSeconds.coerceIn(0L, 1_000_000_000L),
        "gamesPlayedCount" to gamesPlayedCount.coerceIn(0L, 26L),
        "favoriteTheme" to CosmicPalettes.normalizeKey(favoriteTheme),
        "updatedAt" to FieldValue.serverTimestamp(),
    )

    companion object {
        /**
         * Ensures no PII (e.g., email address containing '@') is ever written to the global leaderboard.
         */
        fun sanitizePublicDisplayName(rawName: String?, userId: String): String {
            val cleaned = rawName
                ?.trim()
                ?.substringBefore("@")
                ?.replace("@", "")
                ?.trim()
                ?.take(40)
            if (!cleaned.isNullOrBlank()) return cleaned
            val shortTag = userId.filter { it.isLetterOrDigit() }.take(6).uppercase().ifBlank { "ALPHA" }
            return "Commander $shortTag"
        }
    }
}
