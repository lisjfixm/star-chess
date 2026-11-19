package com.example.ui

import android.content.res.Resources
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.CosmicPaletteOption
import com.example.ui.theme.CosmicPalettes

@Composable
fun CosmicThemeDropdownButton(
    res: Resources,
    activeThemeKey: String,
    onSelectTheme: (String) -> Unit,
    buttonTestTag: String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val active = remember(activeThemeKey) { CosmicPalettes.find(activeThemeKey) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag(buttonTestTag),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Palette,
                    contentDescription = res.str(R.string.action_theme_switch),
                    tint = active.primaryPreview,
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            CosmicPalettes.ALL.forEach { palette ->
                val isSelected = palette.key == active.key
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            PaletteSwatchOrb(palette = palette, sizeDp = 18)
                            Text(
                                text = res.str(palette.nameResId),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) palette.primaryPreview else MaterialTheme.colorScheme.onSurface,
                            )
                            if (isSelected) {
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = palette.primaryPreview,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelectTheme(palette.key)
                    },
                    modifier = Modifier.testTag("theme_dropdown_option_${palette.key}"),
                )
            }
        }
    }
}

@Composable
fun CosmicThemeSwitcherCard(
    res: Resources,
    activeThemeKey: String,
    onSelectTheme: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = remember(activeThemeKey) { CosmicPalettes.find(activeThemeKey) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("theme_switcher_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)),
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
                    imageVector = Icons.Rounded.Palette,
                    contentDescription = res.str(R.string.theme_switcher_title),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column {
                    Text(
                        text = res.str(R.string.theme_switcher_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = res.str(R.string.theme_switcher_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CosmicPalettes.ALL.forEach { palette ->
                    val isSelected = palette.key == active.key
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) palette.primaryPreview.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = BorderStroke(
                            width = if (isSelected) 1.6.dp else 1.dp,
                            color = if (isSelected) palette.primaryPreview else MaterialTheme.colorScheme.outlineVariant,
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(role = Role.RadioButton) { onSelectTheme(palette.key) }
                            .testTag("theme_palette_chip_${palette.key}"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            PaletteSwatchOrb(palette = palette, sizeDp = 22)
                            Text(
                                text = res.str(palette.nameResId),
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            )
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = palette.primaryPreview,
                                    modifier = Modifier.size(16.dp),
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
private fun PaletteSwatchOrb(
    palette: CosmicPaletteOption,
    sizeDp: Int,
) {
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        palette.primaryPreview,
                        palette.secondaryPreview,
                        palette.tertiaryPreview,
                    )
                )
            )
    )
}
