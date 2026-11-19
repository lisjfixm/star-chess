package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.DailyBonusOffer
import com.example.data.local.DailyLoginRewardEntity
import com.example.data.local.LocalArchiveRepository
import com.example.data.local.LocalSnapshotEntity
import com.example.data.local.PreferencesStore
import com.example.data.local.ProgressionSummary
import com.example.data.local.ShellPreferences
import com.example.data.local.StarChessDatabase
import com.example.engine.StarChessWebController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

enum class AppDestination {
    ARENA,
    NEXUS,
    TRIALS,
    PROFILE,
}

sealed interface LocalEvent {
    data object SnapshotSaved : LocalEvent
    data object SnapshotRestored : LocalEvent
    data object CloudRestored : LocalEvent
    data object ConflictKeptCloud : LocalEvent
    data class DailyBonusClaimed(val coins: Int, val streakDay: Int) : LocalEvent
}

class StarChessViewModel(
    appContext: Context,
    private val repository: LocalArchiveRepository,
) : ViewModel() {

    val entriesMap: StateFlow<Map<String, String>> = repository.entriesMapFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyMap(),
        )

    val localSaveUpdatedAtMs: StateFlow<Long> = repository.localSaveTimestampMsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = 0L,
        )

    val summary: StateFlow<ProgressionSummary> = repository.summaryFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = ProgressionSummary(),
        )

    val snapshots: StateFlow<List<LocalSnapshotEntity>> = repository.snapshotsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList(),
        )

    val preferences: StateFlow<ShellPreferences> = repository.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = ShellPreferences(
                preferredLanguage = PreferencesStore.defaultLanguageForLocale(),
                soundEnabled = true,
                bgmEnabled = true,
                lastCloudSyncMs = 0L,
            ),
        )

    val dailyRewardRecord: StateFlow<DailyLoginRewardEntity?> = repository.dailyRewardFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null,
        )

    private val _pendingDailyBonus = MutableStateFlow<DailyBonusOffer?>(null)
    val pendingDailyBonus: StateFlow<DailyBonusOffer?> = _pendingDailyBonus.asStateFlow()

    private var dailyBonusCheckedThisSession = false

    private val _currentTab = MutableStateFlow(AppDestination.ARENA)
    val currentTab: StateFlow<AppDestination> = _currentTab.asStateFlow()

    private val _hasEngineSubView = MutableStateFlow(false)
    val hasEngineSubView: StateFlow<Boolean> = _hasEngineSubView.asStateFlow()

    private val _events = MutableSharedFlow<LocalEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<LocalEvent> = _events.asSharedFlow()

    val webController: StarChessWebController = StarChessWebController(
        appContext = appContext,
        scope = viewModelScope,
        initialEntriesProvider = {
            runBlocking(Dispatchers.IO) {
                repository.getAllEntriesMap()
            }
        },
        onStorageSet = { key, value ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.upsertStorageKey(key, value)
            }
        },
        onStorageRemove = { key ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.removeStorageKey(key)
            }
        },
        onStorageClear = {
            viewModelScope.launch(Dispatchers.IO) {
                repository.clearStorageKeys()
            }
        },
        onEngineBackDepthChanged = { hasSubView ->
            _hasEngineSubView.value = hasSubView
        },
        onPlayDurationTick = { gameId, deltaSec ->
            viewModelScope.launch(Dispatchers.IO) {
                repository.recordPlayDurationDelta(gameId, deltaSec)
            }
        },
    )

    fun checkDailyLoginBonus() {
        if (dailyBonusCheckedThisSession) return
        dailyBonusCheckedThisSession = true
        viewModelScope.launch(Dispatchers.IO) {
            val offer = repository.checkPendingDailyBonus()
            if (offer != null) {
                _pendingDailyBonus.value = offer
            }
        }
    }

    fun claimDailyBonus() {
        val offer = _pendingDailyBonus.value ?: return
        _pendingDailyBonus.value = null
        viewModelScope.launch(Dispatchers.IO) {
            val updatedCoins = repository.claimDailyBonus(offer)
            webController.updateStarCoinsLive(updatedCoins)
            _events.tryEmit(LocalEvent.DailyBonusClaimed(offer.rewardCoins, offer.streakDay))
        }
    }

    fun dismissDailyBonus() {
        _pendingDailyBonus.value = null
    }

    fun selectDestination(destination: AppDestination) {
        _currentTab.value = destination
        when (destination) {
            AppDestination.ARENA -> webController.openArena()
            AppDestination.NEXUS -> webController.openAstralNexus()
            AppDestination.TRIALS -> webController.openTrials()
            AppDestination.PROFILE -> Unit
        }
    }

    fun handleBackPress() {
        if (_currentTab.value == AppDestination.PROFILE) {
            _currentTab.value = AppDestination.ARENA
            return
        }
        if (_hasEngineSubView.value) {
            webController.dispatchEngineBack()
            if (_currentTab.value != AppDestination.ARENA) {
                _currentTab.value = AppDestination.ARENA
            }
        }
    }

    fun switchLanguage(code: String) {
        viewModelScope.launch {
            repository.setLanguage(code)
            webController.setLanguage(code)
        }
    }

    fun selectCosmicTheme(themeKey: String) {
        viewModelScope.launch {
            val palette = com.example.ui.theme.CosmicPalettes.find(themeKey)
            repository.setCosmicTheme(palette.key)
            webController.applyCosmicPalette(
                primaryHex = palette.webHexPrimary,
                secondaryHex = palette.webHexSecondary,
                bgHex = palette.webHexBg,
            )
        }
    }

    fun toggleSound() {
        val next = !preferences.value.soundEnabled
        viewModelScope.launch {
            repository.setSoundEnabled(next)
            webController.toggleSound()
        }
    }

    fun toggleBgm() {
        val next = !preferences.value.bgmEnabled
        viewModelScope.launch {
            repository.setBgmEnabled(next)
            webController.toggleBgm()
        }
    }

    fun saveLocalCheckpoint(labelPrefix: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val timestampLabel = "$labelPrefix ${snapshots.value.size + 1}"
            repository.createLocalCheckpoint(timestampLabel)
            _events.tryEmit(LocalEvent.SnapshotSaved)
        }
    }

    fun restoreLocalCheckpoint(snapshotId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val map = repository.loadCheckpointMap(snapshotId) ?: return@launch
            repository.replaceAllStorageEntries(map)
            webController.applyEntriesAndReload(map)
            _events.tryEmit(LocalEvent.SnapshotRestored)
        }
    }

    fun deleteLocalCheckpoint(snapshotId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCheckpoint(snapshotId)
        }
    }

    suspend fun snapshotEntriesForCloud(): Map<String, String> {
        return repository.getAllEntriesMap()
    }

    fun applyCloudRestoredEntries(entries: Map<String, String>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.replaceAllStorageEntries(entries)
            repository.markCloudSynced()
            webController.applyEntriesAndReload(entries)
            _events.tryEmit(LocalEvent.CloudRestored)
        }
    }

    fun resolveConflictKeepingCloud(entries: Map<String, String>, safetyBackupPrefix: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.createSafetyCheckpointIfNeeded("$safetyBackupPrefix ${snapshots.value.size + 1}")
            repository.replaceAllStorageEntries(entries)
            repository.markCloudSynced()
            webController.applyEntriesAndReload(entries)
            _events.tryEmit(LocalEvent.ConflictKeptCloud)
        }
    }

    fun markCloudSynced() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.markCloudSynced()
        }
    }

    override fun onCleared() {
        super.onCleared()
        webController.destroy()
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val db = StarChessDatabase.getInstance(app)
                val prefs = PreferencesStore(app)
                val repo = LocalArchiveRepository(db.starChessDao(), prefs)
                StarChessViewModel(app.applicationContext, repo)
            }
        }
    }
}
