package com.cosmicminer.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cosmicminer.app.data.GamePreferencesRepository
import com.cosmicminer.app.data.PersistedGameData
import com.cosmicminer.app.model.AchievementItemUi
import com.cosmicminer.app.model.AchievementMetric
import com.cosmicminer.app.model.BottomTab
import com.cosmicminer.app.model.GameCatalog
import com.cosmicminer.app.model.GameUiState
import com.cosmicminer.app.model.MinerRank
import com.cosmicminer.app.model.UpgradeItemUi
import com.cosmicminer.app.model.UpgradeType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GamePreferencesRepository(application.applicationContext)

    private var crystals = 0.0
    private var totalCrystalsEarned = 0.0
    private var experience = 0.0
    private var totalClicks = 0L
    private var criticalHits = 0L
    private var upgradeLevels = mutableMapOf<String, Int>()
    private var claimedAchievements = mutableSetOf<String>()
    private var selectedTab = BottomTab.UPGRADES
    private var isVibrationEnabled = true
    private var isLoaded = false

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = repository.gameDataFlow.first()
            crystals = saved.crystals
            totalCrystalsEarned = saved.totalCrystalsEarned
            experience = saved.experience
            totalClicks = saved.totalClicks
            criticalHits = saved.criticalHits
            upgradeLevels = saved.upgradeLevels.toMutableMap()
            claimedAchievements = saved.claimedAchievements.toMutableSet()
            isVibrationEnabled = saved.isVibrationEnabled
            isLoaded = true
            publishUiState()
        }

        // Smooth passive income loop (10 ticks per second)
        viewModelScope.launch {
            var tickCounter = 0
            while (true) {
                delay(100L)
                if (!isLoaded) continue

                val passive = calculatePassivePerSecond()
                if (passive > 0.0) {
                    val delta = passive / 10.0
                    crystals += delta
                    totalCrystalsEarned += delta
                    experience += delta
                    publishUiState()
                }

                tickCounter++
                if (tickCounter >= 20) {
                    tickCounter = 0
                    persistNow()
                }
            }
        }
    }

    data class ClickResult(
        val gained: Double,
        val isCritical: Boolean
    )

    fun onCrystalClick(): ClickResult {
        val baseClick = calculateClickPower()
        val isCrit = Random.nextFloat() < 0.10f
        val gained = if (isCrit) baseClick * 3.0 else baseClick

        crystals += gained
        totalCrystalsEarned += gained
        experience += gained
        totalClicks += 1L
        if (isCrit) {
            criticalHits += 1L
        }

        publishUiState()
        return ClickResult(gained = gained, isCritical = isCrit)
    }

    fun buyUpgrade(upgradeId: String) {
        val definition = GameCatalog.upgrades.find { it.id == upgradeId } ?: return
        val currentLevel = upgradeLevels[upgradeId] ?: 0
        val cost = definition.costAtLevel(currentLevel)

        if (crystals >= cost) {
            crystals -= cost
            upgradeLevels[upgradeId] = currentLevel + 1
            publishUiState()
            viewModelScope.launch { persistNow() }
        }
    }

    fun claimAchievement(achievementId: String) {
        if (claimedAchievements.contains(achievementId)) return
        val definition = GameCatalog.achievements.find { it.id == achievementId } ?: return

        val currentValue = getMetricValue(definition.metric)
        if (currentValue >= definition.targetValue) {
            claimedAchievements.add(achievementId)
            crystals += definition.rewardCrystals
            totalCrystalsEarned += definition.rewardCrystals
            experience += definition.rewardCrystals
            publishUiState()
            viewModelScope.launch { persistNow() }
        }
    }

    fun selectTab(tab: BottomTab) {
        selectedTab = tab
        publishUiState()
    }

    fun setVibrationEnabled(enabled: Boolean) {
        isVibrationEnabled = enabled
        publishUiState()
        viewModelScope.launch {
            repository.saveVibrationSetting(enabled)
        }
    }

    fun resetProgress() {
        crystals = 0.0
        totalCrystalsEarned = 0.0
        experience = 0.0
        totalClicks = 0L
        criticalHits = 0L
        upgradeLevels.clear()
        claimedAchievements.clear()
        publishUiState()
        viewModelScope.launch {
            repository.resetProgress(keepVibrationSetting = isVibrationEnabled)
        }
    }

    fun saveOnBackground() {
        if (!isLoaded) return
        viewModelScope.launch {
            persistNow()
        }
    }

    private suspend fun persistNow() {
        repository.saveProgress(
            PersistedGameData(
                crystals = crystals,
                totalCrystalsEarned = totalCrystalsEarned,
                experience = experience,
                totalClicks = totalClicks,
                criticalHits = criticalHits,
                upgradeLevels = upgradeLevels.toMap(),
                claimedAchievements = claimedAchievements.toSet(),
                isVibrationEnabled = isVibrationEnabled
            )
        )
    }

    private fun resolveRank(): Pair<MinerRank, MinerRank?> {
        val ranks = GameCatalog.ranks
        var current = ranks.first()
        var next: MinerRank? = ranks.getOrNull(1)

        for (i in ranks.indices) {
            if (experience >= ranks[i].xpThreshold) {
                current = ranks[i]
                next = ranks.getOrNull(i + 1)
            }
        }
        return current to next
    }

    private fun calculateClickPower(): Double {
        val (rank, _) = resolveRank()
        val bonus = GameCatalog.upgrades
            .filter { it.type == UpgradeType.CLICK_POWER }
            .sumOf { (upgradeLevels[it.id] ?: 0) * it.baseGain }
        return (1.0 + bonus) * rank.multiplier
    }

    private fun calculatePassivePerSecond(): Double {
        val (rank, _) = resolveRank()
        val bonus = GameCatalog.upgrades
            .filter { it.type == UpgradeType.PASSIVE_INCOME }
            .sumOf { (upgradeLevels[it.id] ?: 0) * it.baseGain }
        return bonus * rank.multiplier
    }

    private fun getMetricValue(metric: AchievementMetric): Double {
        return when (metric) {
            AchievementMetric.TOTAL_CLICKS -> totalClicks.toDouble()
            AchievementMetric.TOTAL_CRYSTALS_EARNED -> totalCrystalsEarned
            AchievementMetric.TOTAL_UPGRADES_BOUGHT -> upgradeLevels.values.sum().toDouble()
            AchievementMetric.PASSIVE_PER_SEC -> calculatePassivePerSecond()
            AchievementMetric.CRITICAL_HITS -> criticalHits.toDouble()
        }
    }

    private fun publishUiState() {
        val (currentRank, nextRank) = resolveRank()
        val rankProgress = if (nextRank == null) {
            1f
        } else {
            val span = (nextRank.xpThreshold - currentRank.xpThreshold).coerceAtLeast(1.0)
            ((experience - currentRank.xpThreshold) / span).toFloat().coerceIn(0f, 1f)
        }

        val upgradeItems = GameCatalog.upgrades.map { def ->
            val level = upgradeLevels[def.id] ?: 0
            val cost = def.costAtLevel(level)
            UpgradeItemUi(
                definition = def,
                level = level,
                nextCost = cost,
                canAfford = crystals >= cost
            )
        }

        val achievementItems = GameCatalog.achievements.map { def ->
            val currentVal = getMetricValue(def.metric)
            val progress = (currentVal / def.targetValue).toFloat().coerceIn(0f, 1f)
            val unlocked = currentVal >= def.targetValue
            val claimed = claimedAchievements.contains(def.id)
            AchievementItemUi(
                definition = def,
                currentValue = currentVal,
                progress = progress,
                isUnlocked = unlocked,
                isClaimed = claimed
            )
        }

        _uiState.value = GameUiState(
            crystals = crystals,
            totalCrystalsEarned = totalCrystalsEarned,
            experience = experience,
            totalClicks = totalClicks,
            criticalHits = criticalHits,
            clickPower = calculateClickPower(),
            passivePerSecond = calculatePassivePerSecond(),
            currentRank = currentRank,
            nextRank = nextRank,
            rankProgress = rankProgress,
            upgrades = upgradeItems,
            achievements = achievementItems,
            unclaimedAchievementsCount = achievementItems.count { it.isUnlocked && !it.isClaimed },
            selectedTab = selectedTab,
            isVibrationEnabled = isVibrationEnabled,
            isLoaded = isLoaded
        )
    }
}
