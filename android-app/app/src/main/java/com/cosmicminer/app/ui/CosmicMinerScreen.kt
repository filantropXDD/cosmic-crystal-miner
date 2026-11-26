package com.cosmicminer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmicminer.app.model.BottomTab
import com.cosmicminer.app.model.OfflineReward
import com.cosmicminer.app.ui.components.AchievementsList
import com.cosmicminer.app.ui.components.CrystalArena
import com.cosmicminer.app.ui.components.PrestigeCollapsePanel
import com.cosmicminer.app.ui.components.SettingsDialog
import com.cosmicminer.app.ui.components.UpgradesList
import com.cosmicminer.app.ui.theme.CosmicColors
import com.cosmicminer.app.util.AppUpdateInfo
import com.cosmicminer.app.util.NumberFormatter

@Composable
fun CosmicMinerScreen(viewModel: GameViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }

    state.availableUpdate?.let { update ->
        AppUpdateDialog(
            update = update,
            onDismiss = viewModel::dismissUpdateDialog
        )
    }

    state.offlineReward?.let { reward ->
        OfflineRewardDialog(
            reward = reward,
            onDismiss = viewModel::dismissOfflineReward
        )
    }

    if (showSettings) {
        SettingsDialog(
            isVibrationEnabled = state.isVibrationEnabled,
            isSoundEnabled = state.isSoundEnabled,
            totalClicks = state.totalClicks,
            criticalHits = state.criticalHits,
            cometsCaught = state.cometsCaught,
            darkMatter = state.darkMatter,
            isCheckingUpdate = state.isCheckingUpdate,
            updateStatusMessage = state.updateStatusMessage,
            onVibrationChange = viewModel::setVibrationEnabled,
            onSoundChange = viewModel::setSoundEnabled,
            onCheckUpdates = viewModel::checkForUpdatesManual,
            onExportBackup = viewModel::exportBackupCode,
            onImportBackup = viewModel::importBackupCode,
            onResetConfirmed = viewModel::resetProgress,
            onDismiss = {
                viewModel.clearUpdateStatus()
                showSettings = false
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            CosmicColors.DeepSpaceTop,
                            CosmicColors.DeepSpaceBottom,
                            Color(0xFF090C1C)
                        )
                    )
                )
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopCurrencyHeader(
                    crystals = state.crystals,
                    passivePerSec = state.passivePerSecond,
                    clickPower = state.clickPower,
                    darkMatter = state.darkMatter,
                    prestigeMultiplier = state.prestigeMultiplier,
                    onOpenSettings = { showSettings = true }
                )

                RankProgressSection(
                    rankTitle = state.currentRank.title,
                    rankLevel = state.currentRank.level,
                    multiplier = state.currentRank.multiplier,
                    progress = state.rankProgress,
                    experience = state.experience,
                    nextThreshold = state.nextRank?.xpThreshold
                )

                CrystalArena(
                    rankLevel = state.currentRank.level,
                    overchargeSecondsLeft = state.overchargeSecondsLeft,
                    isCometVisible = state.isCometVisible,
                    cometSeed = state.cometSeed,
                    isVibrationEnabled = state.isVibrationEnabled,
                    onCrystalTap = viewModel::onCrystalClick,
                    onCometTap = viewModel::catchComet,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.42f)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.58f)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(Color(0xFF0B0F23))
                ) {
                    val tabs = BottomTab.entries
                    TabRow(
                        selectedTabIndex = tabs.indexOf(state.selectedTab),
                        containerColor = Color(0xFF0E142D),
                        contentColor = CosmicColors.NeonCyan,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(
                                    tabPositions[tabs.indexOf(state.selectedTab)]
                                ),
                                color = CosmicColors.NeonCyan
                            )
                        }
                    ) {
                        tabs.forEach { tab ->
                            val selected = state.selectedTab == tab
                            val badgeCount = when (tab) {
                                BottomTab.ACHIEVEMENTS -> state.unclaimedAchievementsCount
                                BottomTab.PRESTIGE -> if (state.potentialDarkMatter > 0L) 1 else 0
                                else -> 0
                            }
                            Tab(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                text = {
                                    if (badgeCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = if (tab == BottomTab.PRESTIGE) CosmicColors.NeonPurpleBright else CosmicColors.NeonGold,
                                                    contentColor = Color.Black
                                                ) {
                                                    Text(
                                                        text = if (tab == BottomTab.PRESTIGE) "+${state.potentialDarkMatter}"
                                                        else badgeCount.toString(),
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                        ) {
                                            Text(
                                                text = tab.title,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                softWrap = false,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (selected) CosmicColors.TextPrimary else CosmicColors.TextSecondary
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = tab.title,
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selected) CosmicColors.TextPrimary else CosmicColors.TextSecondary
                                        )
                                    }
                                }
                            )
                        }
                    }

                    when (state.selectedTab) {
                        BottomTab.UPGRADES -> UpgradesList(
                            upgrades = state.upgrades,
                            onBuyUpgrade = viewModel::buyUpgrade
                        )
                        BottomTab.ACHIEVEMENTS -> AchievementsList(
                            achievements = state.achievements,
                            onClaimAchievement = viewModel::claimAchievement
                        )
                        BottomTab.PRESTIGE -> PrestigeCollapsePanel(
                            darkMatter = state.darkMatter,
                            prestigeCount = state.prestigeCount,
                            prestigeMultiplier = state.prestigeMultiplier,
                            potentialDarkMatter = state.potentialDarkMatter,
                            runCrystalsEarned = state.runCrystalsEarned,
                            nextDarkMatterThreshold = state.nextDarkMatterThreshold,
                            onCollapseConfirmed = viewModel::performCollapse
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppUpdateDialog(
    update: AppUpdateInfo,
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CosmicColors.SurfaceCard,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Доступно обновление ${update.versionTag}",
                color = CosmicColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Вы можете скачать новую версию прямо с GitHub — весь ваш прогресс сохранится.",
                    color = CosmicColors.NeonCyan,
                    fontSize = 13.sp
                )
                if (update.releaseNotes.isNotEmpty()) {
                    Text(
                        text = update.releaseNotes,
                        color = CosmicColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        uriHandler.openUri(update.apkDownloadUrl)
                    } catch (_: Throwable) {
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CosmicColors.NeonCyan,
                    contentColor = Color(0xFF04131D)
                )
            ) {
                Text("Скачать APK", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Позже", color = CosmicColors.TextSecondary)
            }
        }
    )
}

