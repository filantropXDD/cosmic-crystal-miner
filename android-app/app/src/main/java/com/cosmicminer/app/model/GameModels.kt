package com.cosmicminer.app.model

import com.cosmicminer.app.util.AppUpdateInfo
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.round
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
    fun costAtLevel(level: Int, discountFraction: Double = 0.0): Double {
        val raw = floor(baseCost * costMultiplier.pow(level.toDouble()))
        return floor(raw * (1.0 - discountFraction.coerceIn(0.0, 0.5)))
            .coerceAtLeast(1.0)
    }
}

enum class BuyQuantity(val label: String) {
    X1("x1"),
    X10("x10"),
    X100("x100"),
    MAX("MAX")
}

data class UpgradePurchasePlan(
    val levelsToBuy: Int,
    val totalCost: Double,
    val canAfford: Boolean
)

data class UpgradeItemUi(
    val definition: UpgradeDefinition,
    val level: Int,
    val nextCost: Double,
    val levelsToBuy: Int,
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
    PRESTIGE_COUNT,
    BOSSES_DEFEATED,
    FEVER_COUNT,
    SECRET_HEADER_TAP
}

data class AchievementDefinition(
    val id: String,
    val title: String,
    val description: String,
    val metric: AchievementMetric,
    val targetValue: Double,
    val rewardCrystals: Double,
    val isSecret: Boolean = false
)

data class AchievementItemUi(
    val definition: AchievementDefinition,
    val currentValue: Double,
    val progress: Float,
    val isUnlocked: Boolean,
    val isClaimed: Boolean
)

enum class CrystalSkin(
    val id: String,
    val title: String,
    val subtitle: String,
    val unlockHint: String,
    val requiredAchievementId: String?
) {
    CLASSIC(
        id = "classic",
        title = "Космический кристалл",
        subtitle = "Неоновые грани и орбитальные кольца",
        unlockHint = "Доступен с начала игры",
        requiredAchievementId = null
    ),
    BLACK_HOLE(
        id = "black_hole",
        title = "Чёрная дыра",
        subtitle = "Сфера горизонта и частицы, затягиваемые внутрь",
        unlockHint = "Совершите 1-й Коллапс (достижение «Повелитель Коллапса»)",
        requiredAchievementId = "prestige_1"
    ),
    SOLAR_CORE(
        id = "solar_core",
        title = "Солнечное ядро",
        subtitle = "Огненная звезда с короной и протуберанцами",
        unlockHint = "Поймайте 15 комет (достижение «Звёздный перехватчик»)",
        requiredAchievementId = "comets_15"
    ),
    CYBER_CUBE(
        id = "cyber_cube",
        title = "Кибер-куб",
        subtitle = "Неоновый тессеракт и зелёные биты Матрицы",
        unlockHint = "Купите 100 улучшений за заход («Архитектор флота»)",
        requiredAchievementId = "upgrades_100"
    ),
    ICE_RELIC(
        id = "ice_relic",
        title = "Ледяная реликвия",
        subtitle = "Кристалл вечной мерзлоты и снежинки-осколки",
        unlockHint = "Победите первого босса-астероида («Охотник на Левиафанов»)",
        requiredAchievementId = "boss_1"
    ),
    RAINBOW_PRISM(
        id = "rainbow_prism",
        title = "Радужная призма",
        subtitle = "Спектральная грань — меняет цвет при каждом тапе",
        unlockHint = "Добудьте 1M кристаллов («Миллионер орбиты»)",
        requiredAchievementId = "crystals_1m"
    );

    companion object {
        fun fromId(id: String): CrystalSkin = entries.find { it.id == id } ?: CLASSIC
    }
}

enum class SpaceLocation(
    val id: String,
    val title: String,
    val subtitle: String,
    val unlockHint: String,
    val requiredAchievementId: String?
) {
    NEBULA(
        id = "nebula",
        title = "Фиолетовая туманность",
        subtitle = "Мягкое эфирное свечение и россыпь звёзд",
        unlockHint = "Доступна с начала игры",
        requiredAchievementId = null
    ),
    RED_GIANT(
        id = "red_giant",
        title = "Система Красного гиганта",
        subtitle = "Багровый горизонт и раскалённая звёздная пыль",
        unlockHint = "Сделайте 1 000 кликов («Ударная волна»)",
        requiredAchievementId = "clicks_1000"
    ),
    ASTEROID_BELT(
        id = "asteroid_belt",
        title = "Пояс астероидов",
        subtitle = "Бирюзовая бездна и дрейфующие каменные глыбы",
        unlockHint = "Победите первого босса-астероида («Охотник на Левиафанов»)",
        requiredAchievementId = "boss_1"
    ),
    DEEP_VOID(
        id = "deep_void",
        title = "Глубокая пустота",
        subtitle = "Абсолютный мрак с падающими метеорами",
        unlockHint = "Активируйте Космическую ярость 5 раз («Вспышка сверхновой»)",
        requiredAchievementId = "fever_5"
    );

    companion object {
        fun fromId(id: String): SpaceLocation = entries.find { it.id == id } ?: NEBULA
    }
}

