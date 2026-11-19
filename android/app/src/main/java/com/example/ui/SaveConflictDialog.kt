package com.example.ui

import android.content.res.Resources
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.CompareArrows
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.cloud.ConflictKeepChoice
import com.example.data.cloud.SaveConflictState
import com.example.data.cloud.SaveVersionSnapshot
import java.text.DateFormat
import java.util.Date

@Composable
fun SaveConflictResolutionDialog(
    res: Resources,
    conflict: SaveConflictState,
    isBusy: Boolean,
    onKeepLocal: () -> Unit,
    onKeepCloud: () -> Unit,
    onDecideLater: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }

    Dialog(
        onDismissRequest = {
            if (!isBusy) onDecideLater()
        },
        properties = DialogProperties(
            dismissOnBackPress = !isBusy,
            dismissOnClickOutside = !isBusy,
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .fillMaxWidth()
                .widthIn(max = 640.dp)
                .testTag("save_conflict_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 8.dp,
            border = BorderStroke(
                width = 1.4.dp,
                brush = Brush.linearGradient(
                    listOf(
                        tertiary.copy(alpha = 0.72f),
                        primary.copy(alpha = 0.62f),
                        secondary.copy(alpha = 0.48f),
                    )
                ),
            ),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    tertiary.copy(alpha = 0.28f),
                                    primary.copy(alpha = 0.12f),
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CompareArrows,
                        contentDescription = res.str(R.string.conflict_dialog_title),
                        tint = tertiary,
                        modifier = Modifier.size(28.dp),
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = res.str(R.string.conflict_dialog_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = res.str(R.string.conflict_dialog_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val useSideBySide = maxWidth >= 480.dp
                    if (useSideBySide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ConflictVersionCard(
                                res = res,
                                title = res.str(R.string.conflict_local_card_title),
                                metaLine = res.str(
                                    R.string.conflict_local_meta,
                                    conflict.local.storedKeysCount,
                                    conflict.local.snapshotsCount,
                                ),
                                icon = Icons.Rounded.Storage,
                                accent = secondary,
                                snapshot = conflict.local,
                                counterpart = conflict.cloud,
                                isNewer = conflict.isLocalNewer,
                                isRecommended = conflict.recommendedChoice == ConflictKeepChoice.LOCAL,
                                dateFormat = dateFormat,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("conflict_local_version_card"),
                            )
                            ConflictVersionCard(
                                res = res,
                                title = res.str(R.string.conflict_cloud_card_title),
                                metaLine = res.str(
                                    R.string.conflict_cloud_meta,
                                    conflict.cloud.storedKeysCount,
                                    conflict.cloud.subtitle,
                                ),
                                icon = Icons.Rounded.CloudDone,
                                accent = primary,
                                snapshot = conflict.cloud,
                                counterpart = conflict.local,
                                isNewer = conflict.isCloudNewer,
                                isRecommended = conflict.recommendedChoice == ConflictKeepChoice.CLOUD,
                                dateFormat = dateFormat,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("conflict_cloud_version_card"),
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            ConflictVersionCard(
                                res = res,
                                title = res.str(R.string.conflict_local_card_title),
                                metaLine = res.str(
                                    R.string.conflict_local_meta,
                                    conflict.local.storedKeysCount,
                                    conflict.local.snapshotsCount,
                                ),
                                icon = Icons.Rounded.Storage,
                                accent = secondary,
                                snapshot = conflict.local,
                                counterpart = conflict.cloud,
                                isNewer = conflict.isLocalNewer,
                                isRecommended = conflict.recommendedChoice == ConflictKeepChoice.LOCAL,
                                dateFormat = dateFormat,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("conflict_local_version_card"),
                            )
                            ConflictVersionCard(
                                res = res,
                                title = res.str(R.string.conflict_cloud_card_title),
                                metaLine = res.str(
                                    R.string.conflict_cloud_meta,
                                    conflict.cloud.storedKeysCount,
                                    conflict.cloud.subtitle,
                                ),
                                icon = Icons.Rounded.CloudDone,
                                accent = primary,
                                snapshot = conflict.cloud,
                                counterpart = conflict.local,
                                isNewer = conflict.isCloudNewer,
                                isRecommended = conflict.recommendedChoice == ConflictKeepChoice.CLOUD,
                                dateFormat = dateFormat,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("conflict_cloud_version_card"),
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onKeepLocal,
                        enabled = !isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("conflict_keep_local_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onSecondary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = res.str(R.string.conflict_btn_keep_local),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = onKeepCloud,
                        enabled = !isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("conflict_keep_cloud_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = primary.copy(alpha = 0.22f),
                            contentColor = primary,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = res.str(R.string.conflict_btn_keep_cloud),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                TextButton(
                    onClick = onDecideLater,
                    enabled = !isBusy,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("conflict_decide_later_button"),
                ) {
                    Text(
                        text = res.str(R.string.conflict_btn_later),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ConflictVersionCard(
    res: Resources,
    title: String,
    metaLine: String,
    icon: ImageVector,
    accent: Color,
    snapshot: SaveVersionSnapshot,
    counterpart: SaveVersionSnapshot,
    isNewer: Boolean,
    isRecommended: Boolean,
    dateFormat: DateFormat,
    modifier: Modifier = Modifier,
) {
    val timeLabel = if (snapshot.timestampMs > 0L) {
        res.str(R.string.conflict_updated_at, dateFormat.format(Date(snapshot.timestampMs)))
    } else {
        res.str(R.string.conflict_timestamp_unknown)
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            width = if (isRecommended) 1.5.dp else 1.dp,
            color = if (isRecommended) accent.copy(alpha = 0.78f) else accent.copy(alpha = 0.30f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                        contentDescription = title,
                        tint = accent,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (isRecommended || isNewer) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isRecommended) {
                        ConflictPillBadge(
                            text = res.str(R.string.conflict_badge_recommended),
                            accent = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                    if (isNewer) {
                        ConflictPillBadge(
                            text = res.str(R.string.conflict_badge_newer),
                            accent = accent,
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            ConflictMetricRow(
                label = res.str(R.string.conflict_stat_coins),
                value = snapshot.starCoins,
                otherValue = counterpart.starCoins,
                accent = MaterialTheme.colorScheme.tertiary,
            )
            ConflictMetricRow(
                label = res.str(R.string.conflict_stat_matches),
                value = snapshot.gamesPlayed,
                otherValue = counterpart.gamesPlayed,
                accent = accent,
            )
            ConflictMetricRow(
                label = res.str(R.string.conflict_stat_elo),
                value = snapshot.eloPeak,
                otherValue = counterpart.eloPeak,
                accent = accent,
            )
            ConflictMetricRow(
                label = res.str(R.string.conflict_stat_achievements),
                value = snapshot.achievementsCount,
                otherValue = counterpart.achievementsCount,
                accent = accent,
            )

            Text(
                text = metaLine,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ConflictPillBadge(
    text: String,
    accent: Color,
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = accent.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            maxLines = 1,
        )
    }
}

@Composable
private fun ConflictMetricRow(
    label: String,
    value: Int,
    otherValue: Int,
    accent: Color,
) {
    val isHigher = value > otherValue
    val isDifferent = value != otherValue
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = when {
                isHigher -> accent
                isDifferent -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = if (isDifferent) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
