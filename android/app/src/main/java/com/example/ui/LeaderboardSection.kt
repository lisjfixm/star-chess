package com.example.ui

import android.content.res.Resources
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Leaderboard
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Publish
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
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
import com.example.data.cloud.LeaderboardEntry
import com.example.data.cloud.LeaderboardRepository
import com.example.data.cloud.onGoogleSignInClicked
import com.example.ui.theme.CosmicPalettes
import com.example.ui.theme.RankBronze
import com.example.ui.theme.RankGold
import com.example.ui.theme.RankSilver
import com.example.viewmodel.LeaderboardActionEvent
import com.example.viewmodel.LeaderboardUiState
import com.example.viewmodel.LeaderboardViewModel
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

@Composable
fun GlobalLeaderboardSection(
    res: Resources,
    currentUser: FirebaseUser?,
    activeThemeKey: String,
    localGamesPlayed: Int,
    onCollectEntriesForCloud: suspend () -> Map<String, String>,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (currentUser == null) {
        LeaderboardAuthGateCard(
            res = res,
            onShowMessage = onShowMessage,
            modifier = modifier,
        )
    } else {
        val uid = currentUser.uid
        key("leaderboard_$uid") {
            AuthenticatedLeaderboardCard(
                res = res,
                user = currentUser,
                currentUserId = uid,
                activeThemeKey = activeThemeKey,
                localGamesPlayed = localGamesPlayed,
                onCollectEntriesForCloud = onCollectEntriesForCloud,
                onShowMessage = onShowMessage,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun LeaderboardAuthGateCard(
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
            .testTag("leaderboard_auth_gate_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)),
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
                    imageVector = Icons.Rounded.Leaderboard,
                    contentDescription = res.str(R.string.leaderboard_title),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    text = res.str(R.string.leaderboard_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Text(
                text = res.str(R.string.leaderboard_visitor_gate_desc),
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
                    .testTag("leaderboard_sign_in_button"),
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
private fun AuthenticatedLeaderboardCard(
    res: Resources,
    user: FirebaseUser,
    currentUserId: String,
    activeThemeKey: String,
    localGamesPlayed: Int,
    onCollectEntriesForCloud: suspend () -> Map<String, String>,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val leaderboardViewModel: LeaderboardViewModel = viewModel(
        key = "leaderboard_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                LeaderboardViewModel(LeaderboardRepository(db), currentUserId)
            }
        },
    )

    val uiState by leaderboardViewModel.leaderboardState.collectAsStateWithLifecycle()
    val isPublishing by leaderboardViewModel.isPublishing.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    LaunchedEffect(leaderboardViewModel) {
        leaderboardViewModel.events.collect { event ->
            when (event) {
                LeaderboardActionEvent.Published -> onShowMessage(res.str(R.string.leaderboard_published_msg))
                is LeaderboardActionEvent.Failure -> onShowMessage(event.message)
            }
        }
    }

    // Keep the signed-in player's global ranking fresh when matches or palette change
    LaunchedEffect(currentUserId, localGamesPlayed, activeThemeKey) {
        val entries = onCollectEntriesForCloud()
        leaderboardViewModel.publishFromLocalEntries(
            rawDisplayName = user.displayName,
            entries = entries,
            favoriteTheme = activeThemeKey,
            notifyUser = false,
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("global_leaderboard_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.42f)),
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
                        imageVector = Icons.Rounded.Leaderboard,
                        contentDescription = res.str(R.string.leaderboard_title),
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                    Column {
                        Text(
                            text = res.str(R.string.leaderboard_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = res.str(R.string.leaderboard_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val entries = onCollectEntriesForCloud()
                            leaderboardViewModel.publishFromLocalEntries(
                                rawDisplayName = user.displayName,
                                entries = entries,
                                favoriteTheme = activeThemeKey,
                                notifyUser = true,
                            )
                        }
                    },
                    enabled = !isPublishing,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("leaderboard_publish_button"),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    if (isPublishing) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.Publish,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(res.str(R.string.leaderboard_btn_publish))
                }
            }

            when (val state = uiState) {
                LeaderboardUiState.Loading -> {
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
                            text = res.str(R.string.leaderboard_loading),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                is LeaderboardUiState.Error -> {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                is LeaderboardUiState.Success -> {
                    val entries = state.entries
                    val myIndex = entries.indexOfFirst { it.userId == currentUserId || it.id == currentUserId }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("leaderboard_my_rank_banner"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (myIndex >= 0) {
                                    res.str(R.string.leaderboard_your_rank, myIndex + 1)
                                } else {
                                    res.str(R.string.leaderboard_unranked)
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                            )
                            if (myIndex >= 0) {
                                val me = entries[myIndex]
                                Text(
                                    text = res.str(R.string.leaderboard_wins_label, me.totalWins.toInt()),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }

                    if (entries.isEmpty()) {
                        Text(
                            text = res.str(R.string.leaderboard_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    } else {
                        // Top 3 Podium strip when at least 1 player exists
                        val topThree = entries.take(3)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            topThree.forEachIndexed { idx, entry ->
                                PodiumTile(
                                    res = res,
                                    rank = idx + 1,
                                    entry = entry,
                                    isCurrentUser = entry.userId == currentUserId || entry.id == currentUserId,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            entries.forEachIndexed { idx, entry ->
                                LeaderboardPlayerRow(
                                    res = res,
                                    rank = idx + 1,
                                    entry = entry,
                                    isCurrentUser = entry.userId == currentUserId || entry.id == currentUserId,
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
private fun PodiumTile(
    res: Resources,
    rank: Int,
    entry: LeaderboardEntry,
    isCurrentUser: Boolean,
    modifier: Modifier = Modifier,
) {
    val medalColor = when (rank) {
        1 -> RankGold
        2 -> RankSilver
        else -> RankBronze
    }
    val medalIcon = when (rank) {
        1 -> Icons.Rounded.EmojiEvents
        2 -> Icons.Rounded.WorkspacePremium
        else -> Icons.Rounded.MilitaryTech
    }

    Surface(
        modifier = modifier.testTag("leaderboard_podium_$rank"),
        shape = RoundedCornerShape(14.dp),
        color = if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.2.dp, medalColor.copy(alpha = 0.55f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = medalIcon,
                contentDescription = "#$rank",
                tint = medalColor,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = "#$rank",
                style = MaterialTheme.typography.labelSmall,
                color = medalColor,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = entry.displayName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = res.str(R.string.leaderboard_wins_label, entry.totalWins.toInt()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun LeaderboardPlayerRow(
    res: Resources,
    rank: Int,
    entry: LeaderboardEntry,
    isCurrentUser: Boolean,
) {
    val accentColor: Color = when (rank) {
        1 -> RankGold
        2 -> RankSilver
        3 -> RankBronze
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val paletteTint = remember(entry.favoriteTheme) {
        CosmicPalettes.find(entry.favoriteTheme).primaryPreview
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("leaderboard_row_$rank"),
        shape = RoundedCornerShape(12.dp),
        color = if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            width = if (isCurrentUser) 1.2.dp else 1.dp,
            color = if (isCurrentUser) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
            else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$rank",
                    style = MaterialTheme.typography.labelMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                )
            }

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(paletteTint)
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = entry.displayName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (isCurrentUser) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = res.str(R.string.leaderboard_you_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
                Text(
                    text = res.str(
                        R.string.leaderboard_meta_line,
                        entry.winRatePercent,
                        entry.gamesPlayedCount.toInt(),
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
            ) {
                Text(
                    text = res.str(R.string.leaderboard_wins_label, entry.totalWins.toInt()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}
