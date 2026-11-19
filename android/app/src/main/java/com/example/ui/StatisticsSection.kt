package com.example.ui

import android.content.res.Resources
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.HighlightOff
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import com.example.data.cloud.AggregatedGameStatistics
import com.example.data.cloud.GameStatistic
import com.example.data.cloud.StarChessGameCatalog
import com.example.data.cloud.StatisticsRepository
import com.example.data.cloud.onGoogleSignInClicked
import com.example.viewmodel.StatisticsActionEvent
import com.example.viewmodel.StatisticsUiState
import com.example.viewmodel.StatisticsViewModel
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun GameStatisticsSection(
    res: Resources,
    currentUser: FirebaseUser?,
    preferredLanguage: String,
    localGamesPlayed: Int,
    onCollectEntriesForCloud: suspend () -> Map<String, String>,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (currentUser == null) {
        StatisticsAuthGateCard(
            res = res,
            onShowMessage = onShowMessage,
            modifier = modifier,
        )
    } else {
        val uid = currentUser.uid
        key("stats_$uid") {
            AuthenticatedStatisticsCard(
                res = res,
                currentUserId = uid,
                preferredLanguage = preferredLanguage,
                localGamesPlayed = localGamesPlayed,
                onCollectEntriesForCloud = onCollectEntriesForCloud,
                onShowMessage = onShowMessage,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun StatisticsAuthGateCard(
    res: Resources,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isSigningIn by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("statistics_auth_gate_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.BarChart,
                    contentDescription = res.str(R.string.stats_section_title),
                    tint = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = res.str(R.string.stats_section_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Text(
                text = res.str(R.string.stats_visitor_gate_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(
                onClick = {
                    isSigningIn = true
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = { isSigningIn = false },
                        onAuthError = { msg ->
                            isSigningIn = false
                            onShowMessage(msg)
                        },
                        scope = scope,
                        onAuthCancelled = { isSigningIn = false },
                    )
                },
                enabled = !isSigningIn,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("statistics_sign_in_button"),
                shape = RoundedCornerShape(14.dp),
            ) {
                if (isSigningIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
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
        }
    }
}

@Composable
private fun AuthenticatedStatisticsCard(
    res: Resources,
    currentUserId: String,
    preferredLanguage: String,
    localGamesPlayed: Int,
    onCollectEntriesForCloud: suspend () -> Map<String, String>,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val statsViewModel: StatisticsViewModel = viewModel(
        key = "stats_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                StatisticsViewModel(StatisticsRepository(db), currentUserId)
            }
        },
    )

    val statsState by statsViewModel.statsState.collectAsStateWithLifecycle()
    val isSyncing by statsViewModel.isSyncing.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showPlayedOnly by remember { mutableStateOf(false) }

    LaunchedEffect(statsViewModel) {
        statsViewModel.events.collect { event ->
            when (event) {
                is StatisticsActionEvent.Synced -> {
                    val msg = if (event.updatedGames > 0) {
                        res.str(R.string.stats_sync_done, event.updatedGames)
                    } else {
                        res.str(R.string.stats_sync_up_to_date)
                    }
                    onShowMessage(msg)
                }
                is StatisticsActionEvent.Failure -> onShowMessage(event.message)
            }
        }
    }

    // Automatically push any new local match/duration updates to Firestore once loaded
    val loadedStatsReady = statsState is StatisticsUiState.Success
    LaunchedEffect(loadedStatsReady, localGamesPlayed) {
        if (loadedStatsReady) {
            val entries = onCollectEntriesForCloud()
            statsViewModel.syncFromLocalEntries(entries, notifyUser = false)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("firestore_statistics_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
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
                        imageVector = Icons.Rounded.QueryStats,
                        contentDescription = res.str(R.string.stats_section_title),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Column {
                        Text(
                            text = res.str(R.string.stats_section_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = res.str(R.string.stats_section_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val entries = onCollectEntriesForCloud()
                            statsViewModel.syncFromLocalEntries(entries, notifyUser = true)
                        }
                    },
                    enabled = !isSyncing,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("stats_sync_button"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Sync,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(res.str(R.string.stats_btn_sync))
                }
            }

            when (val state = statsState) {
                StatisticsUiState.Loading -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = res.str(R.string.stats_loading),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                is StatisticsUiState.Error -> {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                is StatisticsUiState.Success -> {
                    val agg = state.stats
                    AggregateStatsBanner(res = res, agg = agg)

                    if (agg.totalMatches > 0L) {
                        WinLossDistributionBar(
                            wins = agg.totalWins,
                            draws = agg.totalDraws,
                            losses = agg.totalLosses,
                            winColor = MaterialTheme.colorScheme.primary,
                            drawColor = MaterialTheme.colorScheme.tertiary,
                            lossColor = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        Text(
                            text = res.str(R.string.stats_empty_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = !showPlayedOnly,
                                onClick = { showPlayedOnly = false },
                                label = { Text(res.str(R.string.stats_filter_all)) },
                                modifier = Modifier.testTag("stats_filter_all_chip"),
                            )
                            FilterChip(
                                selected = showPlayedOnly,
                                onClick = { showPlayedOnly = true },
                                label = { Text(res.str(R.string.stats_filter_played)) },
                                modifier = Modifier.testTag("stats_filter_played_chip"),
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CloudDone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp),
                                )
                                Text(
                                    text = res.str(R.string.stats_games_explored, agg.playedGamesCount),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }

                    val displayedGames = remember(agg.allGames, showPlayedOnly) {
                        if (showPlayedOnly) {
                            agg.allGames.filter { it.totalMatches > 0L || it.playDurationSeconds > 0L }
                        } else {
                            agg.allGames
                        }
                    }

                    if (displayedGames.isEmpty()) {
                        Text(
                            text = res.str(R.string.stats_empty_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            displayedGames.forEach { stat ->
                                PerGameStatisticRow(
                                    res = res,
                                    stat = stat,
                                    preferredLanguage = preferredLanguage,
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
private fun AggregateStatsBanner(
    res: Resources,
    agg: AggregatedGameStatistics,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MiniStatCard(
                label = res.str(R.string.stats_total_wins),
                value = agg.totalWins.toString(),
                icon = Icons.Rounded.EmojiEvents,
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("stats_total_wins_tile"),
            )
            MiniStatCard(
                label = res.str(R.string.stats_total_losses),
                value = agg.totalLosses.toString(),
                icon = Icons.Rounded.HighlightOff,
                accent = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .weight(1f)
                    .testTag("stats_total_losses_tile"),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MiniStatCard(
                label = res.str(R.string.stats_play_duration),
                value = formatPlayDuration(agg.totalPlayDurationSeconds),
                icon = Icons.Rounded.Schedule,
                accent = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("stats_play_duration_tile"),
            )
            MiniStatCard(
                label = res.str(R.string.stats_win_rate),
                value = "${agg.overallWinRatePercent}%",
                icon = Icons.Rounded.BarChart,
                accent = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("stats_win_rate_tile"),
            )
        }
    }
}

@Composable
private fun MiniStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accent,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
private fun WinLossDistributionBar(
    wins: Long,
    draws: Long,
    losses: Long,
    winColor: Color,
    drawColor: Color,
    lossColor: Color,
) {
    val total = (wins + draws + losses).coerceAtLeast(1L).toFloat()
    val winFrac = wins / total
    val drawFrac = draws / total
    val lossFrac = losses / total

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(999.dp))
    ) {
        val w = size.width
        val h = size.height
        val cr = CornerRadius(h / 2f, h / 2f)
        var x = 0f
        if (winFrac > 0f) {
            val segW = w * winFrac
            drawRoundRect(
                color = winColor,
                topLeft = Offset(x, 0f),
                size = Size(segW, h),
                cornerRadius = cr,
            )
            x += segW
        }
        if (drawFrac > 0f) {
            val segW = w * drawFrac
            drawRoundRect(
                color = drawColor,
                topLeft = Offset(x, 0f),
                size = Size(segW, h),
                cornerRadius = cr,
            )
            x += segW
        }
        if (lossFrac > 0f) {
            val segW = (w - x).coerceAtLeast(0f)
            drawRoundRect(
                color = lossColor,
                topLeft = Offset(x, 0f),
                size = Size(segW, h),
                cornerRadius = cr,
            )
        }
    }
}

@Composable
private fun PerGameStatisticRow(
    res: Resources,
    stat: GameStatistic,
    preferredLanguage: String,
) {
    val meta = remember(stat.gameId) {
        StarChessGameCatalog.ALL_26_GAMES.firstOrNull { it.gameId == stat.gameId }
    }
    val gameTitle = meta?.localizedName(preferredLanguage) ?: stat.gameId
    val hasActivity = stat.totalMatches > 0L || stat.playDurationSeconds > 0L

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("game_stat_row_${stat.gameId}"),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (hasActivity) 1f else 0.62f),
        border = BorderStroke(
            1.dp,
            if (hasActivity) MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = gameTitle,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (hasActivity) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (hasActivity) {
                        res.str(R.string.stats_row_wdl, stat.wins.toInt(), stat.losses.toInt(), stat.draws.toInt())
                    } else {
                        res.str(R.string.stats_unplayed)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = if (hasActivity) 0.85f else 0.45f),
                ) {
                    Text(
                        text = formatPlayDuration(stat.playDurationSeconds),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (hasActivity) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Text(
                        text = "${stat.winRatePercent}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (hasActivity) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    )
                }
            }
        }
    }
}

fun formatPlayDuration(totalSeconds: Long): String {
    val safe = totalSeconds.coerceAtLeast(0L)
    val hours = safe / 3600L
    val minutes = (safe % 3600L) / 60L
    val seconds = safe % 60L
    return when {
        hours > 0L -> "${hours}h ${minutes}m"
        minutes > 0L -> "${minutes}m ${seconds}s"
        seconds > 0L -> "${seconds}s"
        else -> "0m"
    }
}
