package com.example.ui

import android.content.res.Resources
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.cloud.CloudSyncRepository
import com.example.data.cloud.authStateFlow
import com.example.data.local.LocalArchiveRepository
import com.example.data.local.StarChessDatabase
import com.example.ui.theme.CosmicPalettes
import com.example.ui.theme.StarChessTheme
import com.example.viewmodel.AppDestination
import com.example.viewmodel.CloudActionEvent
import com.example.viewmodel.CloudSyncViewModel
import com.example.viewmodel.LocalEvent
import com.example.viewmodel.StarChessViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private data class NavTabSpec(
    val destination: AppDestination,
    val labelResId: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

private data class LanguageOption(
    val code: String,
    val shortBadge: String,
    val label: String,
)

private val LANGUAGE_OPTIONS = listOf(
    LanguageOption("en", "EN", "English"),
    LanguageOption("zh-Hant", "繁", "繁體中文"),
    LanguageOption("zh-Hans", "简", "简体中文"),
    LanguageOption("ja", "日", "日本語"),
)

@Composable
fun StarChessRoot() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsStore = remember(context) { com.example.data.local.PreferencesStore(context) }
    val localRepo = remember(context) {
        LocalArchiveRepository(StarChessDatabase.getInstance(context).starChessDao(), prefsStore)
    }
    val shellPrefs by prefsStore.preferencesFlow.collectAsStateWithLifecycle(
        initialValue = com.example.data.local.ShellPreferences(
            preferredLanguage = com.example.data.local.PreferencesStore.defaultLanguageForLocale(),
            soundEnabled = true,
            bgmEnabled = true,
            lastCloudSyncMs = 0L,
            visitorMode = false,
        )
    )
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val credentialManager = remember(context) { androidx.credentials.CredentialManager.create(context) }
    var isCheckingAutoSignIn by remember { mutableStateOf(auth.currentUser == null) }

    LaunchedEffect(Unit) {
        if (auth.currentUser == null) {
            com.example.data.cloud.attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { isCheckingAutoSignIn = false },
                onUnauthenticated = { isCheckingAutoSignIn = false },
                scope = this,
            )
        } else {
            isCheckingAutoSignIn = false
        }
    }

    LaunchedEffect(currentUser?.uid) {
        if (currentUser != null && shellPrefs.visitorMode) {
            prefsStore.setVisitorMode(false)
        }
    }

    val user = currentUser
    when {
        user != null -> {
            androidx.compose.runtime.key(user.uid) {
                StarChessApp(
                    isVisitorMode = false,
                    onExitVisitorMode = {},
                    viewModel = viewModel(
                        key = user.uid,
                        factory = StarChessViewModel.Factory,
                    ),
                )
            }
        }
        shellPrefs.visitorMode -> {
            androidx.compose.runtime.key("visitor_local") {
                StarChessApp(
                    isVisitorMode = true,
                    onExitVisitorMode = {
                        scope.launch { prefsStore.setVisitorMode(false) }
                    },
                    viewModel = viewModel(
                        key = "visitor_local",
                        factory = StarChessViewModel.Factory,
                    ),
                )
            }
        }
        else -> {
            StarChessTheme(themeKey = shellPrefs.cosmicTheme) {
                AuthScreen(
                    preferredLanguage = shellPrefs.preferredLanguage,
                    activeThemeKey = shellPrefs.cosmicTheme,
                    isCheckingAutoSignIn = isCheckingAutoSignIn,
                    onSelectLanguage = { code ->
                        scope.launch(Dispatchers.IO) { localRepo.setLanguage(code) }
                    },
                    onSelectTheme = { key ->
                        scope.launch(Dispatchers.IO) { localRepo.setCosmicTheme(key) }
                    },
                    onAuthSuccess = {
                        isCheckingAutoSignIn = false
                        scope.launch { prefsStore.setVisitorMode(false) }
                    },
                    onContinueAsVisitor = {
                        scope.launch { prefsStore.setVisitorMode(true) }
                    },
                )
            }
        }
    }
}

