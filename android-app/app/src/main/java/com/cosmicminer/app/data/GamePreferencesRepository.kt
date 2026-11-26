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
    val totalCrystalsEarned: Double = 0.0,
    val experience: Double = 0.0,
    val totalClicks: Long = 0L,
    val criticalHits: Long = 0L,
    val upgradeLevels: Map<String, Int> = emptyMap(),
    val claimedAchievements: Set<String> = emptySet(),
    val isVibrationEnabled: Boolean = true
)

class GamePreferencesRepository(private val context: Context) {

    private object Keys {
        val CRYSTALS = doublePreferencesKey("crystals")
        val TOTAL_CRYSTALS = doublePreferencesKey("total_crystals_earned")
        val EXPERIENCE = doublePreferencesKey("experience")
        val TOTAL_CLICKS = longPreferencesKey("total_clicks")
        val CRITICAL_HITS = longPreferencesKey("critical_hits")
        val CLAIMED_ACHIEVEMENTS = stringSetPreferencesKey("claimed_achievements")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")

        fun upgradeLevel(id: String) = intPreferencesKey("upgrade_level_$id")
    }

    val gameDataFlow: Flow<PersistedGameData> = context.gameDataStore.data.map { prefs ->
        val levels = GameCatalog.upgrades.associate { upgrade ->
            upgrade.id to (prefs[Keys.upgradeLevel(upgrade.id)] ?: 0)
        }
        PersistedGameData(
            crystals = prefs[Keys.CRYSTALS] ?: 0.0,
            totalCrystalsEarned = prefs[Keys.TOTAL_CRYSTALS] ?: 0.0,
            experience = prefs[Keys.EXPERIENCE] ?: 0.0,
            totalClicks = prefs[Keys.TOTAL_CLICKS] ?: 0L,
            criticalHits = prefs[Keys.CRITICAL_HITS] ?: 0L,
            upgradeLevels = levels,
            claimedAchievements = prefs[Keys.CLAIMED_ACHIEVEMENTS] ?: emptySet(),
            isVibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true
        )
    }

    suspend fun saveProgress(data: PersistedGameData) {
        context.gameDataStore.edit { prefs ->
            prefs[Keys.CRYSTALS] = data.crystals
            prefs[Keys.TOTAL_CRYSTALS] = data.totalCrystalsEarned
            prefs[Keys.EXPERIENCE] = data.experience
            prefs[Keys.TOTAL_CLICKS] = data.totalClicks
            prefs[Keys.CRITICAL_HITS] = data.criticalHits
            prefs[Keys.CLAIMED_ACHIEVEMENTS] = data.claimedAchievements
            prefs[Keys.VIBRATION_ENABLED] = data.isVibrationEnabled
            data.upgradeLevels.forEach { (id, level) ->
                prefs[Keys.upgradeLevel(id)] = level
            }
        }
    }

    suspend fun saveVibrationSetting(enabled: Boolean) {
        context.gameDataStore.edit { prefs ->
            prefs[Keys.VIBRATION_ENABLED] = enabled
        }
    }

    suspend fun resetProgress(keepVibrationSetting: Boolean) {
        context.gameDataStore.edit { prefs ->
            prefs.clear()
            prefs[Keys.VIBRATION_ENABLED] = keepVibrationSetting
        }
    }
}
