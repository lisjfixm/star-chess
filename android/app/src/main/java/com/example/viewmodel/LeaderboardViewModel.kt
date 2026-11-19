package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.cloud.LeaderboardEntry
import com.example.data.cloud.LeaderboardRepository
import com.example.data.cloud.extractLocalGameStatistics
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface LeaderboardUiState {
    data object Loading : LeaderboardUiState
    data class Success(val entries: List<LeaderboardEntry>) : LeaderboardUiState
    data class Error(val message: String) : LeaderboardUiState
}

sealed interface LeaderboardActionEvent {
    data object Published : LeaderboardActionEvent
    data class Failure(val message: String) : LeaderboardActionEvent
}

class LeaderboardViewModel(
    private val repository: LeaderboardRepository,
    private val currentUserId: String,
) : ViewModel() {

    val leaderboardState: StateFlow<LeaderboardUiState> = repository.observeGlobalLeaderboard(limit = 50L)
        .map<List<LeaderboardEntry>, LeaderboardUiState> { LeaderboardUiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing global leaderboard", rootError)
            emit(LeaderboardUiState.Error(rootError.localizedMessage ?: "Failed to load global leaderboard"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = LeaderboardUiState.Loading,
        )

    private val _isPublishing = MutableStateFlow(false)
    val isPublishing: StateFlow<Boolean> = _isPublishing.asStateFlow()

    private val _events = MutableSharedFlow<LeaderboardActionEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<LeaderboardActionEvent> = _events.asSharedFlow()

    fun publishFromLocalEntries(
        rawDisplayName: String?,
        entries: Map<String, String>,
        favoriteTheme: String,
        notifyUser: Boolean = false,
    ) {
        if (_isPublishing.value) return
        val localStats = extractLocalGameStatistics(entries)
        val totalWins = localStats.values.sumOf { it.wins }
        val totalLosses = localStats.values.sumOf { it.losses }
        val totalDraws = localStats.values.sumOf { it.draws }
        val totalDuration = localStats.values.sumOf { it.playDurationSeconds }
        val gamesPlayedCount = localStats.size.coerceIn(0, 26)

        viewModelScope.launch {
            _isPublishing.value = true
            val result = repository.publishUserRanking(
                userId = currentUserId,
                rawDisplayName = rawDisplayName,
                totalWins = totalWins,
                totalLosses = totalLosses,
                totalDraws = totalDraws,
                totalPlayDurationSeconds = totalDuration,
                gamesPlayedCount = gamesPlayedCount,
                favoriteTheme = favoriteTheme,
            )
            _isPublishing.value = false
            result.fold(
                onSuccess = {
                    if (notifyUser) {
                        _events.tryEmit(LeaderboardActionEvent.Published)
                    }
                },
                onFailure = { error ->
                    if (notifyUser) {
                        _events.tryEmit(
                            LeaderboardActionEvent.Failure(
                                error.localizedMessage ?: "Failed to update leaderboard rank"
                            )
                        )
                    }
                },
            )
        }
    }

    private companion object {
        const val TAG = "LeaderboardVM"
    }
}
