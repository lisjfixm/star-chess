package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.cloud.CloudSaveBundle
import com.example.data.cloud.CloudSyncRepository
import com.example.data.cloud.SaveConflictDetector
import com.example.data.cloud.SaveConflictState
import com.example.data.local.ProgressionSummary
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface CloudSavesUiState {
    data object Loading : CloudSavesUiState
    data class Success(val saves: List<CloudSaveBundle>) : CloudSavesUiState
    data class Error(val message: String) : CloudSavesUiState
}

sealed interface CloudActionEvent {
    data object BackupSuccess : CloudActionEvent
    data object ConflictResolvedKeptLocal : CloudActionEvent
    data class Failure(val message: String) : CloudActionEvent
}

class CloudSyncViewModel(
    private val repository: CloudSyncRepository,
    private val currentUserId: String,
) : ViewModel() {

    val cloudSavesState: StateFlow<CloudSavesUiState> = repository.observeCloudSaves(currentUserId)
        .map<List<CloudSaveBundle>, CloudSavesUiState> { CloudSavesUiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing cloud saves", rootError)
            emit(CloudSavesUiState.Error(rootError.localizedMessage ?: "Failed to load cloud saves"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = CloudSavesUiState.Loading,
        )

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _activeConflict = MutableStateFlow<SaveConflictState?>(null)
    val activeConflict: StateFlow<SaveConflictState?> = _activeConflict.asStateFlow()

    private val snoozedFingerprints = MutableStateFlow<Set<String>>(emptySet())

    private val _events = MutableSharedFlow<CloudActionEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<CloudActionEvent> = _events.asSharedFlow()

    fun evaluateAutoConflict(
        localEntries: Map<String, String>,
        localUpdatedAtMs: Long,
        snapshotsCount: Int,
        lastCloudSyncMs: Long,
    ) {
        val state = cloudSavesState.value
        if (state !is CloudSavesUiState.Success) return

        val currentActive = _activeConflict.value
        val detected = SaveConflictDetector.detectAutomaticConflict(
            localEntries = localEntries,
            localUpdatedAtMs = localUpdatedAtMs,
            snapshotsCount = snapshotsCount,
            cloudSaves = state.saves,
            lastCloudSyncMs = lastCloudSyncMs,
            snoozedFingerprints = snoozedFingerprints.value,
        )

        if (detected != null) {
            if (currentActive == null || currentActive.conflictFingerprint != detected.conflictFingerprint) {
                _activeConflict.value = detected
            }
        } else if (currentActive != null) {
            // Clear stale dialog only if local and that bundle are now identical
            val matchingCloud = state.saves.firstOrNull { it.id == currentActive.cloudBundle.id }
            if (matchingCloud != null &&
                !SaveConflictDetector.arePayloadsDifferent(localEntries, matchingCloud.entries)
            ) {
                _activeConflict.value = null
            }
        }
    }

    fun presentConflict(conflict: SaveConflictState) {
        _activeConflict.value = conflict
    }

    fun dismissConflictForLater() {
        val active = _activeConflict.value ?: return
        snoozedFingerprints.update { it + active.conflictFingerprint }
        _activeConflict.value = null
    }

    fun markConflictResolved(conflictFingerprint: String) {
        snoozedFingerprints.update { it + conflictFingerprint }
        _activeConflict.value = null
    }

    fun resolveConflictKeepingLocal(
        conflict: SaveConflictState,
        primaryLabel: String,
        preferredLanguage: String,
        onLocalKeptSuccess: () -> Unit,
    ) {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            val entries = conflict.local.entries
            val summary = ProgressionSummary.fromEntries(entries)
            val result = repository.upsertCloudSave(
                userId = currentUserId,
                saveId = "primary",
                label = primaryLabel,
                entries = entries,
                summary = summary,
                preferredLanguage = preferredLanguage,
            )
            _isBusy.value = false
            result.fold(
                onSuccess = {
                    snoozedFingerprints.update { it + conflict.conflictFingerprint }
                    _activeConflict.value = null
                    onLocalKeptSuccess()
                    _events.tryEmit(CloudActionEvent.ConflictResolvedKeptLocal)
                },
                onFailure = { error ->
                    _events.tryEmit(CloudActionEvent.Failure(error.localizedMessage ?: "Failed to upload local save"))
                },
            )
        }
    }

    fun backupToCloud(
        saveId: String,
        label: String,
        entries: Map<String, String>,
        summary: ProgressionSummary,
        preferredLanguage: String,
        onSuccess: () -> Unit,
    ) {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.upsertCloudSave(
                userId = currentUserId,
                saveId = saveId,
                label = label,
                entries = entries,
                summary = summary,
                preferredLanguage = preferredLanguage,
            )
            _isBusy.value = false
            result.fold(
                onSuccess = {
                    onSuccess()
                    _events.tryEmit(CloudActionEvent.BackupSuccess)
                },
                onFailure = { error ->
                    _events.tryEmit(CloudActionEvent.Failure(error.localizedMessage ?: "Cloud backup failed"))
                },
            )
        }
    }

    fun deleteCloudSave(saveId: String) {
        if (_isBusy.value) return
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.deleteCloudSave(currentUserId, saveId)
            _isBusy.value = false
            result.onFailure { error ->
                _events.tryEmit(CloudActionEvent.Failure(error.localizedMessage ?: "Failed to delete cloud save"))
            }
        }
    }

    private companion object {
        const val TAG = "CloudSyncVM"
    }
}
