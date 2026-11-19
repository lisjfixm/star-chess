package com.example.data.cloud

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties

@Keep
@IgnoreExtraProperties
data class CloudSaveBundle(
    @DocumentId val id: String = "",
    val userId: String = "",
    val label: String = "Primary Sync",
    val schemaVersion: String = "v7",
    val entries: Map<String, String> = emptyMap(),
    val starCoins: Long = 0L,
    val eloPeak: Long = 1000L,
    val achievementsCount: Long = 0L,
    val gamesPlayed: Long = 0L,
    val preferredLanguage: String = "zh-Hant",
    val deviceName: String = "Android Device",
    val clientSavedAtMs: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "label" to label,
        "schemaVersion" to schemaVersion,
        "entries" to entries,
        "starCoins" to starCoins,
        "eloPeak" to eloPeak,
        "achievementsCount" to achievementsCount,
        "gamesPlayed" to gamesPlayed,
        "preferredLanguage" to preferredLanguage,
        "deviceName" to deviceName,
        "clientSavedAtMs" to clientSavedAtMs,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "label" to label,
        "schemaVersion" to schemaVersion,
        "entries" to entries,
        "starCoins" to starCoins,
        "eloPeak" to eloPeak,
        "achievementsCount" to achievementsCount,
        "gamesPlayed" to gamesPlayed,
        "preferredLanguage" to preferredLanguage,
        "deviceName" to deviceName,
        "clientSavedAtMs" to clientSavedAtMs,
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}