enum class TalentBranch(val title: String) {
    ACTIVE_CLICKER("Активный кликер"),
    TYCOON("Магнат")
}

data class TalentDefinition(
    val id: String,
    val title: String,
    val description: String,
    val branch: TalentBranch,
    val maxLevel: Int,
    val baseCostDm: Long,
    val costStepDm: Long
) {
    fun costAtLevel(level: Int): Long = baseCostDm + level * costStepDm
}

data class TalentItemUi(
    val definition: TalentDefinition,
    val level: Int,
    val nextCostDm: Long,
    val canAfford: Boolean,
    val isMaxed: Boolean
)

data class ExpeditionPreset(
    val id: String,
    val title: String,
    val subtitle: String,
    val durationSeconds: Long,
    val crystalSecondsEquivalent: Double,
    val darkMatterChance: Double
)

data class ExpeditionUiState(
    val activePresetId: String? = null,
    val remainingSeconds: Long = 0L,
    val isCompleted: Boolean = false,
    val lastRewardMessage: String? = null
)

data class BossUiState(
    val isActive: Boolean = false,
    val name: String = "Бронированный Левиафан",
    val currentHp: Double = 0.0,
    val maxHp: Double = 1.0,
    val remainingSeconds: Int = 0,
    val cooldownSeconds: Int = 0,
    val lastResultMessage: String? = null
)

enum class DailyQuestType {
    CLICKS,
    UPGRADES,
    FEVER
}

data class DailyQuestDefinition(
    val id: String,
    val title: String,
    val type: DailyQuestType,
    val target: Int,
    val rewardDarkMatter: Long,
    val rewardCrystalsFactor: Double
)

data class DailyQuestItemUi(
    val definition: DailyQuestDefinition,
    val progress: Int,
    val isCompleted: Boolean,
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
    FLEET("Флот"),
    SKINS("Скины"),
    ACHIEVEMENTS("Ачивки"),
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
    val bossesDefeated: Long = 0L,
    val feverActivations: Long = 0L,
    val bestCps: Int = 0,
    val currentCps: Int = 0,
    val playTimeSeconds: Long = 0L,
    val clickPower: Double = 1.0,
    val passivePerSecond: Double = 0.0,
    val currentRank: MinerRank = GameCatalog.ranks.first(),
    val nextRank: MinerRank? = GameCatalog.ranks.getOrNull(1),
    val rankProgress: Float = 0f,
    val darkMatter: Long = 0L,
    val spentDarkMatter: Long = 0L,
    val prestigeCount: Int = 0,
    val potentialDarkMatter: Long = 0L,
    val prestigeMultiplier: Double = 1.0,
    val nextDarkMatterThreshold: Double = GameCatalog.PRESTIGE_BASE_THRESHOLD,
    val overchargeSecondsLeft: Int = 0,
    val feverMeter: Float = 0f,
    val feverSecondsLeft: Int = 0,
    val comboCount: Int = 0,
    val isMidnightBonusActive: Boolean = false,
    val isCometVisible: Boolean = false,
    val cometSeed: Long = 0L,
    val visualDroneCount: Int = 0,
    val selectedSkin: CrystalSkin = CrystalSkin.CLASSIC,
    val selectedLocation: SpaceLocation = SpaceLocation.NEBULA,
    val unlockedAchievementIds: Set<String> = emptySet(),
    val bossState: BossUiState = BossUiState(),
    val expeditionState: ExpeditionUiState = ExpeditionUiState(),
    val talents: List<TalentItemUi> = emptyList(),
    val dailyQuests: List<DailyQuestItemUi> = emptyList(),
    val offlineReward: OfflineReward? = null,
    val availableUpdate: AppUpdateInfo? = null,
    val isCheckingUpdate: Boolean = false,
    val updateStatusMessage: String? = null,
    val upgrades: List<UpgradeItemUi> = emptyList(),
    val achievements: List<AchievementItemUi> = emptyList(),
    val unclaimedAchievementsCount: Int = 0,
    val unclaimedQuestsCount: Int = 0,
    val secretUnlockedToast: String? = null,
    val selectedTab: BottomTab = BottomTab.UPGRADES,
    val selectedBuyQuantity: BuyQuantity = BuyQuantity.X1,
    val isVibrationEnabled: Boolean = true,
    val isSoundEnabled: Boolean = true,
    val isPotatoMode: Boolean = false,
    val isLoaded: Boolean = false
)

