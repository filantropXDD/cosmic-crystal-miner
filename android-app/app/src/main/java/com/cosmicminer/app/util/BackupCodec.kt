package com.cosmicminer.app.util

import android.util.Base64
import com.cosmicminer.app.data.PersistedGameData
import com.cosmicminer.app.model.CrystalSkin
import com.cosmicminer.app.model.GameCatalog
import com.cosmicminer.app.model.SpaceLocation
import org.json.JSONArray
import org.json.JSONObject

object BackupCodec {
    private const val PREFIX = "CCM1:"

    fun encode(data: PersistedGameData): String {
        val root = JSONObject().apply {
            put("crystals", data.crystals)
            put("runCrystalsEarned", data.runCrystalsEarned)
            put("totalCrystalsEarned", data.totalCrystalsEarned)
            put("experience", data.experience)
            put("totalClicks", data.totalClicks)
            put("criticalHits", data.criticalHits)
            put("cometsCaught", data.cometsCaught)
            put("bossesDefeated", data.bossesDefeated)
            put("feverActivations", data.feverActivations)
            put("bestCps", data.bestCps)
            put("playTimeSeconds", data.playTimeSeconds)
            put("darkMatter", data.darkMatter)
            put("spentDarkMatter", data.spentDarkMatter)
            put("prestigeCount", data.prestigeCount)
            put("secretHeaderUnlocked", data.secretHeaderUnlocked)
            put("selectedSkinId", data.selectedSkinId)
            put("selectedLocationId", data.selectedLocationId)
            put("isVibrationEnabled", data.isVibrationEnabled)
            put("isSoundEnabled", data.isSoundEnabled)

            val upgradesObj = JSONObject()
            data.upgradeLevels.forEach { (id, level) ->
                if (level > 0) upgradesObj.put(id, level)
            }
            put("upgrades", upgradesObj)

            val talentsObj = JSONObject()
            data.talentLevels.forEach { (id, level) ->
                if (level > 0) talentsObj.put(id, level)
            }
            put("talents", talentsObj)

            val achArray = JSONArray()
            data.claimedAchievements.forEach { achArray.put(it) }
            put("achievements", achArray)
        }
        val bytes = root.toString().toByteArray(Charsets.UTF_8)
        return PREFIX + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun decode(rawInput: String): PersistedGameData? {
        return try {
            val trimmed = rawInput.trim()
            if (!trimmed.startsWith(PREFIX)) return null
            val encoded = trimmed.removePrefix(PREFIX)
            val jsonText = String(Base64.decode(encoded, Base64.DEFAULT), Charsets.UTF_8)
            val obj = JSONObject(jsonText)

            val upgradesObj = obj.optJSONObject("upgrades")
            val levels = GameCatalog.upgrades.associate { def ->
                def.id to (upgradesObj?.optInt(def.id, 0) ?: 0).coerceIn(0, GameCatalog.MAX_UPGRADE_LEVEL)
            }

            val talentsObj = obj.optJSONObject("talents")
            val talents = GameCatalog.talents.associate { def ->
                def.id to (talentsObj?.optInt(def.id, 0) ?: 0).coerceIn(0, def.maxLevel)
            }

            val achArray = obj.optJSONArray("achievements")
            val validIds = GameCatalog.achievements.map { it.id }.toSet()
            val claimed = mutableSetOf<String>()
            if (achArray != null) {
                for (i in 0 until achArray.length()) {
                    val id = achArray.optString(i, "")
                    if (validIds.contains(id)) {
                        claimed.add(id)
                    }
                }
            }

            PersistedGameData(
                crystals = obj.optDouble("crystals", 0.0).coerceAtLeast(0.0),
                runCrystalsEarned = obj.optDouble("runCrystalsEarned", 0.0).coerceAtLeast(0.0),
                totalCrystalsEarned = obj.optDouble("totalCrystalsEarned", 0.0).coerceAtLeast(0.0),
                experience = obj.optDouble("experience", 0.0).coerceAtLeast(0.0),
                totalClicks = obj.optLong("totalClicks", 0L).coerceAtLeast(0L),
                criticalHits = obj.optLong("criticalHits", 0L).coerceAtLeast(0L),
                cometsCaught = obj.optLong("cometsCaught", 0L).coerceAtLeast(0L),
                bossesDefeated = obj.optLong("bossesDefeated", 0L).coerceAtLeast(0L),
                feverActivations = obj.optLong("feverActivations", 0L).coerceAtLeast(0L),
                bestCps = obj.optInt("bestCps", 0).coerceAtLeast(0),
                playTimeSeconds = obj.optLong("playTimeSeconds", 0L).coerceAtLeast(0L),
                darkMatter = obj.optLong("darkMatter", 0L).coerceAtLeast(0L),
                spentDarkMatter = obj.optLong("spentDarkMatter", 0L).coerceAtLeast(0L),
                prestigeCount = obj.optInt("prestigeCount", 0).coerceAtLeast(0),
                upgradeLevels = levels,
                talentLevels = talents,
                claimedAchievements = claimed,
                secretHeaderUnlocked = obj.optBoolean("secretHeaderUnlocked", false),
                selectedSkinId = CrystalSkin.fromId(obj.optString("selectedSkinId", CrystalSkin.CLASSIC.id)).id,
                selectedLocationId = SpaceLocation.fromId(obj.optString("selectedLocationId", SpaceLocation.NEBULA.id)).id,
                isVibrationEnabled = obj.optBoolean("isVibrationEnabled", true),
                isSoundEnabled = obj.optBoolean("isSoundEnabled", true),
                lastActiveTimestampMs = System.currentTimeMillis()
            )
        } catch (_: Throwable) {
            null
        }
    }
}
