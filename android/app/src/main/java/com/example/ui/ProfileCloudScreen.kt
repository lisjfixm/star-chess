package com.example.ui

import android.content.res.Resources
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.cloud.CloudSaveBundle
import com.example.data.cloud.CloudSyncRepository
import com.example.data.cloud.SaveConflictDetector
import com.example.data.cloud.SaveConflictState
import com.example.data.cloud.authStateFlow
import com.example.data.cloud.onGoogleSignInClicked
import com.example.data.cloud.signOutFromCloud
import com.example.data.local.DailyLoginRewardEntity
import com.example.data.local.LocalSnapshotEntity
import com.example.data.local.ProgressionSummary
import com.example.viewmodel.CloudSavesUiState
import com.example.viewmodel.CloudSyncViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun ProfileCloudScreen(
    res: Resources,
    summary: ProgressionSummary,
    snapshots: List<LocalSnapshotEntity>,
    dailyRewardRecord: DailyLoginRewardEntity?,
    preferredLanguage: String,
    activeThemeKey: String,
    localEntries: Map<String, String>,
    localSaveUpdatedAtMs: Long,
    lastCloudSyncMs: Long,
    cloudViewModel: CloudSyncViewModel?,
    onSelectTheme: (String) -> Unit,
    onSaveLocalCheckpoint: (String) -> Unit,
    onRestoreLocalCheckpoint: (Long) -> Unit,
    onDeleteLocalCheckpoint: (Long) -> Unit,
    onCollectEntriesForCloud: suspend () -> Map<String, String>,
    onApplyCloudRestore: (Map<String, String>) -> Unit,
    onMarkCloudSynced: () -> Unit,
    onExitVisitorMode: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface,
                    )
                )
            )
    ) {
        val isWideLayout = maxWidth >= 720.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 960.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = res.str(R.string.profile_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        text = res.str(R.string.profile_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    dailyRewardRecord?.let { reward ->
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
                            modifier = Modifier.testTag("profile_daily_checkin_badge"),
                        ) {
                            Text(
                                text = res.str(
                                    R.string.daily_bonus_last_checkin,
                                    reward.lastLoginDate,
                                    reward.consecutiveDays,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }

                    CommanderStatsGrid(res = res, summary = summary, isWide = isWideLayout)
                }
            }

            item {
                CosmicThemeSwitcherCard(
                    res = res,
                    activeThemeKey = activeThemeKey,
                    onSelectTheme = onSelectTheme,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 960.dp),
                )
            }

            item {
                GlobalLeaderboardSection(
                    res = res,
                    currentUser = currentUser,
                    activeThemeKey = activeThemeKey,
                    localGamesPlayed = summary.gamesPlayed,
                    onCollectEntriesForCloud = onCollectEntriesForCloud,
                    onShowMessage = onShowMessage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 960.dp),
                )
            }

            item {
                GameStatisticsSection(
                    res = res,
                    currentUser = currentUser,
                    preferredLanguage = preferredLanguage,
                    localGamesPlayed = summary.gamesPlayed,
                    onCollectEntriesForCloud = onCollectEntriesForCloud,
                    onShowMessage = onShowMessage,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 960.dp),
                )
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 960.dp),
                ) {
                    if (isWideLayout) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            LocalVaultCard(
                                res = res,
                                summary = summary,
                                snapshots = snapshots,
                                onSaveLocalCheckpoint = onSaveLocalCheckpoint,
                                onRestoreLocalCheckpoint = onRestoreLocalCheckpoint,
                                onDeleteLocalCheckpoint = onDeleteLocalCheckpoint,
                                modifier = Modifier.weight(1f),
                            )
                            CloudSyncSection(
                                res = res,
                                currentUser = currentUser,
                                summary = summary,
                                preferredLanguage = preferredLanguage,
                                localEntries = localEntries,
                                localSaveUpdatedAtMs = localSaveUpdatedAtMs,
                                snapshotsCount = snapshots.size,
                                lastCloudSyncMs = lastCloudSyncMs,
                                cloudViewModel = cloudViewModel,
                                onCollectEntriesForCloud = onCollectEntriesForCloud,
                                onApplyCloudRestore = onApplyCloudRestore,
                                onMarkCloudSynced = onMarkCloudSynced,
                                onExitVisitorMode = onExitVisitorMode,
                                onShowMessage = onShowMessage,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            CloudSyncSection(
                                res = res,
                                currentUser = currentUser,
                                summary = summary,
                                preferredLanguage = preferredLanguage,
                                localEntries = localEntries,
                                localSaveUpdatedAtMs = localSaveUpdatedAtMs,
                                snapshotsCount = snapshots.size,
                                lastCloudSyncMs = lastCloudSyncMs,
                                cloudViewModel = cloudViewModel,
                                onCollectEntriesForCloud = onCollectEntriesForCloud,
                                onApplyCloudRestore = onApplyCloudRestore,
                                onMarkCloudSynced = onMarkCloudSynced,
                                onExitVisitorMode = onExitVisitorMode,
                                onShowMessage = onShowMessage,
                            )
                            LocalVaultCard(
                                res = res,
                                summary = summary,
                                snapshots = snapshots,
                                onSaveLocalCheckpoint = onSaveLocalCheckpoint,
                                onRestoreLocalCheckpoint = onRestoreLocalCheckpoint,
                                onDeleteLocalCheckpoint = onDeleteLocalCheckpoint,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommanderStatsGrid(
    res: Resources,
    summary: ProgressionSummary,
    isWide: Boolean,
) {
    val stats = listOf(
        Triple(res.str(R.string.stat_star_coins), summary.starCoins.toString(), Icons.Rounded.Stars),
        Triple(res.str(R.string.stat_peak_elo), summary.eloPeak.toString(), Icons.Rounded.Leaderboard),
        Triple(res.str(R.string.stat_achievements), summary.achievementsCount.toString(), Icons.Rounded.EmojiEvents),
        Triple(res.str(R.string.stat_games_played), summary.gamesPlayed.toString(), Icons.Rounded.SportsEsports),
    )

    if (isWide) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            stats.forEachIndexed { idx, (label, value, icon) ->
                val accent = when (idx) {
                    0 -> MaterialTheme.colorScheme.tertiary
                    1 -> MaterialTheme.colorScheme.primary
                    2 -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.primary
                }
                StatTile(
                    label = label,
                    value = value,
                    icon = icon,
                    accent = accent,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatTile(
                    label = stats[0].first,
                    value = stats[0].second,
                    icon = stats[0].third,
                    accent = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = stats[1].first,
                    value = stats[1].second,
                    icon = stats[1].third,
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatTile(
                    label = stats[2].first,
                    value = stats[2].second,
                    icon = stats[2].third,
                    accent = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                )
                StatTile(
                    label = stats[3].first,
                    value = stats[3].second,
                    icon = stats[3].third,
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accent,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun LocalVaultCard(
    res: Resources,
    summary: ProgressionSummary,
    snapshots: List<LocalSnapshotEntity>,
    onSaveLocalCheckpoint: (String) -> Unit,
    onRestoreLocalCheckpoint: (Long) -> Unit,
    onDeleteLocalCheckpoint: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Storage,
                        contentDescription = res.str(R.string.local_vault_title),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = res.str(R.string.local_vault_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = res.str(R.string.label_keys_count, summary.storedKeysCount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Text(
                text = res.str(R.string.local_vault_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FilledTonalButton(
                onClick = { onSaveLocalCheckpoint(res.str(R.string.label_checkpoint_prefix)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("save_local_checkpoint_button"),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(res.str(R.string.btn_save_local_snapshot))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            if (snapshots.isEmpty()) {
                Text(
                    text = res.str(R.string.empty_local_snapshots),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    snapshots.forEach { snapshot ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("local_snapshot_card_${snapshot.id}"),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = snapshot.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "${res.str(R.string.label_coins_elo, snapshot.starCoins, snapshot.eloPeak)} · ${dateFormat.format(Date(snapshot.createdAtMs))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                OutlinedButton(
                                    onClick = { onRestoreLocalCheckpoint(snapshot.id) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("restore_local_snapshot_${snapshot.id}"),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(10.dp),
                                ) {
                                    Text(res.str(R.string.btn_restore_snapshot))
                                }
                                IconButton(
                                    onClick = { onDeleteLocalCheckpoint(snapshot.id) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("delete_local_snapshot_${snapshot.id}"),
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = res.str(R.string.btn_delete_snapshot),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudSyncSection(
    res: Resources,
    currentUser: FirebaseUser?,
    summary: ProgressionSummary,
    preferredLanguage: String,
    localEntries: Map<String, String>,
    localSaveUpdatedAtMs: Long,
    snapshotsCount: Int,
    lastCloudSyncMs: Long,
    cloudViewModel: CloudSyncViewModel?,
    onCollectEntriesForCloud: suspend () -> Map<String, String>,
    onApplyCloudRestore: (Map<String, String>) -> Unit,
    onMarkCloudSynced: () -> Unit,
    onExitVisitorMode: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (currentUser == null) {
        CloudSignInGateCard(
            res = res,
            onExitVisitorMode = onExitVisitorMode,
            onShowMessage = onShowMessage,
            modifier = modifier,
        )
    } else {
        val uid = currentUser.uid
        key(uid) {
            AuthenticatedCloudSyncCard(
                res = res,
                user = currentUser,
                currentUserId = uid,
                summary = summary,
                preferredLanguage = preferredLanguage,
                localEntries = localEntries,
                localSaveUpdatedAtMs = localSaveUpdatedAtMs,
                snapshotsCount = snapshotsCount,
                lastCloudSyncMs = lastCloudSyncMs,
                sharedCloudViewModel = cloudViewModel,
                onCollectEntriesForCloud = onCollectEntriesForCloud,
                onApplyCloudRestore = onApplyCloudRestore,
                onMarkCloudSynced = onMarkCloudSynced,
                onShowMessage = onShowMessage,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun CloudSignInGateCard(
    res: Resources,
    onExitVisitorMode: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudOff,
                        contentDescription = res.str(R.string.cloud_sync_title),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                    Text(
                        text = res.str(R.string.cloud_sync_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        text = res.str(R.string.badge_visitor),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }
            }

            Text(
                text = res.str(R.string.cloud_auth_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            authError?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Button(
                onClick = {
                    authError = null
                    isSigningIn = true
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            isSigningIn = false
                            authError = null
                        },
                        onAuthError = { msg ->
                            isSigningIn = false
                            authError = msg
                            onShowMessage(msg)
                        },
                        scope = scope,
                        onAuthCancelled = {
                            isSigningIn = false
                        },
                    )
                },
                enabled = !isSigningIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("google_sign_in_button"),
                shape = RoundedCornerShape(14.dp),
            ) {
                if (isSigningIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Login,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(res.str(R.string.btn_sign_in_google))
                }
            }

            OutlinedButton(
                onClick = onExitVisitorMode,
                enabled = !isSigningIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("return_to_login_button"),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(res.str(R.string.btn_return_to_login))
            }
        }
    }
}

@Composable
private fun AuthenticatedCloudSyncCard(
    res: Resources,
    user: FirebaseUser,
    currentUserId: String,
    summary: ProgressionSummary,
    preferredLanguage: String,
    localEntries: Map<String, String>,
    localSaveUpdatedAtMs: Long,
    snapshotsCount: Int,
    lastCloudSyncMs: Long,
    sharedCloudViewModel: CloudSyncViewModel?,
    onCollectEntriesForCloud: suspend () -> Map<String, String>,
    onApplyCloudRestore: (Map<String, String>) -> Unit,
    onMarkCloudSynced: () -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cloudViewModel: CloudSyncViewModel = sharedCloudViewModel ?: viewModel(
        key = "cloud_sync_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                CloudSyncViewModel(CloudSyncRepository(db), currentUserId)
            }
        },
    )

    val cloudState by cloudViewModel.cloudSavesState.collectAsStateWithLifecycle()
    val isBusy by cloudViewModel.isBusy.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
    val divergentConflict: SaveConflictState? = remember(
        cloudState,
        localEntries,
        localSaveUpdatedAtMs,
        snapshotsCount,
    ) {
        val success = cloudState as? CloudSavesUiState.Success ?: return@remember null
        SaveConflictDetector.findDivergenceForBanner(
            localEntries = localEntries,
            localUpdatedAtMs = localSaveUpdatedAtMs,
            snapshotsCount = snapshotsCount,
            cloudSaves = success.saves,
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudDone,
                        contentDescription = res.str(R.string.cloud_sync_title),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column {
                        Text(
                            text = res.str(R.string.cloud_sync_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = user.email ?: user.displayName ?: currentUserId.take(10),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        signOutFromCloud(credentialManager, scope) {
                            onShowMessage(res.str(R.string.msg_signed_out))
                        }
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("google_sign_out_button"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Logout,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(res.str(R.string.btn_sign_out))
                }
            }

            divergentConflict?.let { conflict ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("conflict_warning_banner"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = res.str(R.string.conflict_banner_title),
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = res.str(R.string.conflict_banner_title),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = res.str(
                                    R.string.conflict_banner_desc,
                                    conflict.local.starCoins,
                                    conflict.local.gamesPlayed,
                                    conflict.cloud.starCoins,
                                    conflict.cloud.gamesPlayed,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        FilledTonalButton(
                            onClick = { cloudViewModel.presentConflict(conflict) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("resolve_conflict_banner_button"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text(res.str(R.string.conflict_banner_action))
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            val entries = onCollectEntriesForCloud()
                            val cloudSaves = (cloudState as? CloudSavesUiState.Success)?.saves.orEmpty()
                            val conflict = SaveConflictDetector.checkManualBackupConflict(
                                localEntries = entries,
                                localUpdatedAtMs = if (localSaveUpdatedAtMs > 0L) localSaveUpdatedAtMs else System.currentTimeMillis(),
                                snapshotsCount = snapshotsCount,
                                cloudSaves = cloudSaves,
                                lastCloudSyncMs = lastCloudSyncMs,
                            )
                            if (conflict != null) {
                                cloudViewModel.presentConflict(conflict)
                            } else {
                                cloudViewModel.backupToCloud(
                                    saveId = "primary",
                                    label = res.str(R.string.label_primary_sync),
                                    entries = entries,
                                    summary = summary,
                                    preferredLanguage = preferredLanguage,
                                    onSuccess = onMarkCloudSynced,
                                )
                            }
                        }
                    },
                    enabled = !isBusy,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("cloud_backup_primary_button"),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CloudUpload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = res.str(R.string.btn_cloud_backup),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                FilledTonalButton(
                    onClick = {
                        scope.launch {
                            val entries = onCollectEntriesForCloud()
                            val ts = System.currentTimeMillis()
                            cloudViewModel.backupToCloud(
                                saveId = "ckpt_$ts",
                                label = "${res.str(R.string.label_checkpoint_prefix)} ${dateFormat.format(Date(ts))}",
                                entries = entries,
                                summary = summary,
                                preferredLanguage = preferredLanguage,
                                onSuccess = onMarkCloudSynced,
                            )
                        }
                    },
                    enabled = !isBusy,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("cloud_new_checkpoint_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Backup,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = res.str(R.string.btn_cloud_checkpoint),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            when (val state = cloudState) {
                CloudSavesUiState.Loading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = res.str(R.string.loading_cloud),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                is CloudSavesUiState.Error -> {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                is CloudSavesUiState.Success -> {
                    if (state.saves.isEmpty()) {
                        Text(
                            text = res.str(R.string.cloud_empty_saves),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.saves.forEach { bundle ->
                                CloudSaveRow(
                                    res = res,
                                    bundle = bundle,
                                    dateFormat = dateFormat,
                                    isBusy = isBusy,
                                    onRestore = {
                                        scope.launch {
                                            val entries = onCollectEntriesForCloud()
                                            val conflict = SaveConflictDetector.checkManualRestoreConflict(
                                                localEntries = entries,
                                                localUpdatedAtMs = localSaveUpdatedAtMs,
                                                snapshotsCount = snapshotsCount,
                                                targetBundle = bundle,
                                                lastCloudSyncMs = lastCloudSyncMs,
                                            )
                                            if (conflict != null) {
                                                cloudViewModel.presentConflict(conflict)
                                            } else {
                                                onApplyCloudRestore(bundle.entries)
                                            }
                                        }
                                    },
                                    onDelete = {
                                        cloudViewModel.deleteCloudSave(bundle.id)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CloudSaveRow(
    res: Resources,
    bundle: CloudSaveBundle,
    dateFormat: DateFormat,
    isBusy: Boolean,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
) {
    val tsDate = bundle.updatedAt?.toDate() ?: Date(bundle.clientSavedAtMs)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cloud_save_card_${bundle.id}"),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bundle.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${res.str(R.string.label_coins_elo, bundle.starCoins.toInt(), bundle.eloPeak.toInt())} · ${bundle.deviceName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = dateFormat.format(tsDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
            }

            FilledTonalButton(
                onClick = onRestore,
                enabled = !isBusy,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("restore_cloud_save_${bundle.id}"),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(res.str(R.string.btn_restore_snapshot))
            }

            IconButton(
                onClick = onDelete,
                enabled = !isBusy,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("delete_cloud_save_${bundle.id}"),
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = res.str(R.string.btn_delete_snapshot),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}
