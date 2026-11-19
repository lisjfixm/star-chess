package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import com.example.R

data class CosmicPaletteOption(
    val key: String,
    val nameResId: Int,
    val primaryPreview: Color,
    val secondaryPreview: Color,
    val tertiaryPreview: Color,
    val bgPreview: Color,
    val webHexPrimary: String,
    val webHexSecondary: String,
    val webHexBg: String,
)

object CosmicPalettes {
    val ALL: List<CosmicPaletteOption> = listOf(
        CosmicPaletteOption(
            key = "default",
            nameResId = R.string.theme_starlight_cyan,
            primaryPreview = StarlightCyan,
            secondaryPreview = NebulaMagenta,
            tertiaryPreview = AstralGold,
            bgPreview = CosmosObsidian,
            webHexPrimary = "#4DD0E1",
            webHexSecondary = "#E040FB",
            webHexBg = "#05051A",
        ),
        CosmicPaletteOption(
            key = "nebula_purple",
            nameResId = R.string.theme_nebula_purple,
            primaryPreview = NebulaPrimary,
            secondaryPreview = NebulaSecondary,
            tertiaryPreview = NebulaTertiary,
            bgPreview = NebulaBg,
            webHexPrimary = "#D580FF",
            webHexSecondary = "#7C8CFF",
            webHexBg = "#0B051D",
        ),
        CosmicPaletteOption(
            key = "solar_gold",
            nameResId = R.string.theme_solar_gold,
            primaryPreview = SolarPrimary,
            secondaryPreview = SolarSecondary,
            tertiaryPreview = SolarTertiary,
            bgPreview = SolarBg,
            webHexPrimary = "#FFC107",
            webHexSecondary = "#FF7043",
            webHexBg = "#140B03",
        ),
        CosmicPaletteOption(
            key = "aurora_emerald",
            nameResId = R.string.theme_aurora_emerald,
            primaryPreview = AuroraPrimary,
            secondaryPreview = AuroraSecondary,
            tertiaryPreview = AuroraTertiary,
            bgPreview = AuroraBg,
            webHexPrimary = "#00E6A0",
            webHexSecondary = "#38BDF8",
            webHexBg = "#041517",
        ),
        CosmicPaletteOption(
            key = "supernova_crimson",
            nameResId = R.string.theme_supernova_crimson,
            primaryPreview = SupernovaPrimary,
            secondaryPreview = SupernovaSecondary,
            tertiaryPreview = SupernovaTertiary,
            bgPreview = SupernovaBg,
            webHexPrimary = "#FF5277",
            webHexSecondary = "#FFAB40",
            webHexBg = "#18050B",
        ),
    )

    val SUPPORTED_KEYS: Set<String> = ALL.map { it.key }.toSet()

    fun normalizeKey(raw: String?): String = when (raw) {
        "nebula_purple", "galaxy", "amethyst" -> "nebula_purple"
        "solar_gold", "sunset" -> "solar_gold"
        "aurora_emerald", "aurora" -> "aurora_emerald"
        "supernova_crimson", "lava" -> "supernova_crimson"
        else -> "default"
    }

    fun find(key: String?): CosmicPaletteOption {
        val normalized = normalizeKey(key)
        return ALL.firstOrNull { it.key == normalized } ?: ALL.first()
    }
}

private data class PaletteSpec(
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val onSecondary: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val background: Color,
    val surface: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val mutedText: Color,
)

