package com.cosmicminer.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cosmicminer.app.model.CrystalSkin
import com.cosmicminer.app.model.GameCatalog
import com.cosmicminer.app.model.SpaceLocation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.gameDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "cosmic_miner_prefs"
)

data class PersistedGameData(
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
    val playTimeSeconds: Long = 0L,
    val darkMatter: Long = 0L,
    val spentDarkMatter: Long = 0L,
    val prestigeCount: Int = 0,
    val upgradeLevels: Map<String, Int> = emptyMap(),
    val talentLevels: Map<String, Int> = emptyMap(),
    val claimedAchievements: Set<String> = emptySet(),
    val secretHeaderUnlocked: Boolean = false,
    val selectedSkinId: String = CrystalSkin.CLASSIC.id,
    val selectedLocationId: String = SpaceLocation.NEBULA.id,
    val activeExpeditionId: String? = null,
    val expeditionEndMs: Long = 0L,
    val dailyDateKey: String = "",
    val dailyClicks: Int = 0,
    val dailyUpgrades: Int = 0,
    val dailyFever: Int = 0,
    val dailyClaimedIds: Set<String> = emptySet(),
    val isVibrationEnabled: Boolean = true,
    val isSoundEnabled: Boolean = true,
    val isPotatoMode: Boolean = false,
    val lastActiveTimestampMs: Long = 0L
)

class GamePreferencesRepository(private val context: Context) {

    private object Keys {
        val CRYSTALS = doublePreferencesKey("crystals")
        val RUN_CRYSTALS = doublePreferencesKey("run_crystals_earned")
        val TOTAL_CRYSTALS = doublePreferencesKey("total_crystals_earned")
        val EXPERIENCE = doublePreferencesKey("experience")
        val TOTAL_CLICKS = longPreferencesKey("total_clicks")
        val CRITICAL_HITS = longPreferencesKey("critical_hits")
        val COMETS_CAUGHT = longPreferencesKey("comets_caught")
        val BOSSES_DEFEATED = longPreferencesKey("bosses_defeated")
        val FEVER_ACTIVATIONS = longPreferencesKey("fever_activations")
        val BEST_CPS = intPreferencesKey("best_cps")
        val PLAY_TIME_SEC = longPreferencesKey("play_time_seconds")
        val DARK_MATTER = longPreferencesKey("dark_matter")
        val SPENT_DARK_MATTER = longPreferencesKey("spent_dark_matter")
        val PRESTIGE_COUNT = intPreferencesKey("prestige_count")
        val CLAIMED_ACHIEVEMENTS = stringSetPreferencesKey("claimed_achievements")
        val SECRET_HEADER_UNLOCKED = booleanPreferencesKey("secret_header_unlocked")
        val SELECTED_SKIN = stringPreferencesKey("selected_skin")
        val SELECTED_LOCATION = stringPreferencesKey("selected_location")
        val ACTIVE_EXPEDITION_ID = stringPreferencesKey("active_expedition_id")
        val EXPEDITION_END_MS = longPreferencesKey("expedition_end_ms")
        val DAILY_DATE_KEY = stringPreferencesKey("daily_date_key")
        val DAILY_CLICKS = intPreferencesKey("daily_clicks")
        val DAILY_UPGRADES = intPreferencesKey("daily_upgrades")
        val DAILY_FEVER = intPreferencesKey("daily_fever")
        val DAILY_CLAIMED = stringSetPreferencesKey("daily_claimed")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val POTATO_MODE = booleanPreferencesKey("potato_mode")
        val LAST_ACTIVE_MS = longPreferencesKey("last_active_timestamp_ms")

        fun upgradeLevel(id: String) = intPreferencesKey("upgrade_level_$id")
        fun talentLevel(id: String) = intPreferencesKey("talent_level_$id")
    }