@Composable
fun StarChessApp(
    isVisitorMode: Boolean = false,
    onExitVisitorMode: () -> Unit = {},
    viewModel: StarChessViewModel = viewModel(factory = StarChessViewModel.Factory),
) {
    val entriesMap by viewModel.entriesMap.collectAsStateWithLifecycle()
    val localSaveUpdatedAtMs by viewModel.localSaveUpdatedAtMs.collectAsStateWithLifecycle()
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val snapshots by viewModel.snapshots.collectAsStateWithLifecycle()
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val hasEngineSubView by viewModel.hasEngineSubView.collectAsStateWithLifecycle()
    val pendingDailyBonus by viewModel.pendingDailyBonus.collectAsStateWithLifecycle()
    val dailyRewardRecord by viewModel.dailyRewardRecord.collectAsStateWithLifecycle()

    val context = androidx.compose.ui.platform.LocalContext.current
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    val cloudViewModel: CloudSyncViewModel? = currentUser?.let { signedUser ->
        viewModel(
            key = "cloud_sync_${signedUser.uid}",
            factory = viewModelFactory {
                initializer {
                    val databaseId = context.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(databaseId)
                    CloudSyncViewModel(CloudSyncRepository(db), signedUser.uid)
                }
            },
        )
    }

    val cloudSavesState = cloudViewModel?.cloudSavesState?.collectAsStateWithLifecycle()?.value
    val activeConflict = cloudViewModel?.activeConflict?.collectAsStateWithLifecycle()?.value
    val isCloudBusy = cloudViewModel?.isBusy?.collectAsStateWithLifecycle()?.value ?: false

    val res = rememberLocalizedResources(prefs.preferredLanguage)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var starParticleBurstCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        viewModel.checkDailyLoginBonus()
    }

    LaunchedEffect(prefs.cosmicTheme) {
        val palette = CosmicPalettes.find(prefs.cosmicTheme)
        viewModel.webController.applyCosmicPalette(
            primaryHex = palette.webHexPrimary,
            secondaryHex = palette.webHexSecondary,
            bgHex = palette.webHexBg,
        )
    }

    LaunchedEffect(
        cloudViewModel,
        cloudSavesState,
        entriesMap,
        localSaveUpdatedAtMs,
        snapshots.size,
        prefs.lastCloudSyncMs,
    ) {
        cloudViewModel?.evaluateAutoConflict(
            localEntries = entriesMap,
            localUpdatedAtMs = localSaveUpdatedAtMs,
            snapshotsCount = snapshots.size,
            lastCloudSyncMs = prefs.lastCloudSyncMs,
        )
    }

    LaunchedEffect(cloudViewModel) {
        cloudViewModel?.events?.collect { event ->
            val msg = when (event) {
                CloudActionEvent.BackupSuccess -> res.str(R.string.msg_cloud_backup_done)
                CloudActionEvent.ConflictResolvedKeptLocal -> res.str(R.string.conflict_resolved_kept_local)
                is CloudActionEvent.Failure -> event.message
            }
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val msg = when (event) {
                LocalEvent.SnapshotSaved -> res.str(R.string.msg_snapshot_saved)
                LocalEvent.SnapshotRestored -> res.str(R.string.msg_snapshot_restored)
                LocalEvent.CloudRestored -> res.str(R.string.msg_cloud_restored)
                LocalEvent.ConflictKeptCloud -> res.str(R.string.conflict_resolved_kept_cloud)
                is LocalEvent.DailyBonusClaimed -> {
                    starParticleBurstCount++
                    res.str(R.string.daily_bonus_claimed_msg, event.coins, event.streakDay)
                }
            }
            snackbarHostState.showSnackbar(msg)
        }
    }

    BackHandler(enabled = currentTab != AppDestination.ARENA || hasEngineSubView) {
        viewModel.handleBackPress()
    }

    val navTabs = remember {
        listOf(
            NavTabSpec(
                destination = AppDestination.ARENA,
                labelResId = R.string.nav_arena,
                selectedIcon = Icons.Rounded.GridView,
                unselectedIcon = Icons.Outlined.GridView,
                testTag = "nav_tab_arena",
            ),
            NavTabSpec(
                destination = AppDestination.NEXUS,
                labelResId = R.string.nav_nexus,
                selectedIcon = Icons.Rounded.AutoAwesome,
                unselectedIcon = Icons.Outlined.AutoAwesome,
                testTag = "nav_tab_nexus",
            ),
            NavTabSpec(
                destination = AppDestination.TRIALS,
                labelResId = R.string.nav_trials,
                selectedIcon = Icons.Rounded.EmojiEvents,
                unselectedIcon = Icons.Outlined.EmojiEvents,
                testTag = "nav_tab_trials",
            ),
            NavTabSpec(
                destination = AppDestination.PROFILE,
                labelResId = R.string.nav_profile,
                selectedIcon = if (currentUser != null) Icons.Rounded.CloudDone else Icons.Rounded.CloudSync,
                unselectedIcon = Icons.Outlined.CloudSync,
                testTag = "nav_tab_profile",
            ),
        )
    }

    StarChessTheme(themeKey = prefs.cosmicTheme) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useNavigationRail = maxWidth >= 600.dp

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    StarChessTopBar(
                        res = res,
                        appTitle = res.str(R.string.app_name),
                        visitorBadge = if (isVisitorMode && currentUser == null) res.str(R.string.badge_visitor) else null,
                        starCoins = summary.starCoins,
                        preferredLanguage = prefs.preferredLanguage,
                        activeThemeKey = prefs.cosmicTheme,
                        soundEnabled = prefs.soundEnabled,
                        bgmEnabled = prefs.bgmEnabled,
                        isSignedIn = currentUser != null,
                        starParticlesDescription = res.str(R.string.action_star_particles),
                        soundDescription = res.str(R.string.action_sound_toggle),
                        bgmDescription = res.str(R.string.action_bgm_toggle),
                        langDescription = res.str(R.string.action_language_switch),
                        onTapStarIcon = { starParticleBurstCount++ },
                        onSelectLanguage = viewModel::switchLanguage,
                        onSelectTheme = { key ->
                            viewModel.selectCosmicTheme(key)
                            starParticleBurstCount++
                        },
                        onToggleSound = viewModel::toggleSound,
                        onToggleBgm = viewModel::toggleBgm,
                        onTapCloudBadge = { viewModel.selectDestination(AppDestination.PROFILE) },
                    )
                },
                bottomBar = {
                    if (!useNavigationRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                            tonalElevation = 4.dp,
                        ) {
                            navTabs.forEach { tab ->
                                val selected = currentTab == tab.destination
                                val label = res.str(tab.labelResId)
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.selectDestination(tab.destination) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = label,
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                    modifier = Modifier.testTag(tab.testTag),
                                )
                            }
                        }
                    }
                },
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    if (useNavigationRail) {
                        NavigationRail(
                            modifier = Modifier.fillMaxHeight(),
                            containerColor = MaterialTheme.colorScheme.surface,
                        ) {
                            Spacer(Modifier.height(12.dp))
                            navTabs.forEach { tab ->
                                val selected = currentTab == tab.destination
                                val label = res.str(tab.labelResId)
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { viewModel.selectDestination(tab.destination) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = label,
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                    modifier = Modifier.testTag(tab.testTag),
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        AndroidView(
                            factory = { ctx ->
                                viewModel.webController.createOrReuseWebView(ctx)
                            },
                            update = { webView ->
                                webView.visibility =
                                    if (currentTab == AppDestination.PROFILE) View.INVISIBLE else View.VISIBLE
                            },
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("starchess_arena_webview"),
                        )

                        androidx.compose.animation.AnimatedVisibility(
                            visible = currentTab == AppDestination.PROFILE,
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            ProfileCloudScreen(
                                res = res,
                                summary = summary,
                                snapshots = snapshots,
                                dailyRewardRecord = dailyRewardRecord,
                                preferredLanguage = prefs.preferredLanguage,
                                activeThemeKey = prefs.cosmicTheme,
                                localEntries = entriesMap,
                                localSaveUpdatedAtMs = localSaveUpdatedAtMs,
                                lastCloudSyncMs = prefs.lastCloudSyncMs,
                                cloudViewModel = cloudViewModel,
                                onSelectTheme = { key ->
                                    viewModel.selectCosmicTheme(key)
                                    starParticleBurstCount++
                                },
                                onSaveLocalCheckpoint = viewModel::saveLocalCheckpoint,
                                onRestoreLocalCheckpoint = viewModel::restoreLocalCheckpoint,
                                onDeleteLocalCheckpoint = viewModel::deleteLocalCheckpoint,
                                onCollectEntriesForCloud = viewModel::snapshotEntriesForCloud,
                                onApplyCloudRestore = viewModel::applyCloudRestoredEntries,
                                onMarkCloudSynced = viewModel::markCloudSynced,
                                onExitVisitorMode = onExitVisitorMode,
                                onShowMessage = { message ->
                                    scope.launch { snackbarHostState.showSnackbar(message) }
                                },
                            )
                        }

                        // Subtle cosmic particle atmosphere triggered when the star icon is clicked
                        CosmicParticleOverlay(
                            burstTrigger = starParticleBurstCount,
                            originNormX = 0.08f,
                            originNormY = 0.02f,
                            primary = MaterialTheme.colorScheme.primary,
                            secondary = MaterialTheme.colorScheme.secondary,
                            gold = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("main_menu_cosmic_particles"),
                        )
                    }
                }
            }

            if (activeConflict != null && cloudViewModel != null) {
                SaveConflictResolutionDialog(
                    res = res,
                    conflict = activeConflict,
                    isBusy = isCloudBusy,
                    onKeepLocal = {
                        cloudViewModel.resolveConflictKeepingLocal(
                            conflict = activeConflict,
                            primaryLabel = res.str(R.string.label_primary_sync),
                            preferredLanguage = prefs.preferredLanguage,
                            onLocalKeptSuccess = viewModel::markCloudSynced,
                        )
                    },
                    onKeepCloud = {
                        viewModel.resolveConflictKeepingCloud(
                            entries = activeConflict.cloudBundle.entries,
                            safetyBackupPrefix = res.str(R.string.conflict_local_safety_backup_prefix),
                        )
                        cloudViewModel.markConflictResolved(activeConflict.conflictFingerprint)
                    },
                    onDecideLater = cloudViewModel::dismissConflictForLater,
                )
            } else {
                pendingDailyBonus?.let { offer ->
                    DailyBonusDialog(
                        res = res,
                        offer = offer,
                        onClaim = viewModel::claimDailyBonus,
                        onDismiss = viewModel::dismissDailyBonus,
                    )
                }
            }
        }
    }
}

