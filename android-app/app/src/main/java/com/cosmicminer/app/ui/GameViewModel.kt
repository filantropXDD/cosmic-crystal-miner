package com.cosmicminer.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cosmicminer.app.data.GamePreferencesRepository
import com.cosmicminer.app.data.PersistedGameData
import com.cosmicminer.app.model.AchievementItemUi
import com.cosmicminer.app.model.AchievementMetric
import com.cosmicminer.app.model.BossUiState
import com.cosmicminer.app.model.BottomTab
import com.cosmicminer.app.model.BuyQuantity
import com.cosmicminer.app.model.CrystalSkin
import com.cosmicminer.app.model.DailyQuestItemUi
import com.cosmicminer.app.model.DailyQuestType
import com.cosmicminer.app.model.ExpeditionUiState
import com.cosmicminer.app.model.GameCatalog
import com.cosmicminer.app.model.GameUiState
import com.cosmicminer.app.model.MinerRank
import com.cosmicminer.app.model.OfflineReward
import com.cosmicminer.app.model.SpaceLocation
import com.cosmicminer.app.model.TalentItemUi
import com.cosmicminer.app.model.UpgradeItemUi
import com.cosmicminer.app.model.UpgradeType
import com.cosmicminer.app.util.AppUpdateInfo
import com.cosmicminer.app.util.BackupCodec
import com.cosmicminer.app.util.NumberFormatter
import com.cosmicminer.app.util.SoundEffects
import com.cosmicminer.app.util.UpdateCheckResult
import com.cosmicminer.app.util.UpdateChecker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar
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
    private var bossesDefeated = 0L
    private var feverActivations = 0L
    private var bestCps = 0
    private var playTimeSeconds = 0L
    private var darkMatter = 0L
    private var spentDarkMatter = 0L
    private var prestigeCount = 0
    private var upgradeLevels = mutableMapOf<String, Int>()
    private var cachedClickBonus = 0.0
    private var cachedPassiveBonus = 0.0
    private var cachedTotalUpgradesBought = 0
    private var cachedVisualDroneCount = 0
    private var talentLevels = mutableMapOf<String, Int>()
    private var claimedAchievements = mutableSetOf<String>()
    private var secretHeaderUnlocked = false
    private var headerTapCounter = 0
    private var selectedSkin = CrystalSkin.CLASSIC
    private var selectedLocation = SpaceLocation.NEBULA
    private var activeExpeditionId: String? = null
    private var expeditionEndMs = 0L
    private var expeditionLastMessage: String? = null

    private var dailyDateKey = ""
    private var dailyClicks = 0
    private var dailyUpgrades = 0
    private var dailyFever = 0
    private var dailyClaimedIds = mutableSetOf<String>()

    private var selectedTab = BottomTab.UPGRADES
    private var selectedBuyQuantity = BuyQuantity.X1
    private var isVibrationEnabled = true
    private var isSoundEnabled = true
    private var isPotatoMode = false
    private var isLoaded = false
    private var hasDirtyProgress = false
    private var lastHeavyPublishMs = 0L

    private var overchargeTicksRemaining = 0
    private var isCometVisible = false
    private var cometTicksRemaining = 0
    private var nextCometDelayTicks = 320
    private var cometSeed = 0L

    // Fever / Combo mode
    private var feverMeter = 0f
    private var feverTicksRemaining = 0
    private var comboCount = 0
    private var ticksSinceLastClick = 0

    // Boss battle
    private var isBossActive = false
    private var bossName = "Бронированный Левиафан"
    private var bossCurrentHp = 0.0
    private var bossMaxHp = 1.0
    private var bossTicksRemaining = 0
    private var bossCooldownTicks = 0
    private var bossAutoSpawnTicks = GameCatalog.BOSS_AUTO_SPAWN_SEC * 10
    private var bossLastResult: String? = null

    private val recentClickTimestamps = ArrayDeque<Long>()
    private var secretToast: String? = null
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
            syncDailyQuestDate()

            // Calculate offline income with cryo_vault talent bonus
            val nowMs = System.currentTimeMillis()
            if (saved.lastActiveTimestampMs > 0L) {
                val elapsedSec = ((nowMs - saved.lastActiveTimestampMs) / 1000L)
                    .coerceIn(0L, GameCatalog.MAX_OFFLINE_SECONDS)
                val offlineBonusMult = 1.0 + (talentLevels["cryo_vault"] ?: 0) * 0.35
                val passive = calculatePassivePerSecond() * offlineBonusMult
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

            when (val result = UpdateChecker.checkForUpdate()) {
                is UpdateCheckResult.UpdateAvailable -> {
                    availableUpdate = result.info
                    publishUiState()
                }
                else -> Unit
            }
        }

        // Main game loop (10 logic ticks/sec, throttled UI publishing for passive ticks)
        viewModelScope.launch {
            var saveTickCounter = 0
            var secondTickCounter = 0
            var passivePublishToggle = 0
            while (true) {
                delay(100L)
                if (!isLoaded) continue

                var needsPublish = false
                var passiveOnly = false
                ticksSinceLastClick++
                secondTickCounter++
                passivePublishToggle++

                // Prune CPS window every tick
                val nowMs = System.currentTimeMillis()
                while (recentClickTimestamps.isNotEmpty() && nowMs - recentClickTimestamps.first() > 1000L) {
                    recentClickTimestamps.removeFirst()
                    needsPublish = true
                }

                // Passive income & boss passive damage
                val passive = calculatePassivePerSecond()
                if (passive > 0.0) {
                    val delta = passive / 10.0
                    crystals += delta
                    runCrystalsEarned += delta
                    totalCrystalsEarned += delta
                    experience += delta
                    hasDirtyProgress = true
                    if (isBossActive) {
                        damageBoss(delta)
                    }
                    val uiPublishMod = if (isPotatoMode) 4 else 2
                    if (passivePublishToggle % uiPublishMod == 0) {
                        needsPublish = true
                        passiveOnly = true
                    }
                }

                // Overcharge countdown
                if (overchargeTicksRemaining > 0) {
                    val prevSec = (overchargeTicksRemaining + 9) / 10
                    overchargeTicksRemaining--
                    val newSec = (overchargeTicksRemaining + 9) / 10
                    if (prevSec != newSec || overchargeTicksRemaining == 0) {
                        needsPublish = true
                        passiveOnly = false
                    }
                }

                // Fever countdown or heat decay
                if (feverTicksRemaining > 0) {
                    val prevSec = (feverTicksRemaining + 9) / 10
                    feverTicksRemaining--
                    feverMeter = (feverTicksRemaining / (feverDurationSec() * 10f)).coerceIn(0f, 1f)
                    val newSec = (feverTicksRemaining + 9) / 10
                    if (prevSec != newSec || feverTicksRemaining == 0) {
                        if (feverTicksRemaining == 0) {
                            comboCount = 0
                        }
                        needsPublish = true
                        passiveOnly = false
                    }
                } else if (feverMeter > 0f && ticksSinceLastClick > 16) {
                    feverMeter = (feverMeter - 0.045f).coerceAtLeast(0f)
                    if (feverMeter == 0f) comboCount = 0
                    needsPublish = true
                    passiveOnly = false
                }

                // Comet lifecycle
                if (isCometVisible) {
                    cometTicksRemaining--
                    if (cometTicksRemaining <= 0) {
                        isCometVisible = false
                        nextCometDelayTicks = nextCometDelay()
                        needsPublish = true
                        passiveOnly = false
                    }
                } else {
                    nextCometDelayTicks--
                    if (nextCometDelayTicks <= 0) {
                        isCometVisible = true
                        cometTicksRemaining = 65
                        cometSeed = nowMs
                        needsPublish = true
                        passiveOnly = false
                    }
                }

                // Boss battle timer or auto-spawn
                if (isBossActive) {
                    val prevSec = (bossTicksRemaining + 9) / 10
                    bossTicksRemaining--
                    val newSec = (bossTicksRemaining + 9) / 10
                    if (bossTicksRemaining <= 0) {
                        isBossActive = false
                        bossCooldownTicks = GameCatalog.BOSS_MANUAL_COOLDOWN_SEC * 10
                        bossAutoSpawnTicks = GameCatalog.BOSS_AUTO_SPAWN_SEC * 10
                        bossLastResult = "Босс улетел в гиперпространство"
                        needsPublish = true
                        passiveOnly = false
                    } else if (prevSec != newSec) {
                        needsPublish = true
                        passiveOnly = false
                    }
                } else {
                    if (bossCooldownTicks > 0) {
                        val prevSec = (bossCooldownTicks + 9) / 10
                        bossCooldownTicks--
                        val newSec = (bossCooldownTicks + 9) / 10
                        if (prevSec != newSec || bossCooldownTicks == 0) {
                            needsPublish = true
                            passiveOnly = false
                        }
                    }
                    bossAutoSpawnTicks--
                    if (bossAutoSpawnTicks <= 0) {
                        spawnBossInternal()
                        needsPublish = true
                        passiveOnly = false
                    }
                }

                // 1-second timer updates (playTime, expedition countdown, midnight/day check)
                if (secondTickCounter >= 10) {
                    secondTickCounter = 0
                    playTimeSeconds += 1L
                    syncDailyQuestDate()
                    needsPublish = true
                    passiveOnly = false
                }

                if (needsPublish) {
                    publishUiState(lightweight = passiveOnly)
                }

                saveTickCounter++
                if (saveTickCounter >= 30) {
                    saveTickCounter = 0
                    if (hasDirtyProgress) {
                        persistNow()
                    }
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
        bossesDefeated = saved.bossesDefeated
        feverActivations = saved.feverActivations
        bestCps = saved.bestCps
        playTimeSeconds = saved.playTimeSeconds
        darkMatter = saved.darkMatter
        spentDarkMatter = saved.spentDarkMatter
        prestigeCount = saved.prestigeCount
        upgradeLevels = saved.upgradeLevels.toMutableMap()
        talentLevels = saved.talentLevels.toMutableMap()
        claimedAchievements = saved.claimedAchievements.toMutableSet()
        secretHeaderUnlocked = saved.secretHeaderUnlocked
        selectedSkin = CrystalSkin.fromId(saved.selectedSkinId)
        selectedLocation = SpaceLocation.fromId(saved.selectedLocationId)
        activeExpeditionId = saved.activeExpeditionId
        expeditionEndMs = saved.expeditionEndMs
        dailyDateKey = saved.dailyDateKey
        dailyClicks = saved.dailyClicks
        dailyUpgrades = saved.dailyUpgrades
        dailyFever = saved.dailyFever
        dailyClaimedIds = saved.dailyClaimedIds.toMutableSet()
        isVibrationEnabled = saved.isVibrationEnabled
        isSoundEnabled = saved.isSoundEnabled
        isPotatoMode = saved.isPotatoMode
        recomputeUpgradeSums()
    }

    private fun recomputeUpgradeSums() {
        var clickSum = 0.0
        var passiveSum = 0.0
        var totalLevels = 0
        var passiveLevels = 0
        for (def in GameCatalog.upgrades) {
            val lvl = upgradeLevels[def.id] ?: 0
            totalLevels += lvl
            if (def.type == UpgradeType.CLICK_POWER) {
                clickSum += lvl * def.baseGain
            } else {
                passiveSum += lvl * def.baseGain
                passiveLevels += lvl
            }
        }
        cachedClickBonus = clickSum
        cachedPassiveBonus = passiveSum
        cachedTotalUpgradesBought = totalLevels
        cachedVisualDroneCount = if (passiveLevels <= 0) 0 else minOf(10, 1 + passiveLevels / 3)
    }

    private fun syncDailyQuestDate() {
        val today = LocalDate.now().toString()
        if (dailyDateKey != today) {
            dailyDateKey = today
            dailyClicks = 0
            dailyUpgrades = 0
            dailyFever = 0
            dailyClaimedIds.clear()
        }
    }

    data class ClickResult(
        val gained: Double,
        val isCritical: Boolean,
        val isFever: Boolean
    )

    fun onCrystalClick(): ClickResult {
        val nowMs = System.currentTimeMillis()
        recentClickTimestamps.addLast(nowMs)
        while (recentClickTimestamps.isNotEmpty() && nowMs - recentClickTimestamps.first() > 1000L) {
            recentClickTimestamps.removeFirst()
        }
        val cps = recentClickTimestamps.size
        if (cps > bestCps) bestCps = cps

        ticksSinceLastClick = 0
        comboCount += 1

        // Fever fill & trigger
        val isFeverNow = feverTicksRemaining > 0
        if (!isFeverNow) {
            val heatPerClick = 0.045f * (1f + (talentLevels["fever_surge"] ?: 0) * 0.25f)
            feverMeter = (feverMeter + heatPerClick).coerceAtMost(1f)
            if (feverMeter >= 1f) {
                feverTicksRemaining = feverDurationSec() * 10
                feverActivations += 1L
                dailyFever += 1
                if (isSoundEnabled) {
                    SoundEffects.playFeverStart()
                }
            }
        }

        val activeFever = feverTicksRemaining > 0
        val critChance = 0.10f + (talentLevels["crit_edge"] ?: 0) * 0.03f
        val isCrit = activeFever || Random.nextFloat() < critChance
        val baseClick = calculateClickPower()
        val gained = if (isCrit) baseClick * 3.0 else baseClick

        crystals += gained
        runCrystalsEarned += gained
        totalCrystalsEarned += gained
        experience += gained
        totalClicks += 1L
        dailyClicks += 1
        hasDirtyProgress = true
        if (isCrit) {
            criticalHits += 1L
        }

        if (isBossActive) {
            damageBoss(gained)
        }

        if (isSoundEnabled) {
            SoundEffects.playTap(isCritical = isCrit, isFever = activeFever)
        }

        publishUiState(lightweight = true)
        return ClickResult(gained = gained, isCritical = isCrit, isFever = activeFever)
    }

    fun catchComet(): Double {
        if (!isCometVisible) return 0.0
        isCometVisible = false
        nextCometDelayTicks = nextCometDelay()
        cometsCaught += 1L
        overchargeTicksRemaining = GameCatalog.OVERCHARGE_DURATION_SEC * 10

        val cometTalentMult = 1.0 + (talentLevels["comet_magnet"] ?: 0) * 0.5
        val bonus = maxOf(
            calculateClickPowerWithoutOvercharge() * 25.0,
            calculatePassivePerSecond() * 20.0,
            100.0
        ) * cometTalentMult

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

    fun summonBoss() {
        if (isBossActive || bossCooldownTicks > 0) return
        spawnBossInternal()
        publishUiState()
    }

    private fun spawnBossInternal() {
        val bossNames = listOf(
            "Бронированный Левиафан",
            "Кварцевый Дредноут",
            "Титан Пояса Койпера",
            "Осколок Сверхновой"
        )
        bossName = bossNames.random()
        val expectedDps = calculateClickPowerWithoutOvercharge() * 6.5 + calculatePassivePerSecond()
        bossMaxHp = maxOf(600.0, expectedDps * 18.0)
        bossCurrentHp = bossMaxHp
        isBossActive = true
        bossTicksRemaining = GameCatalog.BOSS_DURATION_SEC * 10
        bossLastResult = null
    }

    private fun damageBoss(damage: Double) {
        if (!isBossActive) return
        bossCurrentHp = (bossCurrentHp - damage).coerceAtLeast(0.0)
        if (bossCurrentHp <= 0.0) {
            isBossActive = false
            bossesDefeated += 1L
            bossCooldownTicks = GameCatalog.BOSS_MANUAL_COOLDOWN_SEC * 10
            bossAutoSpawnTicks = GameCatalog.BOSS_AUTO_SPAWN_SEC * 10

            val crystalChest = maxOf(
                bossMaxHp * 2.2,
                calculatePassivePerSecond() * 45.0,
                500.0
            )
            val dmBonus = if (Random.nextFloat() < 0.45f || bossesDefeated == 1L) 1L else 0L
            crystals += crystalChest
            runCrystalsEarned += crystalChest
            totalCrystalsEarned += crystalChest
            experience += crystalChest
            darkMatter += dmBonus

            bossLastResult = if (dmBonus > 0L) {
                "Победа! Сундук: +${NumberFormatter.format(crystalChest)} крист. и +$dmBonus ТМ"
            } else {
                "Победа! Сундук: +${NumberFormatter.format(crystalChest)} крист."
            }
            if (isSoundEnabled) {
                SoundEffects.playBossDefeated()
            }
            viewModelScope.launch { persistNow() }
        }
    }

    fun startExpedition(presetId: String) {
        if (activeExpeditionId != null) return
        val preset = GameCatalog.expeditions.find { it.id == presetId } ?: return
        activeExpeditionId = preset.id
        expeditionEndMs = System.currentTimeMillis() + preset.durationSeconds * 1000L
        expeditionLastMessage = null
        if (isSoundEnabled) SoundEffects.playPurchase()
        publishUiState()
        viewModelScope.launch { persistNow() }
    }

    fun claimExpedition() {
        val presetId = activeExpeditionId ?: return
        val preset = GameCatalog.expeditions.find { it.id == presetId } ?: return
        if (System.currentTimeMillis() < expeditionEndMs) return

        val rewardCrystals = maxOf(
            calculatePassivePerSecond() * preset.crystalSecondsEquivalent,
            calculateClickPowerWithoutOvercharge() * 35.0,
            250.0
        )
        val dmFound = if (Random.nextDouble() < preset.darkMatterChance) 1L else 0L

        crystals += rewardCrystals
        runCrystalsEarned += rewardCrystals
        totalCrystalsEarned += rewardCrystals
        experience += rewardCrystals
        darkMatter += dmFound

        if (preset.id == "deep_sector_30") {
            overchargeTicksRemaining = maxOf(overchargeTicksRemaining, 150)
        }

        expeditionLastMessage = if (dmFound > 0L) {
            "Добыто +${NumberFormatter.format(rewardCrystals)} крист. и +$dmFound ТМ!"
        } else {
            "Флот вернулся: +${NumberFormatter.format(rewardCrystals)} крист."
        }
        activeExpeditionId = null
        expeditionEndMs = 0L

        if (isSoundEnabled) SoundEffects.playComet()
        publishUiState()
        viewModelScope.launch { persistNow() }
    }

    fun claimDailyQuest(questId: String) {
        syncDailyQuestDate()
        if (dailyClaimedIds.contains(questId)) return
        val def = GameCatalog.dailyQuests.find { it.id == questId } ?: return
        val progress = dailyProgressFor(def.type)
        if (progress < def.target) return

        dailyClaimedIds.add(questId)
        val bonusCrystals = maxOf(
            calculatePassivePerSecond() * def.rewardCrystalsFactor,
            calculateClickPowerWithoutOvercharge() * 20.0,
            200.0
        )
        darkMatter += def.rewardDarkMatter
        crystals += bonusCrystals
        runCrystalsEarned += bonusCrystals
        totalCrystalsEarned += bonusCrystals
        experience += bonusCrystals

        if (isSoundEnabled) SoundEffects.playPurchase()
        publishUiState()
        viewModelScope.launch { persistNow() }
    }

    fun buyTalent(talentId: String) {
        val def = GameCatalog.talents.find { it.id == talentId } ?: return
        val level = talentLevels[talentId] ?: 0
        if (level >= def.maxLevel) return
        val cost = def.costAtLevel(level)
        if (darkMatter < cost) return

        darkMatter -= cost
        spentDarkMatter += cost
        talentLevels[talentId] = level + 1

        if (isSoundEnabled) SoundEffects.playPurchase()
        publishUiState()
        viewModelScope.launch { persistNow() }
    }

    fun selectSkin(skin: CrystalSkin) {
        val unlockedIds = computeUnlockedAchievementIds()
        if (skin.requiredAchievementId != null && !unlockedIds.contains(skin.requiredAchievementId)) return
        selectedSkin = skin
        if (isSoundEnabled) SoundEffects.playPurchase()
        publishUiState()
        viewModelScope.launch { persistNow() }
    }

    fun selectLocation(location: SpaceLocation) {
        val unlockedIds = computeUnlockedAchievementIds()
        if (location.requiredAchievementId != null && !unlockedIds.contains(location.requiredAchievementId)) return
        selectedLocation = location
        if (isSoundEnabled) SoundEffects.playPurchase()
        publishUiState()
        viewModelScope.launch { persistNow() }
    }

    fun onSecretHeaderTap() {
        if (secretHeaderUnlocked) return
        headerTapCounter += 1
        if (headerTapCounter >= 7) {
            secretHeaderUnlocked = true
            secretToast = "Секретное достижение «Тайна заголовка» открыто!"
            if (isSoundEnabled) SoundEffects.playComet()
            publishUiState()
            viewModelScope.launch { persistNow() }
        }
    }

    fun dismissSecretToast() {
        secretToast = null
        publishUiState()
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

    fun exportBackupCode(): String = BackupCodec.encode(snapshotData())

    fun importBackupCode(rawCode: String): Boolean {
        val decoded = BackupCodec.decode(rawCode) ?: return false
        applyPersistedData(decoded)
        offlineReward = null
        overchargeTicksRemaining = 0
        feverTicksRemaining = 0
        feverMeter = 0f
        isBossActive = false
        publishUiState()
        viewModelScope.launch { persistNow() }
        return true
    }

    fun selectBuyQuantity(quantity: BuyQuantity) {
        selectedBuyQuantity = quantity
        publishUiState()
    }

    fun buyUpgrade(upgradeId: String) {
        val definition = GameCatalog.upgrades.find { it.id == upgradeId } ?: return
        val currentLevel = upgradeLevels[upgradeId] ?: 0
        val plan = GameCatalog.calculateUpgradePurchase(
            definition = definition,
            currentLevel = currentLevel,
            crystals = crystals,
            discountFraction = discountFraction(),
            quantity = selectedBuyQuantity
        )

        if (plan.canAfford && plan.levelsToBuy > 0) {
            crystals -= plan.totalCost
            upgradeLevels[upgradeId] = (currentLevel + plan.levelsToBuy).coerceAtMost(GameCatalog.MAX_UPGRADE_LEVEL)
            dailyUpgrades += plan.levelsToBuy
            recomputeUpgradeSums()
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
        feverTicksRemaining = 0
        feverMeter = 0f
        comboCount = 0
        isBossActive = false
        upgradeLevels.clear()
        recomputeUpgradeSums()

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
            repository.saveSettings(isVibrationEnabled, isSoundEnabled, isPotatoMode)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        isSoundEnabled = enabled
        publishUiState()
        viewModelScope.launch {
            repository.saveSettings(isVibrationEnabled, isSoundEnabled, isPotatoMode)
        }
    }

    fun setPotatoMode(enabled: Boolean) {
        isPotatoMode = enabled
        publishUiState()
        viewModelScope.launch {
            repository.saveSettings(isVibrationEnabled, isSoundEnabled, isPotatoMode)
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
        bossesDefeated = 0L
        feverActivations = 0L
        bestCps = 0
        playTimeSeconds = 0L
        darkMatter = 0L
        spentDarkMatter = 0L
        prestigeCount = 0
        overchargeTicksRemaining = 0
        feverTicksRemaining = 0
        feverMeter = 0f
        comboCount = 0
        isBossActive = false
        activeExpeditionId = null
        expeditionEndMs = 0L
        expeditionLastMessage = null
        secretHeaderUnlocked = false
        headerTapCounter = 0
        selectedSkin = CrystalSkin.CLASSIC
        selectedLocation = SpaceLocation.NEBULA
        dailyClicks = 0
        dailyUpgrades = 0
        dailyFever = 0
        dailyClaimedIds.clear()
        offlineReward = null
        upgradeLevels.clear()
        talentLevels.clear()
        claimedAchievements.clear()
        recomputeUpgradeSums()
        hasDirtyProgress = false
        publishUiState()
        viewModelScope.launch {
            repository.resetProgress(
                keepVibration = isVibrationEnabled,
                keepSound = isSoundEnabled,
                keepPotatoMode = isPotatoMode
            )
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
            bossesDefeated = bossesDefeated,
            feverActivations = feverActivations,
            bestCps = bestCps,
            playTimeSeconds = playTimeSeconds,
            darkMatter = darkMatter,
            spentDarkMatter = spentDarkMatter,
            prestigeCount = prestigeCount,
            upgradeLevels = upgradeLevels.toMap(),
            talentLevels = talentLevels.toMap(),
            claimedAchievements = claimedAchievements.toSet(),
            secretHeaderUnlocked = secretHeaderUnlocked,
            selectedSkinId = selectedSkin.id,
            selectedLocationId = selectedLocation.id,
            activeExpeditionId = activeExpeditionId,
            expeditionEndMs = expeditionEndMs,
            dailyDateKey = dailyDateKey,
            dailyClicks = dailyClicks,
            dailyUpgrades = dailyUpgrades,
            dailyFever = dailyFever,
            dailyClaimedIds = dailyClaimedIds.toSet(),
            isVibrationEnabled = isVibrationEnabled,
            isSoundEnabled = isSoundEnabled,
            isPotatoMode = isPotatoMode,
            lastActiveTimestampMs = System.currentTimeMillis()
        )
    }

    private suspend fun persistNow() {
        hasDirtyProgress = false
        repository.saveProgress(snapshotData())
    }

    private fun resolveRank(): Pair<MinerRank, MinerRank?> {
        val ranks = GameCatalog.ranks
        var low = 0
        var high = ranks.lastIndex
        var idx = 0
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (experience >= ranks[mid].xpThreshold) {
                idx = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return ranks[idx] to ranks.getOrNull(idx + 1)
    }

    private fun isMidnightBonusActive(): Boolean {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return hour in 0..3
    }

    private fun midnightMultiplier(): Double = if (isMidnightBonusActive()) 1.2 else 1.0

    private fun prestigeMultiplier(): Double = 1.0 + (darkMatter + spentDarkMatter) * 0.5

    private fun discountFraction(): Double = (talentLevels["stellar_discount"] ?: 0) * 0.05

    private fun feverDurationSec(): Int = GameCatalog.FEVER_DURATION_SEC + (talentLevels["fever_surge"] ?: 0)

    private fun nextCometDelay(): Int {
        val reduction = (talentLevels["comet_magnet"] ?: 0) * 65
        val minTicks = maxOf(180, 380 - reduction)
        val maxTicks = maxOf(280, 560 - reduction)
        return Random.nextInt(minTicks, maxTicks)
    }

    private fun calculateClickPowerWithoutOvercharge(): Double {
        val (rank, _) = resolveRank()
        return (1.0 + cachedClickBonus) * rank.multiplier * prestigeMultiplier() * midnightMultiplier()
    }

    private fun calculateClickPower(): Double {
        val overcharge = if (overchargeTicksRemaining > 0) GameCatalog.OVERCHARGE_MULTIPLIER else 1.0
        return calculateClickPowerWithoutOvercharge() * overcharge
    }

    private fun calculatePassivePerSecond(): Double {
        val (rank, _) = resolveRank()
        val droneTalentMult = 1.0 + (talentLevels["drone_overclock"] ?: 0) * 0.25
        return cachedPassiveBonus * rank.multiplier * prestigeMultiplier() * droneTalentMult * midnightMultiplier()
    }

    private fun getMetricValue(metric: AchievementMetric): Double {
        return when (metric) {
            AchievementMetric.TOTAL_CLICKS -> totalClicks.toDouble()
            AchievementMetric.TOTAL_CRYSTALS_EARNED -> totalCrystalsEarned
            AchievementMetric.TOTAL_UPGRADES_BOUGHT -> cachedTotalUpgradesBought.toDouble()
            AchievementMetric.PASSIVE_PER_SEC -> calculatePassivePerSecond()
            AchievementMetric.CRITICAL_HITS -> criticalHits.toDouble()
            AchievementMetric.COMETS_CAUGHT -> cometsCaught.toDouble()
            AchievementMetric.PRESTIGE_COUNT -> prestigeCount.toDouble()
            AchievementMetric.BOSSES_DEFEATED -> bossesDefeated.toDouble()
            AchievementMetric.FEVER_COUNT -> feverActivations.toDouble()
            AchievementMetric.SECRET_HEADER_TAP -> if (secretHeaderUnlocked) 1.0 else 0.0
        }
    }

    private fun computeUnlockedAchievementIds(): Set<String> {
        return GameCatalog.achievements
            .filter { getMetricValue(it.metric) >= it.targetValue }
            .map { it.id }
            .toSet()
    }

    private fun dailyProgressFor(type: DailyQuestType): Int = when (type) {
        DailyQuestType.CLICKS -> dailyClicks
        DailyQuestType.UPGRADES -> dailyUpgrades
        DailyQuestType.FEVER -> dailyFever
    }

    private fun publishUiState(lightweight: Boolean = false) {
        val prev = _uiState.value
        val (currentRank, nextRank) = resolveRank()
        val rankProgress = if (nextRank == null) {
            1f
        } else {
            val span = (nextRank.xpThreshold - currentRank.xpThreshold).coerceAtLeast(1.0)
            ((experience - currentRank.xpThreshold) / span).toFloat().coerceIn(0f, 1f)
        }

        val nowUiMs = System.currentTimeMillis()
        val upgradeThrottleMs = if (isPotatoMode) 400L else 180L
        val refreshUpgrades = !lightweight ||
            prev.upgrades.isEmpty() ||
            (selectedTab == BottomTab.UPGRADES && nowUiMs - lastHeavyPublishMs >= upgradeThrottleMs)
        if (refreshUpgrades) {
            lastHeavyPublishMs = nowUiMs
        }

        val discount = discountFraction()
        val upgradeItems = if (refreshUpgrades) {
            GameCatalog.upgrades.map { def ->
                val level = upgradeLevels[def.id] ?: 0
                val plan = GameCatalog.calculateUpgradePurchase(
                    definition = def,
                    currentLevel = level,
                    crystals = crystals,
                    discountFraction = discount,
                    quantity = selectedBuyQuantity
                )
                UpgradeItemUi(
                    definition = def,
                    level = level,
                    nextCost = plan.totalCost,
                    levelsToBuy = plan.levelsToBuy,
                    canAfford = plan.canAfford
                )
            }
        } else {
            prev.upgrades
        }

        val achievementItems = if (!lightweight || prev.achievements.isEmpty()) {
            GameCatalog.achievements.map { def ->
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
        } else {
            prev.achievements
        }
        val unlockedIds = if (!lightweight || prev.unlockedAchievementIds.isEmpty()) {
            achievementItems.filter { it.isUnlocked }.map { it.definition.id }.toSet()
        } else {
            prev.unlockedAchievementIds
        }

        val talentItems = if (!lightweight || prev.talents.isEmpty()) {
            GameCatalog.talents.map { def ->
                val level = talentLevels[def.id] ?: 0
                val cost = def.costAtLevel(level)
                val maxed = level >= def.maxLevel
                TalentItemUi(
                    definition = def,
                    level = level,
                    nextCostDm = cost,
                    canAfford = !maxed && darkMatter >= cost,
                    isMaxed = maxed
                )
            }
        } else {
            prev.talents
        }

        val dailyItems = if (!lightweight || prev.dailyQuests.isEmpty()) {
            GameCatalog.dailyQuests.map { def ->
                val prog = dailyProgressFor(def.type)
                val claimed = dailyClaimedIds.contains(def.id)
                DailyQuestItemUi(
                    definition = def,
                    progress = prog,
                    isCompleted = prog >= def.target,
                    isClaimed = claimed
                )
            }
        } else {
            prev.dailyQuests
        }

        val expRemainingSec = if (activeExpeditionId != null) {
            ((expeditionEndMs - System.currentTimeMillis()) / 1000L).coerceAtLeast(0L)
        } else 0L

        val potentialDm = GameCatalog.calculatePotentialDarkMatter(runCrystalsEarned)

        _uiState.value = GameUiState(
            crystals = crystals,
            runCrystalsEarned = runCrystalsEarned,
            totalCrystalsEarned = totalCrystalsEarned,
            experience = experience,
            totalClicks = totalClicks,
            criticalHits = criticalHits,
            cometsCaught = cometsCaught,
            bossesDefeated = bossesDefeated,
            feverActivations = feverActivations,
            bestCps = bestCps,
            currentCps = recentClickTimestamps.size,
            playTimeSeconds = playTimeSeconds,
            clickPower = calculateClickPower(),
            passivePerSecond = calculatePassivePerSecond(),
            currentRank = currentRank,
            nextRank = nextRank,
            rankProgress = rankProgress,
            darkMatter = darkMatter,
            spentDarkMatter = spentDarkMatter,
            prestigeCount = prestigeCount,
            potentialDarkMatter = potentialDm,
            prestigeMultiplier = prestigeMultiplier(),
            nextDarkMatterThreshold = GameCatalog.nextDarkMatterRequirement(potentialDm),
            overchargeSecondsLeft = (overchargeTicksRemaining + 9) / 10,
            feverMeter = feverMeter,
            feverSecondsLeft = (feverTicksRemaining + 9) / 10,
            comboCount = comboCount,
            isMidnightBonusActive = isMidnightBonusActive(),
            isCometVisible = isCometVisible,
            cometSeed = cometSeed,
            visualDroneCount = cachedVisualDroneCount,
            selectedSkin = selectedSkin,
            selectedLocation = selectedLocation,
            unlockedAchievementIds = unlockedIds,
            bossState = BossUiState(
                isActive = isBossActive,
                name = bossName,
                currentHp = bossCurrentHp,
                maxHp = bossMaxHp,
                remainingSeconds = (bossTicksRemaining + 9) / 10,
                cooldownSeconds = (bossCooldownTicks + 9) / 10,
                lastResultMessage = bossLastResult
            ),
            expeditionState = ExpeditionUiState(
                activePresetId = activeExpeditionId,
                remainingSeconds = expRemainingSec,
                isCompleted = activeExpeditionId != null && expRemainingSec == 0L,
                lastRewardMessage = expeditionLastMessage
            ),
            talents = talentItems,
            dailyQuests = dailyItems,
            offlineReward = offlineReward,
            availableUpdate = availableUpdate,
            isCheckingUpdate = isCheckingUpdate,
            updateStatusMessage = updateStatusMessage,
            upgrades = upgradeItems,
            achievements = achievementItems,
            unclaimedAchievementsCount = achievementItems.count { it.isUnlocked && !it.isClaimed },
            unclaimedQuestsCount = dailyItems.count { it.isCompleted && !it.isClaimed },
            secretUnlockedToast = secretToast,
            selectedTab = selectedTab,
            selectedBuyQuantity = selectedBuyQuantity,
            isVibrationEnabled = isVibrationEnabled,
            isSoundEnabled = isSoundEnabled,
            isPotatoMode = isPotatoMode,
            isLoaded = isLoaded
        )
    }
}