object GameCatalog {
    const val MAX_PLAYER_LEVEL = 200
    const val MAX_UPGRADE_LEVEL = 200
    const val PRESTIGE_BASE_THRESHOLD = 100_000.0
    const val OVERCHARGE_DURATION_SEC = 10
    const val OVERCHARGE_MULTIPLIER = 5.0
    const val FEVER_DURATION_SEC = 10
    const val BOSS_DURATION_SEC = 30
    const val BOSS_AUTO_SPAWN_SEC = 600
    const val BOSS_MANUAL_COOLDOWN_SEC = 45
    const val MAX_OFFLINE_SECONDS = 12L * 3600L

    fun calculatePotentialDarkMatter(runCrystalsEarned: Double): Long {
        if (runCrystalsEarned < PRESTIGE_BASE_THRESHOLD) return 0L
        return floor(sqrt(runCrystalsEarned / PRESTIGE_BASE_THRESHOLD)).toLong()
    }

    fun nextDarkMatterRequirement(currentPotential: Long): Double {
        val next = currentPotential + 1L
        return next.toDouble() * next.toDouble() * PRESTIGE_BASE_THRESHOLD
    }

    fun calculateUpgradePurchase(
        definition: UpgradeDefinition,
        currentLevel: Int,
        crystals: Double,
        discountFraction: Double,
        quantity: BuyQuantity
    ): UpgradePurchasePlan {
        val remaining = (MAX_UPGRADE_LEVEL - currentLevel).coerceAtLeast(0)
        if (remaining <= 0) {
            return UpgradePurchasePlan(levelsToBuy = 0, totalCost = 0.0, canAfford = false)
        }
        val discountFactor = 1.0 - discountFraction.coerceIn(0.0, 0.5)
        var raw = definition.baseCost * definition.costMultiplier.pow(currentLevel.toDouble())
        return when (quantity) {
            BuyQuantity.X1 -> {
                val cost = floor(raw * discountFactor).coerceAtLeast(1.0)
                UpgradePurchasePlan(
                    levelsToBuy = 1,
                    totalCost = cost,
                    canAfford = crystals >= cost
                )
            }
            BuyQuantity.X10 -> {
                val target = minOf(10, remaining)
                var sum = 0.0
                for (i in 0 until target) {
                    sum += floor(raw * discountFactor).coerceAtLeast(1.0)
                    raw *= definition.costMultiplier
                }
                UpgradePurchasePlan(
                    levelsToBuy = target,
                    totalCost = sum,
                    canAfford = crystals >= sum
                )
            }
            BuyQuantity.X100 -> {
                val target = minOf(100, remaining)
                var sum = 0.0
                for (i in 0 until target) {
                    sum += floor(raw * discountFactor).coerceAtLeast(1.0)
                    raw *= definition.costMultiplier
                }
                UpgradePurchasePlan(
                    levelsToBuy = target,
                    totalCost = sum,
                    canAfford = crystals >= sum
                )
            }
            BuyQuantity.MAX -> {
                var sum = 0.0
                var count = 0
                for (i in 0 until remaining) {
                    val stepCost = floor(raw * discountFactor).coerceAtLeast(1.0)
                    if (sum + stepCost <= crystals) {
                        sum += stepCost
                        count++
                        raw *= definition.costMultiplier
                    } else {
                        break
                    }
                }
                if (count > 0) {
                    UpgradePurchasePlan(levelsToBuy = count, totalCost = sum, canAfford = true)
                } else {
                    val firstCost = floor(raw * discountFactor).coerceAtLeast(1.0)
                    UpgradePurchasePlan(levelsToBuy = 1, totalCost = firstCost, canAfford = false)
                }
            }
        }
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
        val multiplier = round(rawMult * 100.0) / 100.0
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
            description = "Сделать 1 000 кликов (открывает локацию)",
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
            id = "upgrades_100",
            title = "Архитектор флота",
            description = "Купить 100 улучшений за заход (открывает Кибер-куб)",
            metric = AchievementMetric.TOTAL_UPGRADES_BOUGHT,
            targetValue = 100.0,
            rewardCrystals = 40_000.0
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
            id = "comets_15",
            title = "Звёздный перехватчик",
            description = "Поймать 15 комет (открывает Солнечное ядро)",
            metric = AchievementMetric.COMETS_CAUGHT,
            targetValue = 15.0,
            rewardCrystals = 120_000.0
        ),
        AchievementDefinition(
            id = "fever_5",
            title = "Вспышка сверхновой",
            description = "Включить Космическую ярость 5 раз (открывает Пустоту)",
            metric = AchievementMetric.FEVER_COUNT,
            targetValue = 5.0,
            rewardCrystals = 60_000.0
        ),
        AchievementDefinition(
            id = "boss_1",
            title = "Охотник на Левиафанов",
            description = "Победить босса-астероида (открывает Ледяную реликвию)",
            metric = AchievementMetric.BOSSES_DEFEATED,
            targetValue = 1.0,
            rewardCrystals = 80_000.0
        ),
        AchievementDefinition(
            id = "crystals_1m",
            title = "Миллионер орбиты",
            description = "Добыть 1M кристаллов (открывает Радужную призму)",
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
            description = "Совершить 1-й Коллапс (открывает Чёрную дыру)",
            metric = AchievementMetric.PRESTIGE_COUNT,
            targetValue = 1.0,
            rewardCrystals = 500_000.0
        ),
        AchievementDefinition(
            id = "secret_header_tap",
            title = "Тайна заголовка",
            description = "Секрет: нажать 7 раз на слово «Кристаллы»",
            metric = AchievementMetric.SECRET_HEADER_TAP,
            targetValue = 1.0,
            rewardCrystals = 25_000.0,
            isSecret = true
        )
    )

    val talents: List<TalentDefinition> = listOf(
        TalentDefinition(
            id = "crit_edge",
            title = "Резонансное лезвие",
            description = "+3% к шансу крита x3 за уровень",
            branch = TalentBranch.ACTIVE_CLICKER,
            maxLevel = 5,
            baseCostDm = 1L,
            costStepDm = 1L
        ),
        TalentDefinition(
            id = "fever_surge",
            title = "Ядерный перегрев",
            description = "+25% быстрее нагрев комбо и +1с к Ярости",
            branch = TalentBranch.ACTIVE_CLICKER,
            maxLevel = 5,
            baseCostDm = 2L,
            costStepDm = 1L
        ),
        TalentDefinition(
            id = "comet_magnet",
            title = "Притяжение комет",
            description = "Кометы чаще и приносят +50% кристаллов",
            branch = TalentBranch.ACTIVE_CLICKER,
            maxLevel = 3,
            baseCostDm = 3L,
            costStepDm = 2L
        ),
        TalentDefinition(
            id = "drone_overclock",
            title = "Разгон флота",
            description = "+25% к доходу дронов и станций за уровень",
            branch = TalentBranch.TYCOON,
            maxLevel = 5,
            baseCostDm = 1L,
            costStepDm = 1L
        ),
        TalentDefinition(
            id = "stellar_discount",
            title = "Космические субсидии",
            description = "-5% к цене всех улучшений за уровень",
            branch = TalentBranch.TYCOON,
            maxLevel = 5,
            baseCostDm = 2L,
            costStepDm = 1L
        ),
        TalentDefinition(
            id = "cryo_vault",
            title = "Квантовое хранилище",
            description = "+35% к офлайн-добыче за уровень",
            branch = TalentBranch.TYCOON,
            maxLevel = 3,
            baseCostDm = 3L,
            costStepDm = 2L
        )
    )

    val expeditions: List<ExpeditionPreset> = listOf(
        ExpeditionPreset(
            id = "scout_5",
            title = "Разведка орбиты",
            subtitle = "Быстрая вылазка за обломками",
            durationSeconds = 5L * 60L,
            crystalSecondsEquivalent = 90.0,
            darkMatterChance = 0.25
        ),
        ExpeditionPreset(
            id = "anomaly_15",
            title = "Пояс аномалий",
            subtitle = "Богатая жила и шанс Тёмной материи",
            durationSeconds = 15L * 60L,
            crystalSecondsEquivalent = 320.0,
            darkMatterChance = 0.65
        ),
        ExpeditionPreset(
            id = "deep_sector_30",
            title = "Дальний сектор",
            subtitle = "Крупный груз и гарантированная ТМ",
            durationSeconds = 30L * 60L,
            crystalSecondsEquivalent = 750.0,
            darkMatterChance = 1.0
        )
    )

    val dailyQuests: List<DailyQuestDefinition> = listOf(
        DailyQuestDefinition(
            id = "daily_clicks_300",
            title = "Тапнуть 300 раз",
            type = DailyQuestType.CLICKS,
            target = 300,
            rewardDarkMatter = 1L,
            rewardCrystalsFactor = 40.0
        ),
        DailyQuestDefinition(
            id = "daily_upgrades_5",
            title = "Купить 5 улучшений",
            type = DailyQuestType.UPGRADES,
            target = 5,
            rewardDarkMatter = 1L,
            rewardCrystalsFactor = 55.0
        ),
        DailyQuestDefinition(
            id = "daily_fever_3",
            title = "Включить Ярость 3 раза",
            type = DailyQuestType.FEVER,
            target = 3,
            rewardDarkMatter = 2L,
            rewardCrystalsFactor = 80.0
        )
    )
}
