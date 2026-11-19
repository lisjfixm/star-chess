package com.example.data.local

import org.json.JSONArray
import org.json.JSONObject

data class ProgressionSummary(
    val starCoins: Int = 0,
    val eloPeak: Int = 1000,
    val achievementsCount: Int = 0,
    val gamesPlayed: Int = 0,
    val themeKey: String = "default",
    val storedKeysCount: Int = 0,
) {
    companion object {
        fun fromEntries(entries: Map<String, String>): ProgressionSummary {
            val coins = entries["starchess.starCoins"]?.trim()?.toIntOrNull()?.coerceAtLeast(0) ?: 0
            val theme = entries["starchess.theme"]?.trim()?.ifBlank { "default" } ?: "default"

            var peakElo = 1000
            entries["starchess.elo"]?.let { raw ->
                runCatching {
                    val obj = JSONObject(raw)
                    listOf("eloByGame", "eloBestByGame").forEach { key ->
                        val sub = obj.optJSONObject(key)
                        if (sub != null) {
                            val keys = sub.keys()
                            while (keys.hasNext()) {
                                val gameKey = keys.next()
                                val valElo = sub.optInt(gameKey, 1000)
                                if (valElo > peakElo) peakElo = valElo
                            }
                        }
                    }
                }
            }

            var achCount = 0
            var totalGamesFromStats = 0
            entries["starchess.ach"]?.let { raw ->
                runCatching {
                    val obj = JSONObject(raw)
                    achCount = obj.optJSONObject("ach")?.length() ?: 0
                    val stats = obj.optJSONObject("stats")
                    if (stats != null) {
                        totalGamesFromStats = stats.optInt("total", 0)
                    }
                }
            }

            var recordsCount = 0
            entries["starchess.records"]?.let { raw ->
                runCatching {
                    recordsCount = JSONArray(raw).length()
                }
            }

            var statsByGameTotal = 0
            entries["starchess.v7.statsByGame"]?.let { raw ->
                runCatching {
                    val obj = JSONObject(raw)
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val g = keys.next()
                        val item = obj.optJSONObject(g)
                        if (item != null) {
                            statsByGameTotal += item.optInt("played", item.optInt("total", 0))
                        }
                    }
                }
            }

            val gamesPlayed = maxOf(totalGamesFromStats, statsByGameTotal, recordsCount)

            return ProgressionSummary(
                starCoins = coins,
                eloPeak = peakElo,
                achievementsCount = achCount,
                gamesPlayed = gamesPlayed,
                themeKey = theme,
                storedKeysCount = entries.size,
            )
        }
    }
}
