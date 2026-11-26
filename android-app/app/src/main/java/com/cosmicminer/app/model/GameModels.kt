package com.cosmicminer.app.model

import com.cosmicminer.app.util.AppUpdateInfo
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt

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
    CRITICAL_HITS,
    COMETS_CAUGHT,
    PRESTIGE_COUNT
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
    val isCritical: Boolean,
    val isComet: Boolean = false
)

data class OfflineReward(
    val elapsedSeconds: Long,
    val crystalsEarned: Double
)

enum class BottomTab(val title: String) {
    UPGRADES("Улучшения"),
    ACHIEVEMENTS("Достижения"),
    PRESTIGE("Коллапс")
}

data class GameUiState(
    val crystals: Double = 0.0,
    val runCrystalsEarned: Double = 0.0,
    val totalCrystalsEarned: Double = 0.0,
    val experience: Double = 0.0,
    val totalClicks: Long = 0L,
    val criticalHits: Long = 0L,
    val cometsCaught: Long = 0L,
    val clickPower: Double = 1.0,
    val passivePerSecond: Double = 0.0,
    val currentRank: MinerRank = GameCatalog.ranks.first(),
    val nextRank: MinerRank? = GameCatalog.ranks.getOrNull(1),
    val rankProgress: Float = 0f,
    val darkMatter: Long = 0L,
    val prestigeCount: Int = 0,
    val potentialDarkMatter: Long = 0L,
    val prestigeMultiplier: Double = 1.0,
    val nextDarkMatterThreshold: Double = GameCatalog.PRESTIGE_BASE_THRESHOLD,
    val overchargeSecondsLeft: Int = 0,
    val isCometVisible: Boolean = false,
    val cometSeed: Long = 0L,
    val offlineReward: OfflineReward? = null,
    val availableUpdate: AppUpdateInfo? = null,
    val isCheckingUpdate: Boolean = false,
    val updateStatusMessage: String? = null,
    val upgrades: List<UpgradeItemUi> = emptyList(),
    val achievements: List<AchievementItemUi> = emptyList(),
    val unclaimedAchievementsCount: Int = 0,
    val selectedTab: BottomTab = BottomTab.UPGRADES,
    val isVibrationEnabled: Boolean = true,
    val isSoundEnabled: Boolean = true,
    val isLoaded: Boolean = false
)

object GameCatalog {
    const val MAX_PLAYER_LEVEL = 200
    const val MAX_UPGRADE_LEVEL = 200
    const val PRESTIGE_BASE_THRESHOLD = 100_000.0
    const val OVERCHARGE_DURATION_SEC = 10
    const val OVERCHARGE_MULTIPLIER = 5.0
    const val MAX_OFFLINE_SECONDS = 12L * 3600L

    fun calculatePotentialDarkMatter(runCrystalsEarned: Double): Long {
        if (runCrystalsEarned < PRESTIGE_BASE_THRESHOLD) return 0L
        return floor(sqrt(runCrystalsEarned / PRESTIGE_BASE_THRESHOLD)).toLong()
    }

    fun nextDarkMatterRequirement(currentPotential: Long): Double {
        val next = currentPotential + 1L
        return next.toDouble() * next.toDouble() * PRESTIGE_BASE_THRESHOLD
    }

    private fun titleForLevel(level: Int): String = when {
        level <= 15 -> "Новичок"
        level <= 30 -> "Стажёр орбиты"
        level <= 49 -> "Шахтёр"
        level <= 69 -> "Рейнджер пояса"
        level <= 89 -> "Мастер"
        level <= 109 -> "Звёздный магнат"
        level <= 129 -> "Космический барон"
        level <= 149 -> "Повелитель сингулярности"
        level <= 169 -> "Хранитель туманности"
        level <= 184 -> "Архитектор галактик"
        level <= 199 -> "Покоритель пустоты"
        else -> "Бог кристаллов"
    }

