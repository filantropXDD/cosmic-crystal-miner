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
import com.cosmicminer.app.model.OfflineReward
import com.cosmicminer.app.model.UpgradeItemUi
import com.cosmicminer.app.model.UpgradeType
import com.cosmicminer.app.util.AppUpdateInfo
import com.cosmicminer.app.util.BackupCodec
import com.cosmicminer.app.util.SoundEffects
import com.cosmicminer.app.util.UpdateCheckResult
import com.cosmicminer.app.util.UpdateChecker
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
    private var runCrystalsEarned = 0.0
    private var totalCrystalsEarned = 0.0
    private var experience = 0.0
    private var totalClicks = 0L
    private var criticalHits = 0L
    private var cometsCaught = 0L
    private var darkMatter = 0L
    private var prestigeCount = 0
    private var upgradeLevels = mutableMapOf<String, Int>()
    private var claimedAchievements = mutableSetOf<String>()
    private var selectedTab = BottomTab.UPGRADES
    private var isVibrationEnabled = true
    private var isSoundEnabled = true
    private var isLoaded = false

    private var overchargeTicksRemaining = 0
    private var isCometVisible = false
    private var cometTicksRemaining = 0
    private var nextCometDelayTicks = 350
    private var cometSeed = 0L
    private var offlineReward: OfflineReward? = null

    private var availableUpdate: AppUpdateInfo? = null
    private var isCheckingUpdate = false
    private var updateStatusMessage: String? = null

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = repository.gameDataFlow.first()
            applyPersistedData(saved)

            // Calculate offline income
            val nowMs = System.currentTimeMillis()
            if (saved.lastActiveTimestampMs > 0L) {
                val elapsedSec = ((nowMs - saved.lastActiveTimestampMs) / 1000L)
                    .coerceIn(0L, GameCatalog.MAX_OFFLINE_SECONDS)
                val passive = calculatePassivePerSecond()
                if (elapsedSec >= 10L && passive > 0.0) {
                    val earned = passive * elapsedSec.toDouble()
                    crystals += earned
                    runCrystalsEarned += earned
                    totalCrystalsEarned += earned
                    experience += earned
                    offlineReward = OfflineReward(
                        elapsedSeconds = elapsedSec,
                        crystalsEarned = earned
                    )
                }
            }

            isLoaded = true
            publishUiState()
            persistNow()

            // Silent update check on startup
            when (val result = UpdateChecker.checkForUpdate()) {
                is UpdateCheckResult.UpdateAvailable -> {
                    availableUpdate = result.info
                    publishUiState()
                }
                else -> Unit
            }
        }

        // Main game loop (10 ticks per second)
        viewModelScope.launch {
            var saveTickCounter = 0
            while (true) {
                delay(100L)
                if (!isLoaded) continue

                var needsPublish = false

                val passive = calculatePassivePerSecond()
                if (passive > 0.0) {
                    val delta = passive / 10.0
                    crystals += delta
                    runCrystalsEarned += delta
                    totalCrystalsEarned += delta
                    experience += delta
                    needsPublish = true
                }

                if (overchargeTicksRemaining > 0) {
                    val prevSec = (overchargeTicksRemaining + 9) / 10
                    overchargeTicksRemaining--
                    val newSec = (overchargeTicksRemaining + 9) / 10
                    if (prevSec != newSec || overchargeTicksRemaining == 0) {
                        needsPublish = true
                    }
                }

                if (isCometVisible) {
                    cometTicksRemaining--
                    if (cometTicksRemaining <= 0) {
                        isCometVisible = false
                        nextCometDelayTicks = Random.nextInt(400, 600)
                        needsPublish = true
                    }
                } else {
                    nextCometDelayTicks--
                    if (nextCometDelayTicks <= 0) {
                        isCometVisible = true
                        cometTicksRemaining = 65
                        cometSeed = System.currentTimeMillis()
                        needsPublish = true
                    }
                }

                if (needsPublish) {
                    publishUiState()
                }

                saveTickCounter++
                if (saveTickCounter >= 20) {
                    saveTickCounter = 0
                    persistNow()
                }
            }
        }
    }

    private fun applyPersistedData(saved: PersistedGameData) {
        crystals = saved.crystals
        runCrystalsEarned = saved.runCrystalsEarned
        totalCrystalsEarned = saved.totalCrystalsEarned
        experience = saved.experience
        totalClicks = saved.totalClicks
        criticalHits = saved.criticalHits
        cometsCaught = saved.cometsCaught
        darkMatter = saved.darkMatter
        prestigeCount = saved.prestigeCount
        upgradeLevels = saved.upgradeLevels.toMutableMap()
        claimedAchievements = saved.claimedAchievements.toMutableSet()
        isVibrationEnabled = saved.isVibrationEnabled
        isSoundEnabled = saved.isSoundEnabled
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
        runCrystalsEarned += gained
        totalCrystalsEarned += gained
        experience += gained
        totalClicks += 1L
        if (isCrit) {
            criticalHits += 1L
        }

        if (isSoundEnabled) {
            SoundEffects.playTap(isCrit)
        }

        publishUiState()
        return ClickResult(gained = gained, isCritical = isCrit)
    }

    fun catchComet(): Double {
        if (!isCometVisible) return 0.0
        isCometVisible = false
        nextCometDelayTicks = Random.nextInt(400, 600)
        cometsCaught += 1L
        overchargeTicksRemaining = GameCatalog.OVERCHARGE_DURATION_SEC * 10

        val bonus = maxOf(
            calculateClickPowerWithoutOvercharge() * 25.0,
            calculatePassivePerSecond() * 20.0,
            100.0
        )
        crystals += bonus
        runCrystalsEarned += bonus
        totalCrystalsEarned += bonus
        experience += bonus

        if (isSoundEnabled) {
            SoundEffects.playComet()
        }

        publishUiState()
        viewModelScope.launch { persistNow() }
        return bonus
    }

    fun dismissOfflineReward() {
        offlineReward = null
        publishUiState()
    }

    fun checkForUpdatesManual() {
        if (isCheckingUpdate) return
        isCheckingUpdate = true
        updateStatusMessage = null
        publishUiState()

        viewModelScope.launch {
            when (val result = UpdateChecker.checkForUpdate()) {
                is UpdateCheckResult.UpdateAvailable -> {
                    availableUpdate = result.info
                    updateStatusMessage = "Найдена версия ${result.info.versionTag}"
                }
                is UpdateCheckResult.UpToDate -> {
                    updateStatusMessage = "Установлена последняя версия (${UpdateChecker.CURRENT_VERSION})"
                }
                is UpdateCheckResult.Error -> {
                    updateStatusMessage = result.message
                }
            }
            isCheckingUpdate = false
            publishUiState()
        }
    }

    fun dismissUpdateDialog() {
        availableUpdate = null
        publishUiState()
    }

    fun clearUpdateStatus() {
        updateStatusMessage = null
        publishUiState()
    }

    fun exportBackupCode(): String {
        return BackupCodec.encode(snapshotData())
    }

    fun importBackupCode(rawCode: String): Boolean {
        val decoded = BackupCodec.decode(rawCode) ?: return false
        applyPersistedData(decoded)
        offlineReward = null
        overchargeTicksRemaining = 0
        publishUiState()
        viewModelScope.launch { persistNow() }
        return true
    }

    fun buyUpgrade(upgradeId: String) {
        val definition = GameCatalog.upgrades.find { it.id == upgradeId } ?: return
        val currentLevel = upgradeLevels[upgradeId] ?: 0
        val cost = definition.costAtLevel(currentLevel)

        if (crystals >= cost) {
            crystals -= cost
            upgradeLevels[upgradeId] = currentLevel + 1
            if (isSoundEnabled) {
                SoundEffects.playPurchase()
            }
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
            runCrystalsEarned += definition.rewardCrystals
            totalCrystalsEarned += definition.rewardCrystals
            experience += definition.rewardCrystals
            if (isSoundEnabled) {
                SoundEffects.playPurchase()
            }
            publishUiState()
            viewModelScope.launch { persistNow() }
        }
    }

    fun performCollapse() {
        val gainedDarkMatter = GameCatalog.calculatePotentialDarkMatter(runCrystalsEarned)
        if (gainedDarkMatter <= 0L) return

        darkMatter += gainedDarkMatter
        prestigeCount += 1
        crystals = 0.0
        runCrystalsEarned = 0.0
        experience = 0.0
        overchargeTicksRemaining = 0
        upgradeLevels.clear()

        if (isSoundEnabled) {
            SoundEffects.playPrestige()
        }

        selectedTab = BottomTab.UPGRADES
        publishUiState()
        viewModelScope.launch { persistNow() }
    }

    fun selectTab(tab: BottomTab) {
        selectedTab = tab
        publishUiState()
    }

    fun setVibrationEnabled(enabled: Boolean) {
        isVibrationEnabled = enabled
        publishUiState()
        viewModelScope.launch {
            repository.saveSettings(isVibrationEnabled, isSoundEnabled)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        isSoundEnabled = enabled
        publishUiState()
        viewModelScope.launch {
            repository.saveSettings(isVibrationEnabled, isSoundEnabled)
        }
    }

    fun resetProgress() {
        crystals = 0.0
        runCrystalsEarned = 0.0
        totalCrystalsEarned = 0.0
        experience = 0.0
        totalClicks = 0L
        criticalHits = 0L
        cometsCaught = 0L
        darkMatter = 0L
        prestigeCount = 0
        overchargeTicksRemaining = 0
        offlineReward = null
        upgradeLevels.clear()
        claimedAchievements.clear()
        publishUiState()
        viewModelScope.launch {
            repository.resetProgress(keepVibration = isVibrationEnabled, keepSound = isSoundEnabled)
        }
    }

    fun saveOnBackground() {
        if (!isLoaded) return
        viewModelScope.launch {
            persistNow()
        }
    }

    private fun snapshotData(): PersistedGameData {
        return PersistedGameData(
            crystals = crystals,
            runCrystalsEarned = runCrystalsEarned,
            totalCrystalsEarned = totalCrystalsEarned,
            experience = experience,
            totalClicks = totalClicks,
            criticalHits = criticalHits,
            cometsCaught = cometsCaught,
            darkMatter = darkMatter,
            prestigeCount = prestigeCount,
            upgradeLevels = upgradeLevels.toMap(),
            claimedAchievements = claimedAchievements.toSet(),
            isVibrationEnabled = isVibrationEnabled,
            isSoundEnabled = isSoundEnabled,
            lastActiveTimestampMs = System.currentTimeMillis()
        )
    }

    private suspend fun persistNow() {
        repository.saveProgress(snapshotData())
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

    private fun prestigeMultiplier(): Double = 1.0 + darkMatter * 0.5

    private fun calculateClickPowerWithoutOvercharge(): Double {
        val (rank, _) = resolveRank()
        val bonus = GameCatalog.upgrades
            .filter { it.type == UpgradeType.CLICK_POWER }
            .sumOf { (upgradeLevels[it.id] ?: 0) * it.baseGain }
        return (1.0 + bonus) * rank.multiplier * prestigeMultiplier()
    }

    private fun calculateClickPower(): Double {
        val overcharge = if (overchargeTicksRemaining > 0) GameCatalog.OVERCHARGE_MULTIPLIER else 1.0
        return calculateClickPowerWithoutOvercharge() * overcharge
    }

    private fun calculatePassivePerSecond(): Double {
        val (rank, _) = resolveRank()
        val bonus = GameCatalog.upgrades
            .filter { it.type == UpgradeType.PASSIVE_INCOME }
            .sumOf { (upgradeLevels[it.id] ?: 0) * it.baseGain }
        return bonus * rank.multiplier * prestigeMultiplier()
    }

    private fun getMetricValue(metric: AchievementMetric): Double {
        return when (metric) {
            AchievementMetric.TOTAL_CLICKS -> totalClicks.toDouble()
            AchievementMetric.TOTAL_CRYSTALS_EARNED -> totalCrystalsEarned
            AchievementMetric.TOTAL_UPGRADES_BOUGHT -> upgradeLevels.values.sum().toDouble()
            AchievementMetric.PASSIVE_PER_SEC -> calculatePassivePerSecond()
            AchievementMetric.CRITICAL_HITS -> criticalHits.toDouble()
            AchievementMetric.COMETS_CAUGHT -> cometsCaught.toDouble()
            AchievementMetric.PRESTIGE_COUNT -> prestigeCount.toDouble()
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

        val potentialDm = GameCatalog.calculatePotentialDarkMatter(runCrystalsEarned)

        _uiState.value = GameUiState(
            crystals = crystals,
            runCrystalsEarned = runCrystalsEarned,
            totalCrystalsEarned = totalCrystalsEarned,
            experience = experience,
            totalClicks = totalClicks,
            criticalHits = criticalHits,
            cometsCaught = cometsCaught,
            clickPower = calculateClickPower(),
            passivePerSecond = calculatePassivePerSecond(),
            currentRank = currentRank,
            nextRank = nextRank,
            rankProgress = rankProgress,
            darkMatter = darkMatter,
            prestigeCount = prestigeCount,
            potentialDarkMatter = potentialDm,
            prestigeMultiplier = prestigeMultiplier(),
            nextDarkMatterThreshold = GameCatalog.nextDarkMatterRequirement(potentialDm),
            overchargeSecondsLeft = (overchargeTicksRemaining + 9) / 10,
            isCometVisible = isCometVisible,
            cometSeed = cometSeed,
            offlineReward = offlineReward,
            availableUpdate = availableUpdate,
            isCheckingUpdate = isCheckingUpdate,
            updateStatusMessage = updateStatusMessage,
            upgrades = upgradeItems,
            achievements = achievementItems,
            unclaimedAchievementsCount = achievementItems.count { it.isUnlocked && !it.isClaimed },
            selectedTab = selectedTab,
            isVibrationEnabled = isVibrationEnabled,
            isSoundEnabled = isSoundEnabled,
            isLoaded = isLoaded
        )
    }
}