@Composable
private fun StarChessTopBar(
    res: Resources,
    appTitle: String,
    visitorBadge: String?,
    starCoins: Int,
    preferredLanguage: String,
    activeThemeKey: String,
    soundEnabled: Boolean,
    bgmEnabled: Boolean,
    isSignedIn: Boolean,
    starParticlesDescription: String,
    soundDescription: String,
    bgmDescription: String,
    langDescription: String,
    onTapStarIcon: () -> Unit,
    onSelectLanguage: (String) -> Unit,
    onSelectTheme: (String) -> Unit,
    onToggleSound: () -> Unit,
    onToggleBgm: () -> Unit,
    onTapCloudBadge: () -> Unit,
) {
    var langMenuExpanded by remember { mutableStateOf(false) }
    val activeLang = remember(preferredLanguage) {
        LANGUAGE_OPTIONS.firstOrNull { it.code == preferredLanguage } ?: LANGUAGE_OPTIONS[1]
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceContainer,
                    )
                )
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .size(38.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onTapStarIcon)
                    .semantics { contentDescription = starParticlesDescription }
                    .testTag("topbar_spinning_star"),
                contentAlignment = Alignment.Center,
            ) {
                OrganicTopBarStarIcon(
                    primary = MaterialTheme.colorScheme.primary,
                    secondary = MaterialTheme.colorScheme.secondary,
                    gold = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(30.dp),
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = appTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (visitorBadge != null) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable(onClick = onTapCloudBadge)
                            .testTag("topbar_visitor_badge"),
                    ) {
                        Text(
                            text = visitorBadge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            maxLines = 1,
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable(onClick = onTapCloudBadge)
                    .testTag("top_star_coins_chip"),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Stars,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = starCoins.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.Bold,
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSignedIn) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            )
                    )
                }
            }

            CosmicThemeDropdownButton(
                res = res,
                activeThemeKey = activeThemeKey,
                onSelectTheme = onSelectTheme,
                buttonTestTag = "top_theme_button",
            )

            Box {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(999.dp))
                        .clickable { langMenuExpanded = true }
                        .testTag("top_language_button"),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Language,
                            contentDescription = langDescription,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = activeLang.shortBadge,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                DropdownMenu(
                    expanded = langMenuExpanded,
                    onDismissRequest = { langMenuExpanded = false },
                ) {
                    LANGUAGE_OPTIONS.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "${option.shortBadge} · ${option.label}",
                                    fontWeight = if (option.code == preferredLanguage) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            onClick = {
                                langMenuExpanded = false
                                onSelectLanguage(option.code)
                            },
                            modifier = Modifier.testTag("lang_option_${option.code}"),
                        )
                    }
                }
            }

            IconButton(
                onClick = onToggleBgm,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("top_bgm_toggle"),
            ) {
                Icon(
                    imageVector = if (bgmEnabled) Icons.Rounded.MusicNote else Icons.Rounded.MusicOff,
                    contentDescription = bgmDescription,
                    tint = if (bgmEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            IconButton(
                onClick = onToggleSound,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("top_sound_toggle"),
            ) {
                Icon(
                    imageVector = if (soundEnabled) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                    contentDescription = soundDescription,
                    tint = if (soundEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}
