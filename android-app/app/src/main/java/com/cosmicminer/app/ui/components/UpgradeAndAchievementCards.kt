package com.cosmicminer.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.SatelliteAlt
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmicminer.app.model.AchievementItemUi
import com.cosmicminer.app.model.GameCatalog
import com.cosmicminer.app.model.UpgradeItemUi
import com.cosmicminer.app.model.UpgradeType
import com.cosmicminer.app.ui.theme.CosmicColors
import com.cosmicminer.app.util.NumberFormatter

@Composable
fun UpgradesList(
    upgrades: List<UpgradeItemUi>,
    onBuyUpgrade: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(upgrades, key = { it.definition.id }) { item ->
            UpgradeCard(
                item = item,
                onBuy = { onBuyUpgrade(item.definition.id) }
            )
        }
    }
}

@Composable
private fun UpgradeCard(
    item: UpgradeItemUi,
    onBuy: () -> Unit
) {
    val isClickType = item.definition.type == UpgradeType.CLICK_POWER
    val accentColor = if (isClickType) CosmicColors.NeonCyan else CosmicColors.NeonPurpleBright
    val icon = upgradeIconFor(item.definition.id)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicColors.SurfaceCard),
        border = BorderStroke(
            width = 1.dp,
            color = if (item.canAfford) accentColor.copy(alpha = 0.45f) else CosmicColors.BorderSubtle
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = item.definition.title,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.definition.title,
                    color = CosmicColors.TextPrimary,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Ур. ${item.level}",
                        color = accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "· ${item.definition.description}",
                        color = CosmicColors.TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = onBuy,
                enabled = item.canAfford,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isClickType) CosmicColors.NeonCyan else CosmicColors.NeonPurple,
                    contentColor = if (isClickType) Color(0xFF04131D) else Color.White,
                    disabledContainerColor = CosmicColors.SurfaceElevated.copy(alpha = 0.55f),
                    disabledContentColor = CosmicColors.TextSecondary.copy(alpha = 0.55f)
                ),
                modifier = Modifier.alpha(if (item.canAfford) 1f else 0.6f)
            ) {
                Text(
                    text = NumberFormatter.format(item.nextCost),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun AchievementsList(
    achievements: List<AchievementItemUi>,
    onClaimAchievement: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(achievements, key = { it.definition.id }) { item ->
            AchievementCard(
                item = item,
                onClaim = { onClaimAchievement(item.definition.id) }
            )
        }
    }
}

@Composable
private fun AchievementCard(
    item: AchievementItemUi,
    onClaim: () -> Unit
) {
    val isReadyToClaim = item.isUnlocked && !item.isClaimed

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicColors.SurfaceCard),
        border = BorderStroke(
            width = 1.dp,
            color = when {
                isReadyToClaim -> CosmicColors.NeonGold.copy(alpha = 0.7f)
                item.isClaimed -> CosmicColors.EmeraldSuccess.copy(alpha = 0.35f)
                else -> CosmicColors.BorderSubtle
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (item.isClaimed) CosmicColors.EmeraldSuccess.copy(alpha = 0.15f)
                            else CosmicColors.NeonGold.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (item.isClaimed) Icons.Rounded.CheckCircle else Icons.Rounded.EmojiEvents,
                        contentDescription = item.definition.title,
                        tint = if (item.isClaimed) CosmicColors.EmeraldSuccess else CosmicColors.NeonGold,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.definition.title,
                        color = CosmicColors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = item.definition.description,
                        color = CosmicColors.TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                when {
                    item.isClaimed -> {
                        Text(
                            text = "Получено",
                            color = CosmicColors.EmeraldSuccess,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    isReadyToClaim -> {
                        Button(
                            onClick = onClaim,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CosmicColors.NeonGold,
                                contentColor = Color(0xFF1F1600)
                            )
                        ) {
                            Text(
                                text = "Забрать +${NumberFormatter.format(item.definition.rewardCrystals)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = "+${NumberFormatter.format(item.definition.rewardCrystals)}",
                            color = CosmicColors.NeonGold.copy(alpha = 0.75f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LinearProgressIndicator(
                    progress = { item.progress },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (item.isUnlocked) CosmicColors.NeonGold else CosmicColors.NeonPurple,
                    trackColor = CosmicColors.SurfaceElevated
                )
                Text(
                    text = "${NumberFormatter.format(item.currentValue.coerceAtMost(item.definition.targetValue))} / ${NumberFormatter.format(item.definition.targetValue)}",
                    color = CosmicColors.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun PrestigeCollapsePanel(
    darkMatter: Long,
    prestigeCount: Int,
    prestigeMultiplier: Double,
    potentialDarkMatter: Long,
    runCrystalsEarned: Double,
    nextDarkMatterThreshold: Double,
    onCollapseConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showConfirm by remember { mutableStateOf(false) }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            containerColor = CosmicColors.SurfaceCard,
            title = {
                Text(
                    text = "Запустить Большой взрыв?",
                    color = CosmicColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Вы сбросите текущие кристаллы, ранг и улучшения, но получите +$potentialDarkMatter Тёмной материи. Каждый её фрагмент навсегда увеличивает весь доход на +50%.",
                    color = CosmicColors.TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirm = false
                        onCollapseConfirmed()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CosmicColors.NeonPurple,
                        contentColor = Color.White
                    )
                ) {
                    Text("Коллапс (+$potentialDarkMatter ТМ)", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("Отмена", color = CosmicColors.TextSecondary)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CosmicColors.SurfaceCard),
            border = BorderStroke(1.dp, CosmicColors.NeonPurple.copy(alpha = 0.45f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Система престижа · Коллапс",
                    color = CosmicColors.NeonPurpleBright,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Сбросьте текущую добычу ради Тёмной материи. Каждая единица Тёмной материи даёт +50% ко всему доходу навсегда.",
                    color = CosmicColors.TextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Тёмная материя", color = CosmicColors.TextSecondary, fontSize = 12.sp)
                        Text(
                            text = "$darkMatter ТМ",
                            color = CosmicColors.NeonPurpleBright,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Постоянный бонус", color = CosmicColors.TextSecondary, fontSize = 12.sp)
                        Text(
                            text = "x${NumberFormatter.formatRate(prestigeMultiplier)}",
                            color = CosmicColors.NeonGold,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Перезагрузок совершено: $prestigeCount",
                    color = CosmicColors.TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CosmicColors.SurfaceCard),
            border = BorderStroke(
                1.dp,
                if (potentialDarkMatter > 0) CosmicColors.NeonGold.copy(alpha = 0.6f)
                else CosmicColors.BorderSubtle
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (potentialDarkMatter > 0) {
                        "Доступно к получению: +$potentialDarkMatter ТМ"
                    } else {
                        "Накопите ${NumberFormatter.format(GameCatalog.PRESTIGE_BASE_THRESHOLD)} кристаллов за заход"
                    },
                    color = if (potentialDarkMatter > 0) CosmicColors.NeonGold else CosmicColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                val progress = (runCrystalsEarned / nextDarkMatterThreshold).toFloat().coerceIn(0f, 1f)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = CosmicColors.NeonPurpleBright,
                    trackColor = CosmicColors.SurfaceElevated
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Следующая ТМ: ${NumberFormatter.format(runCrystalsEarned)} / ${NumberFormatter.format(nextDarkMatterThreshold)}",
                    color = CosmicColors.TextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { showConfirm = true },
                    enabled = potentialDarkMatter > 0L,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CosmicColors.NeonPurple,
                        contentColor = Color.White,
                        disabledContainerColor = CosmicColors.SurfaceElevated.copy(alpha = 0.55f),
                        disabledContentColor = CosmicColors.TextSecondary.copy(alpha = 0.55f)
                    )
                ) {
                    Text(
                        text = if (potentialDarkMatter > 0L) "Совершить Коллапс (+$potentialDarkMatter ТМ)"
                        else "Недостаточно кристаллов",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun upgradeIconFor(id: String): ImageVector {
    return when (id) {
        "laser_pickaxe" -> Icons.Rounded.TouchApp
        "plasma_drill" -> Icons.Rounded.Build
        "quantum_amplifier" -> Icons.Rounded.Bolt
        "gravity_hammer" -> Icons.Rounded.AutoAwesome
        "core_annihilator" -> Icons.Rounded.Whatshot
        "supernova_prism" -> Icons.Rounded.Flare
        "mini_drone" -> Icons.Rounded.Memory
        "orbital_station" -> Icons.Rounded.SatelliteAlt
        "singularity_reactor" -> Icons.Rounded.RocketLaunch
        "dyson_sphere" -> Icons.Rounded.Public
        "star_forge" -> Icons.Rounded.AutoAwesome
        "multiverse_matrix" -> Icons.Rounded.Hub
        else -> Icons.Rounded.Bolt
    }
}
