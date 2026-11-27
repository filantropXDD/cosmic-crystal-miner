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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.TaskAlt
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cosmicminer.app.model.BossUiState
import com.cosmicminer.app.model.CrystalSkin
import com.cosmicminer.app.model.DailyQuestItemUi
import com.cosmicminer.app.model.ExpeditionUiState
import com.cosmicminer.app.model.GameCatalog
import com.cosmicminer.app.model.SpaceLocation
import com.cosmicminer.app.ui.theme.CosmicColors
import com.cosmicminer.app.util.NumberFormatter

@Composable
fun FleetAndEventsPanel(
    bossState: BossUiState,
    expeditionState: ExpeditionUiState,
    dailyQuests: List<DailyQuestItemUi>,
    onSummonBoss: () -> Unit,
    onStartExpedition: (String) -> Unit,
    onClaimExpedition: () -> Unit,
    onClaimDailyQuest: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Boss Asteroid Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicColors.SurfaceCard),
                border = BorderStroke(1.dp, Color(0xFFF87171).copy(alpha = 0.45f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Whatshot,
                            contentDescription = null,
                            tint = Color(0xFFF87171),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Босс-астероид (30 сек)",
                            color = CosmicColors.TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "Разбейте бронированного Левиафана быстрыми кликами и дронами, чтобы получить сундук с кристаллами и шанс на Тёмную материю.",
                        color = CosmicColors.TextSecondary,
                        fontSize = 12.sp
                    )
                    if (bossState.lastResultMessage != null) {
                        Text(
                            text = bossState.lastResultMessage,
                            color = CosmicColors.NeonGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    val canSummon = !bossState.isActive && bossState.cooldownSeconds == 0
                    Button(
                        onClick = onSummonBoss,
                        enabled = canSummon,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White,
                            disabledContainerColor = CosmicColors.SurfaceElevated.copy(alpha = 0.55f),
                            disabledContentColor = CosmicColors.TextSecondary
                        )
                    ) {
                        Text(
                            text = when {
                                bossState.isActive -> "Бой уже идёт (${bossState.remainingSeconds}с)"
                                bossState.cooldownSeconds > 0 -> "Перезарядка: ${bossState.cooldownSeconds}с"
                                else -> "Вызвать босса"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Expeditions Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicColors.SurfaceCard),
                border = BorderStroke(1.dp, CosmicColors.NeonCyan.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Explore,
                            contentDescription = null,
                            tint = CosmicColors.NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Экспедиции флота",
                            color = CosmicColors.TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (expeditionState.lastRewardMessage != null) {
                        Text(
                            text = expeditionState.lastRewardMessage,
                            color = CosmicColors.EmeraldSuccess,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (expeditionState.activePresetId != null) {
                        val active = GameCatalog.expeditions.find { it.id == expeditionState.activePresetId }
                        Text(
                            text = active?.title ?: "Экспедиция",
                            color = CosmicColors.NeonCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (expeditionState.isCompleted) {
                            Button(
                                onClick = onClaimExpedition,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CosmicColors.EmeraldSuccess,
                                    contentColor = Color(0xFF042016)
                                )
                            ) {
                                Text("Забрать находки", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text(
                                text = "До возвращения: ${NumberFormatter.formatDuration(expeditionState.remainingSeconds)}",
                                color = CosmicColors.TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        GameCatalog.expeditions.forEach { preset ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CosmicColors.SurfaceElevated.copy(alpha = 0.55f))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${preset.title} · ${preset.durationSeconds / 60} мин",
                                        color = CosmicColors.TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = preset.subtitle,
                                        color = CosmicColors.TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { onStartExpedition(preset.id) },
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = CosmicColors.NeonCyan,
                                        contentColor = Color(0xFF04131D)
                                    )
                                ) {
                                    Text("Старт", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Daily Quests Section
        item {
            Text(
                text = "Ежедневные мини-квесты",
                color = CosmicColors.NeonGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
        items(dailyQuests, key = { it.definition.id }) { quest ->
            val progress = (quest.progress.toFloat() / quest.definition.target.toFloat()).coerceIn(0f, 1f)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = CosmicColors.SurfaceCard),
                border = BorderStroke(
                    1.dp,
                    if (quest.isCompleted && !quest.isClaimed) CosmicColors.NeonGold.copy(alpha = 0.65f)
                    else CosmicColors.BorderSubtle
                )
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (quest.isClaimed) Icons.Rounded.CheckCircle else Icons.Rounded.TaskAlt,
                            contentDescription = null,
                            tint = if (quest.isClaimed) CosmicColors.EmeraldSuccess else CosmicColors.NeonGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = quest.definition.title,
                                color = CosmicColors.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Награда: +${quest.definition.rewardDarkMatter} ТМ и кристаллы",
                                color = CosmicColors.TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        if (quest.isClaimed) {
                            Text("Выполнено", color = CosmicColors.EmeraldSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        } else if (quest.isCompleted) {
                            Button(
                                onClick = { onClaimDailyQuest(quest.definition.id) },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CosmicColors.NeonGold,
                                    contentColor = Color(0xFF1F1600)
                                )
                            ) {
                                Text("Забрать", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text(
                                text = "${quest.progress.coerceAtMost(quest.definition.target)}/${quest.definition.target}",
                                color = CosmicColors.NeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (quest.isCompleted) CosmicColors.NeonGold else CosmicColors.NeonPurpleBright,
                        trackColor = CosmicColors.SurfaceElevated
                    )
                }
            }
        }
    }
}

@Composable
fun SkinsAndLocationsPanel(
    selectedSkin: CrystalSkin,
    selectedLocation: SpaceLocation,
    unlockedAchievementIds: Set<String>,
    onSelectSkin: (CrystalSkin) -> Unit,
    onSelectLocation: (SpaceLocation) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Коллекция объектов (открываются за достижения)",
                color = CosmicColors.NeonCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        items(CrystalSkin.entries, key = { it.id }) { skin ->
            val unlocked = skin.requiredAchievementId == null || unlockedAchievementIds.contains(skin.requiredAchievementId)
            val isSelected = skin == selectedSkin
            CustomizationItemCard(
                title = skin.title,
                subtitle = skin.subtitle,
                unlockHint = skin.unlockHint,
                isUnlocked = unlocked,
                isSelected = isSelected,
                accentColor = CosmicColors.NeonCyan,
                onSelect = { onSelectSkin(skin) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Космические локации",
                color = CosmicColors.NeonPurpleBright,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
        items(SpaceLocation.entries, key = { it.id }) { loc ->
            val unlocked = loc.requiredAchievementId == null || unlockedAchievementIds.contains(loc.requiredAchievementId)
            val isSelected = loc == selectedLocation
            CustomizationItemCard(
                title = loc.title,
                subtitle = loc.subtitle,
                unlockHint = loc.unlockHint,
                isUnlocked = unlocked,
                isSelected = isSelected,
                accentColor = CosmicColors.NeonPurpleBright,
                onSelect = { onSelectLocation(loc) }
            )
        }
    }
}

@Composable
private fun CustomizationItemCard(
    title: String,
    subtitle: String,
    unlockHint: String,
    isUnlocked: Boolean,
    isSelected: Boolean,
    accentColor: Color,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CosmicColors.SurfaceCard),
        border = BorderStroke(
            1.dp,
            when {
                isSelected -> accentColor.copy(alpha = 0.85f)
                isUnlocked -> CosmicColors.BorderSubtle
                else -> Color.White.copy(alpha = 0.08f)
            }
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
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isUnlocked) accentColor.copy(alpha = 0.16f) else CosmicColors.SurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isUnlocked) Icons.Rounded.AutoAwesome else Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = if (isUnlocked) accentColor else CosmicColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (isUnlocked) CosmicColors.TextPrimary else CosmicColors.TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isUnlocked) subtitle else unlockHint,
                    color = if (isUnlocked) CosmicColors.TextSecondary else CosmicColors.NeonGold.copy(alpha = 0.85f),
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (isUnlocked) {
                Button(
                    onClick = onSelect,
                    enabled = !isSelected,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color(0xFF04131D),
                        disabledContainerColor = CosmicColors.EmeraldSuccess.copy(alpha = 0.22f),
                        disabledContentColor = CosmicColors.EmeraldSuccess
                    )
                ) {
                    Text(
                        text = if (isSelected) "Выбрано" else "Выбрать",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun StatsDialog(
    totalClicks: Long,
    bestCps: Int,
    currentCps: Int,
    criticalHits: Long,
    cometsCaught: Long,
    bossesDefeated: Long,
    feverActivations: Long,
    playTimeSeconds: Long,
    totalCrystalsEarned: Double,
    darkMatter: Long,
    prestigeCount: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CosmicColors.SurfaceCard,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = "Статистика добытчика",
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
                StatRow("Всего кликов", totalClicks.toString())
                StatRow("Рекорд CPS (кликов/сек)", "$bestCps (сейчас $currentCps)")
                StatRow("Выбито критов (x3)", criticalHits.toString(), CosmicColors.NeonGold)
                StatRow("Поймано комет", cometsCaught.toString(), CosmicColors.NeonCyan)
                StatRow("Побеждено боссов", bossesDefeated.toString(), Color(0xFFF87171))
                StatRow("Активаций Ярости", feverActivations.toString(), CosmicColors.NeonGold)
                StatRow("Время в игре", NumberFormatter.formatDuration(playTimeSeconds))
                StatRow("Добыто кристаллов", NumberFormatter.format(totalCrystalsEarned), CosmicColors.NeonCyan)
                StatRow("Тёмная материя / Коллапсов", "$darkMatter ТМ · $prestigeCount", CosmicColors.NeonPurpleBright)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть", color = CosmicColors.NeonCyan, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    valueColor: Color = CosmicColors.TextPrimary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CosmicColors.SurfaceElevated.copy(alpha = 0.55f))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = CosmicColors.TextSecondary, fontSize = 12.sp)
        Text(text = value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}