fun cosmicColorSchemeFor(themeKey: String): ColorScheme {
    val spec = when (CosmicPalettes.normalizeKey(themeKey)) {
        "nebula_purple" -> PaletteSpec(
            primary = NebulaPrimary,
            onPrimary = Color(0xFF1F0633),
            secondary = NebulaSecondary,
            onSecondary = Color(0xFF0A1038),
            tertiary = NebulaTertiary,
            onTertiary = Color(0xFF2D0624),
            background = NebulaBg,
            surface = NebulaDeep,
            surfaceContainer = NebulaSurface,
            surfaceContainerHigh = NebulaSurfaceHigh,
            mutedText = Color(0xFFB5A7D6),
        )
        "solar_gold" -> PaletteSpec(
            primary = SolarPrimary,
            onPrimary = Color(0xFF261700),
            secondary = SolarSecondary,
            onSecondary = Color(0xFF2E0B02),
            tertiary = SolarTertiary,
            onTertiary = Color(0xFF261B00),
            background = SolarBg,
            surface = SolarDeep,
            surfaceContainer = SolarSurface,
            surfaceContainerHigh = SolarSurfaceHigh,
            mutedText = Color(0xFFCBB69A),
        )
        "aurora_emerald" -> PaletteSpec(
            primary = AuroraPrimary,
            onPrimary = Color(0xFF002419),
            secondary = AuroraSecondary,
            onSecondary = Color(0xFF031F30),
            tertiary = AuroraTertiary,
            onTertiary = Color(0xFF05291E),
            background = AuroraBg,
            surface = AuroraDeep,
            surfaceContainer = AuroraSurface,
            surfaceContainerHigh = AuroraSurfaceHigh,
            mutedText = Color(0xFF9AC5C8),
        )
        "supernova_crimson" -> PaletteSpec(
            primary = SupernovaPrimary,
            onPrimary = Color(0xFF2B000B),
            secondary = SupernovaSecondary,
            onSecondary = Color(0xFF2B1500),
            tertiary = SupernovaTertiary,
            onTertiary = Color(0xFF2B1B00),
            background = SupernovaBg,
            surface = SupernovaDeep,
            surfaceContainer = SupernovaSurface,
            surfaceContainerHigh = SupernovaSurfaceHigh,
            mutedText = Color(0xFFCDA3B2),
        )
        else -> PaletteSpec(
            primary = StarlightCyan,
            onPrimary = Color(0xFF04121A),
            secondary = NebulaMagenta,
            onSecondary = Color(0xFF1A0524),
            tertiary = AstralGold,
            onTertiary = Color(0xFF1E1700),
            background = CosmosObsidian,
            surface = CosmosDeep,
            surfaceContainer = CosmosSurface,
            surfaceContainerHigh = CosmosSurfaceHigh,
            mutedText = NebulaMuted,
        )
    }

    return darkColorScheme(
        primary = spec.primary,
        onPrimary = spec.onPrimary,
        primaryContainer = spec.primary.copy(alpha = 0.20f),
        onPrimaryContainer = StarlightInk,
        secondary = spec.secondary,
        onSecondary = spec.onSecondary,
        secondaryContainer = spec.secondary.copy(alpha = 0.20f),
        onSecondaryContainer = StarlightInk,
        tertiary = spec.tertiary,
        onTertiary = spec.onTertiary,
        tertiaryContainer = spec.tertiary.copy(alpha = 0.18f),
        onTertiaryContainer = spec.tertiary,
        background = spec.background,
        onBackground = StarlightInk,
        surface = spec.surface,
        onSurface = StarlightInk,
        surfaceVariant = spec.surfaceContainer,
        onSurfaceVariant = spec.mutedText,
        surfaceContainerLowest = spec.background,
        surfaceContainerLow = spec.surface,
        surfaceContainer = spec.surfaceContainer,
        surfaceContainerHigh = spec.surfaceContainerHigh,
        outline = CosmosOutline,
        outlineVariant = Color(0x1EFFFFFF),
        error = ErrorCoral,
        onError = Color(0xFF280004),
    )
}

@Composable
fun StarChessTheme(
    themeKey: String = "default",
    content: @Composable () -> Unit,
) {
    val scheme = remember(themeKey) { cosmicColorSchemeFor(themeKey) }
    MaterialTheme(
        colorScheme = scheme,
        typography = StarChessTypography,
        content = content,
    )
}