@Composable
private fun OfflineRewardDialog(
    reward: OfflineReward,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CosmicColors.SurfaceCard,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "С возвращением!",
                color = CosmicColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Пока вас не было (${NumberFormatter.formatDuration(reward.elapsedSeconds)}), ваш флот добыл +${NumberFormatter.format(reward.crystalsEarned)} кристаллов!",
                color = CosmicColors.TextSecondary,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CosmicColors.NeonCyan,
                    contentColor = Color(0xFF04131D)
                )
            ) {
                Text("Забрать", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun TopCurrencyHeader(
    crystals: Double,
    passivePerSec: Double,
    clickPower: Double,
    darkMatter: Long,
    prestigeMultiplier: Double,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Кристаллы",
                    color = CosmicColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                if (darkMatter > 0L) {
                    Text(
                        text = "$darkMatter ТМ (x${NumberFormatter.formatRate(prestigeMultiplier)})",
                        color = CosmicColors.NeonPurpleBright,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = NumberFormatter.format(crystals),
                color = CosmicColors.TextPrimary,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(3.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "+${NumberFormatter.formatRate(passivePerSec)} / сек",
                    color = CosmicColors.NeonPurpleBright,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "+${NumberFormatter.formatRate(clickPower)} за клик",
                    color = CosmicColors.NeonCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = "Настройки",
                tint = CosmicColors.TextSecondary
            )
        }
    }
}

@Composable
private fun RankProgressSection(
    rankTitle: String,
    rankLevel: Int,
    multiplier: Double,
    progress: Float,
    experience: Double,
    nextThreshold: Double?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = "Ранг $rankLevel · $rankTitle",
            color = CosmicColors.NeonGold,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (nextThreshold != null) {
                "Множитель x$multiplier · ${NumberFormatter.format(experience)} / ${NumberFormatter.format(nextThreshold)}"
            } else {
                "Макс. ранг · Множитель x$multiplier"
            },
            color = CosmicColors.TextSecondary,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = CosmicColors.NeonGold,
            trackColor = CosmicColors.SurfaceElevated
        )
    }
}
