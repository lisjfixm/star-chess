package com.example.data.cloud

import android.content.Context
import android.os.Build
import com.example.R
import com.example.data.local.PreferencesStore
import com.example.data.local.ProgressionSummary
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CloudSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun observeCloudSaves(userId: String): Flow<List<CloudSaveBundle>> = flow {
        val currentUid = requireUserId()
        check(currentUid == userId) { "Signed-in user changed; refusing cross-user observe." }
        val path = "users/$userId/cloudSaves"
        emitAll(
            db.collection("users")
                .document(userId)
                .collection("cloudSaves")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudSaveBundle::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    ).sortedByDescending { bundle ->
                        bundle.updatedAt?.toDate()?.time ?: bundle.clientSavedAtMs
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun upsertCloudSave(
        userId: String,
        saveId: String,
        label: String,
        entries: Map<String, String>,
        summary: ProgressionSummary,
        preferredLanguage: String,
    ): Result<Unit> {
        val safeSaveId = saveId.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128).ifBlank { "primary" }
        val docPath = "users/$userId/cloudSaves/$safeSaveId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user write." }

            val sanitizedEntries = entries
                .filterKeys { it.startsWith("starchess.") && it.length <= 128 }
                .entries
                .take(200)
                .associate { (k, v) -> k to v }

            val normalizedLang = PreferencesStore.normalizeLanguage(preferredLanguage)
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}".trim().take(80).ifBlank { "Android Device" }
            val safeLabel = label.trim().take(80).ifBlank { "Primary Sync" }
            val schemaVer = sanitizedEntries["starchess.v7.schemaVer"]?.take(16)?.ifBlank { "v7" } ?: "v7"

            val bundle = CloudSaveBundle(
                userId = uid,
                label = safeLabel,
                schemaVersion = schemaVer,
                entries = sanitizedEntries,
                starCoins = summary.starCoins.toLong().coerceIn(0L, 1_000_000_000L),
                eloPeak = summary.eloPeak.toLong().coerceIn(0L, 10_000L),
                achievementsCount = summary.achievementsCount.toLong().coerceIn(0L, 1_000L),
                gamesPlayed = summary.gamesPlayed.toLong().coerceIn(0L, 1_000_000L),
                preferredLanguage = normalizedLang,
                deviceName = deviceName,
                clientSavedAtMs = System.currentTimeMillis().coerceAtLeast(0L),
            )

            val docRef = db.collection("users").document(uid)
                .collection("cloudSaves").document(safeSaveId)

            val existingDoc = docRef.get().await()
            if (existingDoc.exists()) {
                docRef.update(bundle.toUpdateMap()).await()
            } else {
                docRef.set(bundle.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudSave(userId: String, saveId: String): Result<Unit> {
        val docPath = "users/$userId/cloudSaves/$saveId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user delete." }
            require(saveId.matches(Regex("^[a-zA-Z0-9_\\-]+$")) && saveId.length <= 128) {
                "Invalid saveId"
            }
            db.collection("users").document(uid)
                .collection("cloudSaves").document(saveId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
