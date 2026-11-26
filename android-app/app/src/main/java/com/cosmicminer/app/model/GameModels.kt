package com.cosmicminer.app.model

import kotlin.math.floor
import kotlin.math.pow

enum class UpgradeType {
    CLICK_POWER,
    PASSIVE_INCOME
}

data class UpgradeDefinition(
    val id: String,
    val title: String,
    val description: String,
    val type: UpgradeType,
    val baseCost: Double,
    val baseGain: Double,
    val costMultiplier: Double = 1.15
) {
    fun costAtLevel(level: Int): Double {
        return floor(baseCost * costMultiplier.pow(level.toDouble()))
    }
}

data class UpgradeItemUi(
    val definition: UpgradeDefinition,
    val level: Int,
    val nextCost: Double,
    val canAfford: Boolean
)

data class MinerRank(
    val level: Int,
    val title: String,
    val xpThreshold: Double,
    val multiplier: Double
)

enum class AchievementMetric {
    TOTAL_CLICKS,
    TOTAL_CRYSTALS_EARNED,
    TOTAL_UPGRADES_BOUGHT,
    PASSIVE_PER_SEC,
    CRITICAL_HITS
}

data class AchievementDefinition(
    val id: String,
    val title: String,
    val description: String,
    val metric: AchievementMetric,
    val targetValue: Double,
    val rewardCrystals: Double
)

data class AchievementItemUi(
    val definition: AchievementDefinition,
    val currentValue: Double,
    val progress: Float,
    val isUnlocked: Boolean,
    val isClaimed: Boolean
)

data class FloatingNumber(
    val id: Long,
    val text: String,
    val x: Float,
    val y: Float,
    val isCritical: Boolean
)

enum class BottomTab(val title: String) {
    UPGRADES("Улучшения"),
    ACHIEVEMENTS("Достижения")
}

data class GameUiState(
    val crystals: Double = 0.0,
    val totalCrystalsEarned: Double = 0.0,
    val experience: Double = 0.0,
    val totalClicks: Long = 0L,
    val criticalHits: Long = 0L,
    val clickPower: Double = 1.0,
    val passivePerSecond: Double = 0.0,
    val currentRank: MinerRank = GameCatalog.ranks.first(),
    val nextRank: MinerRank? = GameCatalog.ranks.getOrNull(1),
    val rankProgress: Float = 0f,
    val upgrades: List<UpgradeItemUi> = emptyList(),
    val achievements: List<AchievementItemUi> = emptyList(),
    val unclaimedAchievementsCount: Int = 0,
    val selectedTab: BottomTab = BottomTab.UPGRADES,
    val isVibrationEnabled: Boolean = true,
    val isLoaded: Boolean = false
)

object GameCatalog {
    val ranks: List<MinerRank> = listOf(
        MinerRank(1, "Новичок", 0.0, 1.0),
        MinerRank(2, "Стажёр орбиты", 100.0, 1.15),
        MinerRank(3, "Шахтёр", 600.0, 1.35),
        MinerRank(4, "Рейнджер пояса", 2_500.0, 1.6),
        MinerRank(5, "Мастер", 10_000.0, 2.0),
        MinerRank(6, "Звёздный магнат", 45_000.0, 2.5),
        MinerRank(7, "Космический барон", 180_000.0, 3.2),
        MinerRank(8, "Повелитель сингулярности", 750_000.0, 4.0)
    )

    val upgrades: List<UpgradeDefinition> = listOf(
        UpgradeDefinition(
            id = "laser_pickaxe",
            title = "Лазерная кирка",
            description = "+1 к силе клика",
            type = UpgradeType.CLICK_POWER,
            baseCost = 15.0,
            baseGain = 1.0
        ),
        UpgradeDefinition(
            id = "plasma_drill",
            title = "Плазменный бур",
            description = "+5 к силе клика",
            type = UpgradeType.CLICK_POWER,
            baseCost = 120.0,
            baseGain = 5.0
        ),
        UpgradeDefinition(
            id = "quantum_amplifier",
            title = "Квантовый усилитель",
            description = "+25 к силе клика",
            type = UpgradeType.CLICK_POWER,
            baseCost = 1_100.0,
            baseGain = 25.0
        ),
        UpgradeDefinition(
            id = "gravity_hammer",
            title = "Гравитационный молот",
            description = "+120 к силе клика",
            type = UpgradeType.CLICK_POWER,
            baseCost = 12_000.0,
            baseGain = 120.0
        ),
        UpgradeDefinition(
            id = "mini_drone",
            title = "Мини-дрон",
            description = "+2 крист./сек",
            type = UpgradeType.PASSIVE_INCOME,
            baseCost = 25.0,
            baseGain = 2.0
        ),
        UpgradeDefinition(
            id = "orbital_station",
            title = "Орбитальная станция",
            description = "+15 крист./сек",
            type = UpgradeType.PASSIVE_INCOME,
            baseCost = 220.0,
            baseGain = 15.0
        ),
        UpgradeDefinition(
            id = "singularity_reactor",
            title = "Сингулярный реактор",
            description = "+100 крист./сек",
            type = UpgradeType.PASSIVE_INCOME,
            baseCost = 2_400.0,
            baseGain = 100.0
        ),
        UpgradeDefinition(
            id = "dyson_sphere",
            title = "Сфера Дайсона",
            description = "+650 крист./сек",
            type = UpgradeType.PASSIVE_INCOME,
            baseCost = 28_000.0,
            baseGain = 650.0
        )
    )

    val achievements: List<AchievementDefinition> = listOf(
        AchievementDefinition(
            id = "clicks_100",
            title = "Первый импульс",
            description = "Сделать 100 кликов по кристаллу",
            metric = AchievementMetric.TOTAL_CLICKS,
            targetValue = 100.0,
            rewardCrystals = 50.0
        ),
        AchievementDefinition(
            id = "crystals_1k",
            title = "Тысяча граней",
            description = "Добыть 1 000 кристаллов за всё время",
            metric = AchievementMetric.TOTAL_CRYSTALS_EARNED,
            targetValue = 1_000.0,
            rewardCrystals = 250.0
        ),
        AchievementDefinition(
            id = "upgrades_10",
            title = "Инженер флота",
            description = "Купить 10 любых улучшений",
            metric = AchievementMetric.TOTAL_UPGRADES_BOUGHT,
            targetValue = 10.0,
            rewardCrystals = 500.0
        ),
        AchievementDefinition(
            id = "passive_50",
            title = "Автономная сеть",
            description = "Достичь пассивного дохода 50/сек",
            metric = AchievementMetric.PASSIVE_PER_SEC,
            targetValue = 50.0,
            rewardCrystals = 1_500.0
        ),
        AchievementDefinition(
            id = "crits_25",
            title = "Охотник за критами",
            description = "Выбить 25 критических кликов (x3)",
            metric = AchievementMetric.CRITICAL_HITS,
            targetValue = 25.0,
            rewardCrystals = 2_500.0
        )
    )
}
