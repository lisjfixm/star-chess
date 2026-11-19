package com.example.ui

import android.content.res.Resources
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.local.DailyBonusOffer
import com.example.data.local.LocalArchiveRepository

@Composable
fun DailyBonusDialog(
    res: Resources,
    offer: DailyBonusOffer,
    onClaim: () -> Unit,
    onDismiss: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 380.dp)
                .testTag("daily_bonus_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 8.dp,
            border = BorderStroke(
                width = 1.4.dp,
                brush = Brush.linearGradient(
                    listOf(
                        tertiary.copy(alpha = 0.72f),
                        primary.copy(alpha = 0.58f),
                        secondary.copy(alpha = 0.45f),
                    )
                ),
            ),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    tertiary.copy(alpha = 0.28f),
                                    primary.copy(alpha = 0.10f),
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CardGiftcard,
                        contentDescription = res.str(R.string.daily_bonus_title),
                        tint = tertiary,
                        modifier = Modifier.size(30.dp),
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, primary.copy(alpha = 0.4f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = primary,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = res.str(R.string.daily_bonus_streak_badge, offer.streakDay),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = res.str(R.string.daily_bonus_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = res.str(R.string.daily_bonus_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, tertiary.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Stars,
                            contentDescription = null,
                            tint = tertiary,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = res.str(R.string.daily_bonus_reward_amount, offer.rewardCoins),
                            style = MaterialTheme.typography.titleLarge,
                            color = tertiary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                // Compact 7-day cycle strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val activeIdx = offer.cycleDayIndex
                    LocalArchiveRepository.DAILY_REWARD_SCHEDULE.forEachIndexed { idx, coins ->
                        val dayNum = idx + 1
                        val isPast = dayNum < activeIdx
                        val isToday = dayNum == activeIdx
                        val bg = when {
                            isToday -> tertiary.copy(alpha = 0.22f)
                            isPast -> primary.copy(alpha = 0.16f)
                            else -> MaterialTheme.colorScheme.surfaceContainerHigh
                        }
                        val borderColor = when {
                            isToday -> tertiary
                            isPast -> primary.copy(alpha = 0.45f)
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            color = bg,
                            border = BorderStroke(1.dp, borderColor),
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(
                                    text = res.str(R.string.daily_bonus_day_label, dayNum),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isToday) tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                                if (isPast) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = primary,
                                        modifier = Modifier.size(14.dp),
                                    )
                                } else {
                                    Text(
                                        text = "+$coins",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isToday) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))

                Button(
                    onClick = onClaim,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("daily_bonus_claim_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Stars,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = res.str(R.string.daily_bonus_btn_claim),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("daily_bonus_dismiss_button"),
                ) {
                    Text(
                        text = res.str(R.string.daily_bonus_btn_later),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
