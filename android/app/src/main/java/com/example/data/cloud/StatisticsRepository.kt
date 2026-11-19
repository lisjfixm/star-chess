package com.example.data.cloud

import android.content.Context
import com.example.R
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

class StatisticsRepository(
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
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore statistics.")
    }

    fun observeGameStatistics(userId: String): Flow<List<GameStatistic>> = flow {
        val currentUid = requireUserId()
        check(currentUid == userId) { "Signed-in user changed; refusing cross-user statistics observe." }
        val path = "users/$userId/gameStats"
        emitAll(
            db.collection("users")
                .document(userId)
                .collection("gameStats")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        GameStatistic::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    )
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

    suspend fun syncLocalGameStats(
        userId: String,
        localStats: Map<String, LocalGameStatSnapshot>,
        existingCloudByGame: Map<String, GameStatistic>,
    ): Result<Int> {
        val basePath = "users/$userId/gameStats"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user statistics write." }

            var updatedCount = 0
            for ((gameId, local) in localStats) {
                if (gameId !in StarChessGameCatalog.SUPPORTED_IDS) continue
                val existing = existingCloudByGame[gameId]
                val mergedWins = maxOf(local.wins, existing?.wins ?: 0L).coerceIn(0L, 1_000_000L)
                val mergedLosses = maxOf(local.losses, existing?.losses ?: 0L).coerceIn(0L, 1_000_000L)
                val mergedDraws = maxOf(local.draws, existing?.draws ?: 0L).coerceIn(0L, 1_000_000L)
                val mergedDuration = maxOf(local.playDurationSeconds, existing?.playDurationSeconds ?: 0L)
                    .coerceIn(0L, 100_000_000L)

                if (existing != null &&
                    existing.wins == mergedWins &&
                    existing.losses == mergedLosses &&
                    existing.draws == mergedDraws &&
                    existing.playDurationSeconds == mergedDuration
                ) {
                    continue
                }

                val docRef = db.collection("users")
                    .document(uid)
                    .collection("gameStats")
                    .document(gameId)

                val stat = GameStatistic(
                    userId = uid,
                    gameId = gameId,
                    wins = mergedWins,
                    losses = mergedLosses,
                    draws = mergedDraws,
                    playDurationSeconds = mergedDuration,
                )

                val remoteSnap = docRef.get().await()
                if (remoteSnap.exists()) {
                    docRef.update(stat.toUpdateMap()).await()
                } else {
                    docRef.set(stat.toCreateMap()).await()
                }
                updatedCount++
            }
            Result.success(updatedCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, basePath)
            Result.failure(e)
        }
    }
}