    val ranks: List<MinerRank> = (1..MAX_PLAYER_LEVEL).map { level ->
        val xpThreshold = when (level) {
            1 -> 0.0
            MAX_PLAYER_LEVEL -> 600_000_000.0
            else -> floor(25.0 * (24_000_000.0).pow((level - 2) / 198.0))
        }
        val t = (level - 1) / (MAX_PLAYER_LEVEL - 1).toDouble()
        val rawMult = 1.0 + 11.0 * t.pow(2.2)
        val multiplier = kotlin.math.round(rawMult * 100.0) / 100.0
        MinerRank(
            level = level,
            title = titleForLevel(level),
            xpThreshold = xpThreshold,
            multiplier = multiplier
        )
    }

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
            id = "core_annihilator",
            title = "Аннигилятор коры",
            description = "+600 к силе клика",
            type = UpgradeType.CLICK_POWER,
            baseCost = 140_000.0,
            baseGain = 600.0
        ),
        UpgradeDefinition(
            id = "supernova_prism",
            title = "Сверхновая призма",
            description = "+3.5K к силе клика",
            type = UpgradeType.CLICK_POWER,
            baseCost = 1_800_000.0,
            baseGain = 3_500.0
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
        ),
        UpgradeDefinition(
            id = "star_forge",
            title = "Звёздная кузница",
            description = "+4.2K крист./сек",
            type = UpgradeType.PASSIVE_INCOME,
            baseCost = 350_000.0,
            baseGain = 4_200.0
        ),
        UpgradeDefinition(
            id = "multiverse_matrix",
            title = "Матрица вселенной",
            description = "+30K крист./сек",
            type = UpgradeType.PASSIVE_INCOME,
            baseCost = 4_500_000.0,
            baseGain = 30_000.0
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
        ),
        AchievementDefinition(
            id = "clicks_1000",
            title = "Ударная волна",
            description = "Сделать 1 000 кликов по кристаллу",
            metric = AchievementMetric.TOTAL_CLICKS,
            targetValue = 1_000.0,
            rewardCrystals = 5_000.0
        ),
        AchievementDefinition(
            id = "upgrades_50",
            title = "Промышленный гигант",
            description = "Купить 50 улучшений за один заход",
            metric = AchievementMetric.TOTAL_UPGRADES_BOUGHT,
            targetValue = 50.0,
            rewardCrystals = 15_000.0
        ),
        AchievementDefinition(
            id = "comets_3",
            title = "Ловец комет",
            description = "Поймать 3 пролетающие кометы",
            metric = AchievementMetric.COMETS_CAUGHT,
            targetValue = 3.0,
            rewardCrystals = 25_000.0
        ),
        AchievementDefinition(
            id = "crystals_1m",
            title = "Миллионер орбиты",
            description = "Добыть 1M кристаллов за всё время",
            metric = AchievementMetric.TOTAL_CRYSTALS_EARNED,
            targetValue = 1_000_000.0,
            rewardCrystals = 50_000.0
        ),
        AchievementDefinition(
            id = "passive_5k",
            title = "Космический магнат",
            description = "Достичь пассивного дохода 5K/сек",
            metric = AchievementMetric.PASSIVE_PER_SEC,
            targetValue = 5_000.0,
            rewardCrystals = 150_000.0
        ),
        AchievementDefinition(
            id = "crits_150",
            title = "Мастер резонанса",
            description = "Выбить 150 критических ударов",
            metric = AchievementMetric.CRITICAL_HITS,
            targetValue = 150.0,
            rewardCrystals = 300_000.0
        ),
        AchievementDefinition(
            id = "prestige_1",
            title = "Повелитель Коллапса",
            description = "Совершить свой первый Коллапс (престиж)",
            metric = AchievementMetric.PRESTIGE_COUNT,
            targetValue = 1.0,
            rewardCrystals = 500_000.0
        )
    )
}
