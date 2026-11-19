package com.example.data.cloud

import android.content.Context
import com.example.R
import com.example.ui.theme.CosmicPalettes
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class LeaderboardRepository(
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
            ?: throw IllegalStateException("User must be signed in with Google before accessing the leaderboard.")
    }

    fun observeGlobalLeaderboard(limit: Long = 50L): Flow<List<LeaderboardEntry>> = flow {
        val currentUid = requireUserId()
        val safeLimit = limit.coerceIn(1L, 50L)
        val path = "leaderboard"
        emitAll(
            db.collection(path)
                .orderBy("totalWins", Query.Direction.DESCENDING)
                .limit(safeLimit)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LeaderboardEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    ).sortedWith(
                        compareByDescending<LeaderboardEntry> { it.totalWins }
                            .thenByDescending { it.winRatePercent }
                            .thenByDescending { it.totalPlayDurationSeconds }
                            .thenBy { it.displayName.lowercase() }
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val toLog = firestoreError ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == currentUid) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun publishUserRanking(
        userId: String,
        rawDisplayName: String?,
        totalWins: Long,
        totalLosses: Long,
        totalDraws: Long,
        totalPlayDurationSeconds: Long,
        gamesPlayedCount: Int,
        favoriteTheme: String,
    ): Result<Unit> {
        val docPath = "leaderboard/$userId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Signed-in user changed; refusing cross-user leaderboard write." }

            val docRef = db.collection("leaderboard").document(uid)
            val remoteSnap = docRef.get().await()
            val existingWins = (remoteSnap.data?.get("totalWins") as? Number)?.toLong() ?: 0L
            val existingLosses = (remoteSnap.data?.get("totalLosses") as? Number)?.toLong() ?: 0L
            val existingDraws = (remoteSnap.data?.get("totalDraws") as? Number)?.toLong() ?: 0L
            val existingDuration = (remoteSnap.data?.get("totalPlayDurationSeconds") as? Number)?.toLong() ?: 0L
            val existingGames = (remoteSnap.data?.get("gamesPlayedCount") as? Number)?.toLong() ?: 0L

            val entry = LeaderboardEntry(
                userId = uid,
                displayName = LeaderboardEntry.sanitizePublicDisplayName(rawDisplayName, uid),
                totalWins = maxOf(totalWins, existingWins).coerceIn(0L, 26_000_000L),
                totalLosses = maxOf(totalLosses, existingLosses).coerceIn(0L, 26_000_000L),
                totalDraws = maxOf(totalDraws, existingDraws).coerceIn(0L, 26_000_000L),
                totalPlayDurationSeconds = maxOf(totalPlayDurationSeconds, existingDuration).coerceIn(0L, 1_000_000_000L),
                gamesPlayedCount = maxOf(gamesPlayedCount.toLong(), existingGames).coerceIn(0L, 26L),
                favoriteTheme = CosmicPalettes.normalizeKey(favoriteTheme),
            )

            if (remoteSnap.exists()) {
                docRef.update(entry.toUpdateMap()).await()
            } else {
                docRef.set(entry.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }
}
