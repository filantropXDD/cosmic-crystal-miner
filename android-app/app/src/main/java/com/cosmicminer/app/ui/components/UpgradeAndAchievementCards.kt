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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.SatelliteAlt
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.definition.title,
                        color = CosmicColors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Text(
                        text = "Ур. ${item.level}",
                        color = accentColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${item.definition.description} · ${if (isClickType) "Клик" else "Авто"}",
                    color = CosmicColors.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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

private fun upgradeIconFor(id: String): ImageVector {
    return when (id) {
        "laser_pickaxe" -> Icons.Rounded.TouchApp
        "plasma_drill" -> Icons.Rounded.Build
        "quantum_amplifier" -> Icons.Rounded.Bolt
        "gravity_hammer" -> Icons.Rounded.AutoAwesome
        "mini_drone" -> Icons.Rounded.Memory
        "orbital_station" -> Icons.Rounded.SatelliteAlt
        "singularity_reactor" -> Icons.Rounded.RocketLaunch
        "dyson_sphere" -> Icons.Rounded.Public
        else -> Icons.Rounded.Bolt
    }
}
