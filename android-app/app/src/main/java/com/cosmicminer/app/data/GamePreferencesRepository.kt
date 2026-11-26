package com.cosmicminer.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.cosmicminer.app.model.GameCatalog
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
    val darkMatter: Long = 0L,
    val prestigeCount: Int = 0,
    val upgradeLevels: Map<String, Int> = emptyMap(),
    val claimedAchievements: Set<String> = emptySet(),
    val isVibrationEnabled: Boolean = true,
    val isSoundEnabled: Boolean = true,
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
        val DARK_MATTER = longPreferencesKey("dark_matter")
        val PRESTIGE_COUNT = intPreferencesKey("prestige_count")
        val CLAIMED_ACHIEVEMENTS = stringSetPreferencesKey("claimed_achievements")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val LAST_ACTIVE_MS = longPreferencesKey("last_active_timestamp_ms")

        fun upgradeLevel(id: String) = intPreferencesKey("upgrade_level_$id")
    }

    val gameDataFlow: Flow<PersistedGameData> = context.gameDataStore.data.map { prefs ->
        val levels = GameCatalog.upgrades.associate { upgrade ->
            upgrade.id to (prefs[Keys.upgradeLevel(upgrade.id)] ?: 0)
        }
        val totalCrystals = prefs[Keys.TOTAL_CRYSTALS] ?: 0.0
        PersistedGameData(
            crystals = prefs[Keys.CRYSTALS] ?: 0.0,
            runCrystalsEarned = prefs[Keys.RUN_CRYSTALS] ?: totalCrystals,
            totalCrystalsEarned = totalCrystals,
            experience = prefs[Keys.EXPERIENCE] ?: 0.0,
            totalClicks = prefs[Keys.TOTAL_CLICKS] ?: 0L,
            criticalHits = prefs[Keys.CRITICAL_HITS] ?: 0L,
            cometsCaught = prefs[Keys.COMETS_CAUGHT] ?: 0L,
            darkMatter = prefs[Keys.DARK_MATTER] ?: 0L,
            prestigeCount = prefs[Keys.PRESTIGE_COUNT] ?: 0,
            upgradeLevels = levels,
            claimedAchievements = prefs[Keys.CLAIMED_ACHIEVEMENTS] ?: emptySet(),
            isVibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
            isSoundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
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
            prefs[Keys.DARK_MATTER] = data.darkMatter
            prefs[Keys.PRESTIGE_COUNT] = data.prestigeCount
            prefs[Keys.CLAIMED_ACHIEVEMENTS] = data.claimedAchievements
            prefs[Keys.VIBRATION_ENABLED] = data.isVibrationEnabled
            prefs[Keys.SOUND_ENABLED] = data.isSoundEnabled
            prefs[Keys.LAST_ACTIVE_MS] = data.lastActiveTimestampMs
            data.upgradeLevels.forEach { (id, level) ->
                prefs[Keys.upgradeLevel(id)] = level
            }
        }
    }

    suspend fun saveSettings(vibrationEnabled: Boolean, soundEnabled: Boolean) {
        context.gameDataStore.edit { prefs ->
            prefs[Keys.VIBRATION_ENABLED] = vibrationEnabled
            prefs[Keys.SOUND_ENABLED] = soundEnabled
        }
    }

    suspend fun resetProgress(keepVibration: Boolean, keepSound: Boolean) {
        context.gameDataStore.edit { prefs ->
            prefs.clear()
            prefs[Keys.VIBRATION_ENABLED] = keepVibration
            prefs[Keys.SOUND_ENABLED] = keepSound
            prefs[Keys.LAST_ACTIVE_MS] = System.currentTimeMillis()
        }
    }
}
