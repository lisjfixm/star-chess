package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.cloud.onGoogleSignInClicked
import kotlin.math.sin

private val AUTH_LANG_OPTIONS = listOf(
    "en" to "EN",
    "zh-Hant" to "繁",
    "zh-Hans" to "简",
    "ja" to "日",
)

@Composable
fun AuthScreen(
    preferredLanguage: String,
    activeThemeKey: String,
    isCheckingAutoSignIn: Boolean,
    onSelectLanguage: (String) -> Unit,
    onSelectTheme: (String) -> Unit,
    onAuthSuccess: () -> Unit,
    onContinueAsVisitor: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val res = rememberLocalizedResources(preferredLanguage)

    var isSigningIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var starBurstCount by remember { mutableIntStateOf(0) }

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val background = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surface

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(background, surface, background)
                )
            )
    ) {
        CosmicStarfieldBackdrop(
            primary = primary,
            secondary = secondary,
            gold = tertiary,
            modifier = Modifier.fillMaxSize(),
        )
        CosmicParticleOverlay(
            burstTrigger = starBurstCount,
            originNormX = 0.5f,
            originNormY = 0.24f,
            primary = primary,
            secondary = secondary,
            gold = tertiary,
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    CosmicThemeDropdownButton(
                        res = res,
                        activeThemeKey = activeThemeKey,
                        onSelectTheme = { key ->
                            onSelectTheme(key)
                            starBurstCount++
                        },
                        buttonTestTag = "auth_theme_button",
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        AUTH_LANG_OPTIONS.forEach { (code, badge) ->
                            val selected = code == preferredLanguage
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent
                                    )
                                    .clickable { onSelectLanguage(code) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("auth_lang_$code"),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = badge,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f, fill = true))
            Spacer(Modifier.height(16.dp))

            val starActionDesc = res.str(R.string.action_star_particles)
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .clickable(role = Role.Button) { starBurstCount++ }
                    .semantics { contentDescription = starActionDesc }
                    .testTag("auth_spinning_star_emblem"),
                contentAlignment = Alignment.Center,
            ) {
                OrganicAuthStarEmblem(
                    primary = primary,
                    secondary = secondary,
                    gold = tertiary,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Spacer(Modifier.height(18.dp))

            Text(
                text = res.str(R.string.auth_welcome_title),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = res.str(R.string.auth_welcome_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 420.dp),
            )

            Spacer(Modifier.height(26.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f)
                ),
                border = BorderStroke(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            primary.copy(alpha = 0.55f),
                            secondary.copy(alpha = 0.45f),
                            tertiary.copy(alpha = 0.35f),
                        )
                    ),
                ),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AuthHighlightRow(
                        icon = Icons.Rounded.GridView,
                        tint = primary,
                        text = res.str(R.string.auth_feature_games),
                    )
                    AuthHighlightRow(
                        icon = Icons.Rounded.CloudDone,
                        tint = secondary,
                        text = res.str(R.string.auth_feature_sync),
                    )
                    AuthHighlightRow(
                        icon = Icons.Rounded.OfflinePin,
                        tint = tertiary,
                        text = res.str(R.string.auth_feature_offline),
                    )

                    Spacer(Modifier.height(4.dp))

                    if (isCheckingAutoSignIn) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = primary,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = res.str(R.string.auth_checking_session),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    errorMessage?.let { msg ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            )
                        }
                    }

                    Button(
                        onClick = {
                            errorMessage = null
                            isSigningIn = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isSigningIn = false
                                    errorMessage = null
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isSigningIn = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = {
                                    isSigningIn = false
                                },
                            )
                        },
                        enabled = !isSigningIn && !isCheckingAutoSignIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("auth_google_sign_in_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.5.dp,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Login,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = res.str(R.string.btn_sign_in_google),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onContinueAsVisitor,
                        enabled = !isSigningIn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .minimumInteractiveComponentSize()
                            .testTag("auth_visitor_button"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PersonOutline,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = res.str(R.string.btn_continue_visitor),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    Text(
                        text = res.str(R.string.auth_visitor_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )

                    Text(
                        text = res.str(R.string.auth_privacy_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Spacer(Modifier.weight(1f, fill = true))
        }
    }
}

@Composable
private fun AuthHighlightRow(
    icon: ImageVector,
    tint: Color,
    text: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun CosmicStarfieldBackdrop(
    primary: Color,
    secondary: Color,
    gold: Color,
    modifier: Modifier = Modifier,
) {
    val motion = rememberOrganicStarMotion(seedOffset = 1.3f, speedMultiplier = 0.6f)
    val t = motion.elapsedSec

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val driftX = (sin(t * 0.27f) * 24f)
        val driftY = (sin(t * 0.19f + 1.1f) * 18f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondary.copy(alpha = 0.20f), Color.Transparent),
                center = Offset(w * 0.16f + driftX, h * 0.18f + driftY),
                radius = w * 0.58f,
            ),
            radius = w * 0.58f,
            center = Offset(w * 0.16f + driftX, h * 0.18f + driftY),
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(w * 0.86f - driftX, h * 0.82f - driftY),
                radius = w * 0.54f,
            ),
            radius = w * 0.54f,
            center = Offset(w * 0.86f - driftX, h * 0.82f - driftY),
        )

        val starPositions = listOf(
            Triple(0.12f, 0.14f, 2.5f),
            Triple(0.28f, 0.08f, 1.8f),
            Triple(0.74f, 0.12f, 2.2f),
            Triple(0.88f, 0.24f, 2.8f),
            Triple(0.08f, 0.42f, 1.6f),
            Triple(0.92f, 0.48f, 2.0f),
            Triple(0.18f, 0.78f, 2.4f),
            Triple(0.82f, 0.74f, 1.9f),
            Triple(0.45f, 0.92f, 2.2f),
            Triple(0.64f, 0.88f, 1.5f),
            Triple(0.36f, 0.22f, 1.4f),
            Triple(0.62f, 0.30f, 1.7f),
        )
        starPositions.forEachIndexed { idx, (rx, ry, rad) ->
            val twinkle = (0.30f + 0.55f * (0.5f + 0.5f * sin(t * (1.1f + idx * 0.31f) + idx * 1.7f)))
                .coerceIn(0.15f, 0.92f)
            val color = when (idx % 3) {
                0 -> primary.copy(alpha = twinkle * 0.8f)
                1 -> gold.copy(alpha = twinkle * 0.75f)
                else -> Color.White.copy(alpha = twinkle * 0.7f)
            }
            drawCircle(
                color = color,
                radius = (rad * (0.85f + 0.25f * twinkle)).dp.toPx(),
                center = Offset(w * rx, h * ry),
            )
        }
    }
}
