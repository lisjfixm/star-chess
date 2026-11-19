package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.cloud.AggregatedGameStatistics
import com.example.data.cloud.GameStatistic
import com.example.data.cloud.StatisticsRepository
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

sealed interface StatisticsUiState {
    data object Loading : StatisticsUiState
    data class Success(val stats: AggregatedGameStatistics) : StatisticsUiState
    data class Error(val message: String) : StatisticsUiState
}

sealed interface StatisticsActionEvent {
    data class Synced(val updatedGames: Int) : StatisticsActionEvent
    data class Failure(val message: String) : StatisticsActionEvent
}

class StatisticsViewModel(
    private val repository: StatisticsRepository,
    private val currentUserId: String,
) : ViewModel() {

    val statsState: StateFlow<StatisticsUiState> = repository.observeGameStatistics(currentUserId)
        .map<List<GameStatistic>, StatisticsUiState> { docs ->
            StatisticsUiState.Success(
                AggregatedGameStatistics.fromCloudList(docs, currentUserId)
            )
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing Firestore game statistics", rootError)
            emit(StatisticsUiState.Error(rootError.localizedMessage ?: "Failed to load statistics"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = StatisticsUiState.Loading,
        )

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _events = MutableSharedFlow<StatisticsActionEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<StatisticsActionEvent> = _events.asSharedFlow()

    fun syncFromLocalEntries(entries: Map<String, String>, notifyUser: Boolean = false) {
        if (_isSyncing.value) return
        val localStats = extractLocalGameStatistics(entries)
        if (localStats.isEmpty()) {
            if (notifyUser) {
                _events.tryEmit(StatisticsActionEvent.Synced(0))
            }
            return
        }
        val existingCloud = (statsState.value as? StatisticsUiState.Success)
            ?.stats
            ?.allGames
            ?.filter { it.createdAt != null || it.totalMatches > 0L || it.playDurationSeconds > 0L }
            ?.associateBy { it.gameId }
            ?: emptyMap()

        viewModelScope.launch {
            _isSyncing.value = true
            val result = repository.syncLocalGameStats(
                userId = currentUserId,
                localStats = localStats,
                existingCloudByGame = existingCloud,
            )
            _isSyncing.value = false
            result.fold(
                onSuccess = { count ->
                    if (notifyUser) {
                        _events.tryEmit(StatisticsActionEvent.Synced(count))
                    }
                },
                onFailure = { error ->
                    if (notifyUser) {
                        _events.tryEmit(
                            StatisticsActionEvent.Failure(
                                error.localizedMessage ?: "Failed to sync game statistics"
                            )
                        )
                    }
                },
            )
        }
    }

    private companion object {
        const val TAG = "StatisticsVM"
    }
}
