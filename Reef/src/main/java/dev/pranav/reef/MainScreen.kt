package dev.pranav.reef

import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.core.net.toUri
import dev.pranav.reef.timer.TimerStateManager
import dev.pranav.reef.util.isAccessibilityServiceEnabledForBlocker
import dev.pranav.reef.util.prefs
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MainScreen(
    onNavigateToTimer: () -> Unit,
    onNavigateToUsage: () -> Unit,
    onNavigateToRoutines: () -> Unit,
    onNavigateToWhitelist: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToIntro: () -> Unit,
    onRequestAccessibility: () -> Unit
) {
    val context = LocalContext.current
    val timerState by TimerStateManager.state.collectAsState()
    var showDiscordDialog by remember { mutableStateOf(false) }
    var showDonateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (prefs.getBoolean("first_run", true)) {
            onNavigateToIntro()
        } else {
            if (timerState.isRunning) {
                onNavigateToTimer()
            }

            delay(500)
            if (!prefs.getBoolean("discord_shown", false)) {
                showDiscordDialog = true
            } else if (prefs.getBoolean("show_dialog", false)) {
                showDonateDialog = true
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        AnimatedWaveIcon()
                    }
                },
                actions = {
                    FilledTonalIconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.settings)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            HeroFocusButton(
                onClick = {
                    if (context.isAccessibilityServiceEnabledForBlocker()) {
                        onNavigateToTimer()
                    } else {
                        onRequestAccessibility()
                    }
                }
            )

            Spacer(Modifier.height(48.dp))

            Text(
                text = stringResource(R.string.manage_focus),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 4.dp, bottom = 16.dp)
            )

            NavigationCards(
                onNavigateToUsage = onNavigateToUsage,
                onNavigateToRoutines = onNavigateToRoutines,
                onNavigateToWhitelist = onNavigateToWhitelist
            )

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showDiscordDialog) {
        CommunityDialog(
            onDismiss = {
                prefs.edit { putBoolean("discord_shown", true) }
                showDiscordDialog = false
            },
            onJoin = {
                prefs.edit { putBoolean("discord_shown", true) }
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        "https://discord.gg/46wCMRVAre".toUri()
                    )
                )
                showDiscordDialog = false
            }
        )
    }

    if (showDonateDialog) {
        DonateDialog(
            onDismiss = {
                prefs.edit { putBoolean("show_dialog", false) }
                showDonateDialog = false
            },
            onSupport = {
                prefs.edit { putBoolean("show_dialog", false) }
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        "https://PranavPurwar.github.io/donate.html".toUri()
                    )
                )
                showDonateDialog = false
            }
        )
    }
}

@Composable
private fun AnimatedWaveIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val rotation by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_rotation"
    )

    Icon(
        Icons.Rounded.Waves,
        contentDescription = null,
        modifier = Modifier
            .size(24.dp)
            .rotate(rotation),
        tint = MaterialTheme.colorScheme.primary
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HeroFocusButton(onClick: () -> Unit) {
    //ElevatedCard(
    //    onClick = onClick,
    //    modifier = Modifier.fillMaxWidth(),
    //    shape = RoundedCornerShape(28.dp),
    //    colors = CardDefaults.elevatedCardColors(
    //        containerColor = MaterialTheme.colorScheme.primaryContainer
    //    ),
    //    elevation = CardDefaults.elevatedCardElevation(
    //        defaultElevation = 2.dp,
    //        pressedElevation = 1.dp
    //    )
    //) {
    //    Column(
    //        modifier = Modifier
    //            .fillMaxWidth()
    //            .padding(24.dp),
    //        verticalArrangement = Arrangement.spacedBy(16.dp)
    //    ) {
    //        Row(
    //            modifier = Modifier.fillMaxWidth(),
    //            horizontalArrangement = Arrangement.SpaceBetween,
    //            verticalAlignment = Alignment.CenterVertically
    //        ) {
    //            Text(
    //                text = "Focus Mode",
    //                style = MaterialTheme.typography.headlineMedium,
    //                fontWeight = FontWeight.Bold,
    //                color = MaterialTheme.colorScheme.onPrimaryContainer
    //            )
    //
    //            Surface(
    //                modifier = Modifier.size(56.dp),
    //                shape = CircleShape,
    //                color = MaterialTheme.colorScheme.primary
    //            ) {
    //                Box(
    //                    contentAlignment = Alignment.Center,
    //                    modifier = Modifier.fillMaxSize()
    //                ) {
    //                    Icon(
    //                        Icons.Rounded.PlayArrow,
    //                        contentDescription = "Start",
    //                        modifier = Modifier.size(32.dp),
    //                        tint = MaterialTheme.colorScheme.onPrimary
    //                    )
    //                }
    //            }
    //        }
    //
    //        Text(
    //            text = "Start a focused work session",
    //            style = MaterialTheme.typography.bodyLarge,
    //            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
    //        )
    //    }
    //}

    IconButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .aspectRatio(1.3f),
        shapes = IconButtonDefaults.shapes(pressedShape = IconButtonDefaults.extraLargePressedShape),
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Rounded.Waves,
                contentDescription = stringResource(R.string.focus_mode),
                modifier = Modifier.fillMaxSize(0.6f),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = stringResource(R.string.focus_mode),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NavigationCards(
    onNavigateToUsage: () -> Unit,
    onNavigateToRoutines: () -> Unit,
    onNavigateToWhitelist: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        NavigationCard(
            icon = Icons.Rounded.BarChart,
            title = stringResource(R.string.app_usage),
            description = stringResource(R.string.app_usage_desc),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            accentColor = MaterialTheme.colorScheme.primary,
            onClick = onNavigateToUsage
        )

        NavigationCard(
            icon = Icons.Rounded.Schedule,
            title = stringResource(R.string.routines),
            description = stringResource(R.string.routines_desc),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            accentColor = MaterialTheme.colorScheme.secondary,
            onClick = onNavigateToRoutines
        )

        NavigationCard(
            icon = Icons.Rounded.Verified,
            title = stringResource(R.string.whitelist_apps),
            description = stringResource(R.string.whitelist_apps_desc),
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            accentColor = MaterialTheme.colorScheme.tertiary,
            onClick = onNavigateToWhitelist
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun NavigationCard(
    icon: ImageVector,
    title: String,
    description: String,
    containerColor: Color,
    contentColor: Color,
    accentColor: Color,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 1.dp
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.15f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = accentColor
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }

            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun CommunityDialog(
    onDismiss: () -> Unit,
    onJoin: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Rounded.Groups,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.join_community),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = stringResource(R.string.join_community_desc),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onJoin) {
                Icon(
                    Icons.Rounded.Forum,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.join_discord))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.maybe_later))
            }
        }
    )
}

@Composable
private fun DonateDialog(
    onDismiss: () -> Unit,
    onSupport: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Rounded.Favorite,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = stringResource(R.string.support_development),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.support_development_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.any_amount_helps),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        confirmButton = {
            Button(onClick = onSupport) {
                Icon(
                    Icons.Rounded.VolunteerActivism,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.support_development))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.maybe_later))
            }
        }
    )
}