    val gameDataFlow: Flow<PersistedGameData> = context.gameDataStore.data.map { prefs ->
        val levels = GameCatalog.upgrades.associate { upgrade ->
            upgrade.id to (prefs[Keys.upgradeLevel(upgrade.id)] ?: 0)
        }
        val talents = GameCatalog.talents.associate { talent ->
            talent.id to (prefs[Keys.talentLevel(talent.id)] ?: 0)
        }
        val totalCrystals = prefs[Keys.TOTAL_CRYSTALS] ?: 0.0
        val expIdRaw = prefs[Keys.ACTIVE_EXPEDITION_ID] ?: ""
        PersistedGameData(
            crystals = prefs[Keys.CRYSTALS] ?: 0.0,
            runCrystalsEarned = prefs[Keys.RUN_CRYSTALS] ?: totalCrystals,
            totalCrystalsEarned = totalCrystals,
            experience = prefs[Keys.EXPERIENCE] ?: 0.0,
            totalClicks = prefs[Keys.TOTAL_CLICKS] ?: 0L,
            criticalHits = prefs[Keys.CRITICAL_HITS] ?: 0L,
            cometsCaught = prefs[Keys.COMETS_CAUGHT] ?: 0L,
            bossesDefeated = prefs[Keys.BOSSES_DEFEATED] ?: 0L,
            feverActivations = prefs[Keys.FEVER_ACTIVATIONS] ?: 0L,
            bestCps = prefs[Keys.BEST_CPS] ?: 0,
            playTimeSeconds = prefs[Keys.PLAY_TIME_SEC] ?: 0L,
            darkMatter = prefs[Keys.DARK_MATTER] ?: 0L,
            spentDarkMatter = prefs[Keys.SPENT_DARK_MATTER] ?: 0L,
            prestigeCount = prefs[Keys.PRESTIGE_COUNT] ?: 0,
            upgradeLevels = levels,
            talentLevels = talents,
            claimedAchievements = prefs[Keys.CLAIMED_ACHIEVEMENTS] ?: emptySet(),
            secretHeaderUnlocked = prefs[Keys.SECRET_HEADER_UNLOCKED] ?: false,
            selectedSkinId = prefs[Keys.SELECTED_SKIN] ?: CrystalSkin.CLASSIC.id,
            selectedLocationId = prefs[Keys.SELECTED_LOCATION] ?: SpaceLocation.NEBULA.id,
            activeExpeditionId = expIdRaw.ifEmpty { null },
            expeditionEndMs = prefs[Keys.EXPEDITION_END_MS] ?: 0L,
            dailyDateKey = prefs[Keys.DAILY_DATE_KEY] ?: "",
            dailyClicks = prefs[Keys.DAILY_CLICKS] ?: 0,
            dailyUpgrades = prefs[Keys.DAILY_UPGRADES] ?: 0,
            dailyFever = prefs[Keys.DAILY_FEVER] ?: 0,
            dailyClaimedIds = prefs[Keys.DAILY_CLAIMED] ?: emptySet(),
            isVibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            isSoundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
            isPotatoMode = prefs[Keys.POTATO_MODE] ?: false,
            lastActiveTimestampMs = prefs[Keys.LAST_ACTIVE_MS] ?: 0L
        )
    }

    suspend fun saveProgress(data: PersistedGameData) {
        context.gameDataStore.edit { prefs ->
            prefs[Keys.CRYSTALS] = data.crystals
            prefs[Keys.RUN_CRYSTALS] = data.runCrystalsEarned
            prefs[Keys.TOTAL_CRYSTALS] = data.totalCrystalsEarned
            prefs[Keys.EXPERIENCE] = data.experience
            prefs[Keys.TOTAL_CLICKS] = data.totalClicks
            prefs[Keys.CRITICAL_HITS] = data.criticalHits
            prefs[Keys.COMETS_CAUGHT] = data.cometsCaught
            prefs[Keys.BOSSES_DEFEATED] = data.bossesDefeated
            prefs[Keys.FEVER_ACTIVATIONS] = data.feverActivations
            prefs[Keys.BEST_CPS] = data.bestCps
            prefs[Keys.PLAY_TIME_SEC] = data.playTimeSeconds
            prefs[Keys.DARK_MATTER] = data.darkMatter
            prefs[Keys.SPENT_DARK_MATTER] = data.spentDarkMatter
            prefs[Keys.PRESTIGE_COUNT] = data.prestigeCount
            prefs[Keys.CLAIMED_ACHIEVEMENTS] = data.claimedAchievements
            prefs[Keys.SECRET_HEADER_UNLOCKED] = data.secretHeaderUnlocked
            prefs[Keys.SELECTED_SKIN] = data.selectedSkinId
            prefs[Keys.SELECTED_LOCATION] = data.selectedLocationId
            prefs[Keys.ACTIVE_EXPEDITION_ID] = data.activeExpeditionId ?: ""
            prefs[Keys.EXPEDITION_END_MS] = data.expeditionEndMs
            prefs[Keys.DAILY_DATE_KEY] = data.dailyDateKey
            prefs[Keys.DAILY_CLICKS] = data.dailyClicks
            prefs[Keys.DAILY_UPGRADES] = data.dailyUpgrades
            prefs[Keys.DAILY_FEVER] = data.dailyFever
            prefs[Keys.DAILY_CLAIMED] = data.dailyClaimedIds
            prefs[Keys.VIBRATION_ENABLED] = data.isVibrationEnabled
            prefs[Keys.SOUND_ENABLED] = data.isSoundEnabled
            prefs[Keys.POTATO_MODE] = data.isPotatoMode
            prefs[Keys.LAST_ACTIVE_MS] = data.lastActiveTimestampMs
            data.upgradeLevels.forEach { (id, level) ->
                prefs[Keys.upgradeLevel(id)] = level
            }
            data.talentLevels.forEach { (id, level) ->
                prefs[Keys.talentLevel(id)] = level
            }
        }
    }

    suspend fun saveSettings(vibrationEnabled: Boolean, soundEnabled: Boolean, potatoMode: Boolean) {
        context.gameDataStore.edit { prefs ->
            prefs[Keys.VIBRATION_ENABLED] = vibrationEnabled
            prefs[Keys.SOUND_ENABLED] = soundEnabled
            prefs[Keys.POTATO_MODE] = potatoMode
        }
    }

    suspend fun resetProgress(keepVibration: Boolean, keepSound: Boolean, keepPotatoMode: Boolean) {
        context.gameDataStore.edit { prefs ->
            prefs.clear()
            prefs[Keys.VIBRATION_ENABLED] = keepVibration
            prefs[Keys.SOUND_ENABLED] = keepSound
            prefs[Keys.POTATO_MODE] = keepPotatoMode
            prefs[Keys.LAST_ACTIVE_MS] = System.currentTimeMillis()
        }
    }
}
