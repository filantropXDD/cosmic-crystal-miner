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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmicminer.app.model.BottomTab
import com.cosmicminer.app.ui.components.AchievementsList
import com.cosmicminer.app.ui.components.CrystalArena
import com.cosmicminer.app.ui.components.SettingsDialog
import com.cosmicminer.app.ui.components.UpgradesList
import com.cosmicminer.app.ui.theme.CosmicColors
import com.cosmicminer.app.util.NumberFormatter

@Composable
fun CosmicMinerScreen(viewModel: GameViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        SettingsDialog(
            isVibrationEnabled = state.isVibrationEnabled,
            totalClicks = state.totalClicks,
            criticalHits = state.criticalHits,
            onVibrationChange = viewModel::setVibrationEnabled,
            onResetConfirmed = viewModel::resetProgress,
            onDismiss = { showSettings = false }
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
                // Top HUD
                TopCurrencyHeader(
                    crystals = state.crystals,
                    passivePerSec = state.passivePerSecond,
                    clickPower = state.clickPower,
                    onOpenSettings = { showSettings = true }
                )

                // Rank / XP Progress Bar
                RankProgressSection(
                    rankTitle = state.currentRank.title,
                    rankLevel = state.currentRank.level,
                    multiplier = state.currentRank.multiplier,
                    progress = state.rankProgress,
                    experience = state.experience,
                    nextThreshold = state.nextRank?.xpThreshold
                )

                // Central Interactive Crystal
                CrystalArena(
                    isVibrationEnabled = state.isVibrationEnabled,
                    onCrystalTap = viewModel::onCrystalClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.42f)
                )

                // Bottom Panel: Tabs + Scrollable List
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
                            Tab(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                text = {
                                    if (tab == BottomTab.ACHIEVEMENTS && state.unclaimedAchievementsCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = CosmicColors.NeonGold,
                                                    contentColor = Color.Black
                                                ) {
                                                    Text(state.unclaimedAchievementsCount.toString())
                                                }
                                            }
                                        ) {
                                            Text(
                                                text = tab.title,
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (selected) CosmicColors.TextPrimary else CosmicColors.TextSecondary
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = tab.title,
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
                    }
                }
            }
        }
    }
}

@Composable
private fun TopCurrencyHeader(
    crystals: Double,
    passivePerSec: Double,
    clickPower: Double,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Кристаллы",
                color = CosmicColors.TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = NumberFormatter.format(crystals),
                color = CosmicColors.TextPrimary,
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(4.dp))
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ранг $rankLevel · $rankTitle",
                color = CosmicColors.NeonGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (nextThreshold != null) {
                    "Множитель x$multiplier · ${NumberFormatter.format(experience)} / ${NumberFormatter.format(nextThreshold)}"
                } else {
                    "Макс. ранг · Множитель x$multiplier"
                },
                color = CosmicColors.TextSecondary,
                fontSize = 11.sp
            )
        }

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
