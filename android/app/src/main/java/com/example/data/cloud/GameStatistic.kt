package com.example.data.cloud

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import org.json.JSONArray
import org.json.JSONObject

data class SupportedGameMeta(
    val gameId: String,
    val nameEn: String,
    val nameZhHant: String,
    val nameZhHans: String,
    val nameJa: String,
) {
    fun localizedName(languageCode: String): String = when (languageCode) {
        "zh-Hant" -> nameZhHant
        "zh-Hans" -> nameZhHans
        "ja" -> nameJa
        else -> nameEn
    }
}

object StarChessGameCatalog {
    val ALL_26_GAMES: List<SupportedGameMeta> = listOf(
        SupportedGameMeta("ttt", "Tic-Tac-Toe", "井字棋", "井字棋", "三目並べ"),
        SupportedGameMeta("c4", "Connect Four", "四子棋", "四子棋", "四目並べ"),
        SupportedGameMeta("gomoku", "Gomoku", "五子棋", "五子棋", "五目並べ"),
        SupportedGameMeta("c6", "Connect6", "六子棋", "六子棋", "六目並べ"),
        SupportedGameMeta("checkers", "Chinese Checkers", "星陣跳棋", "星阵跳棋", "ダイヤモンドゲーム"),
        SupportedGameMeta("go", "Go", "圍棋", "围棋", "囲碁"),
        SupportedGameMeta("jungle", "Jungle Chess", "鬥獸棋", "斗兽棋", "闘獣棋"),
        SupportedGameMeta("chess", "Chess", "西洋棋", "国际象棋", "チェス"),
        SupportedGameMeta("reversi", "Reversi", "黑白棋", "黑白棋", "リバーシ"),
        SupportedGameMeta("xiangqi", "Xiangqi", "象棋", "中国象棋", "シャンチー"),
        SupportedGameMeta("draughts", "Draughts", "西洋跳棋", "国际跳棋", "チェッカー"),
        SupportedGameMeta("hex", "Hex", "六角棋", "六角棋", "ヘックス"),
        SupportedGameMeta("pente", "Pente", "雙吃五子棋", "双吃五子棋", "ペンテ"),
        SupportedGameMeta("dots", "Star Dots & Boxes", "星點連盒", "星点连盒", "ドット＆ボックス"),
        SupportedGameMeta("morris", "Nine Men's Morris", "九子直棋", "九子直棋", "ナインメンズモリス"),
        SupportedGameMeta("mancala", "Mancala", "星盤播棋", "星盘播棋", "マンカラ"),
        SupportedGameMeta("ataxx", "Star Ataxx", "星際同化棋", "星际同化棋", "アタックス"),
        SupportedGameMeta("breakthrough", "Star Breakthrough", "星際突破棋", "星际突破棋", "ブレイクスルー"),
        SupportedGameMeta("shogi", "Shogi", "將棋", "将棋", "将棋"),
        SupportedGameMeta("banqi", "Banqi", "暗棋", "暗棋", "暗棋"),
        SupportedGameMeta("uttt", "Ultimate Tic-Tac-Toe", "終極井字棋", "终极井字棋", "究極三目並べ"),
        SupportedGameMeta("qubic", "Qubic 3D", "星際立體四子", "星际立体四子", "立体四目並べ"),
        SupportedGameMeta("loa", "Lines of Action", "星群匯聚棋", "星群汇聚棋", "ラインズ・オブ・アクション"),
        SupportedGameMeta("amazons", "Game of the Amazons", "亞馬遜棋", "亚马逊棋", "アマゾンズ"),
        SupportedGameMeta("yavalath", "Yavalath", "星蜂四連棋", "星蜂四连棋", "ヤヴァラス"),
        SupportedGameMeta("snakes", "Star Snakes", "星際蛇棋", "星际蛇棋", "スネークス"),
    )

    val SUPPORTED_IDS: Set<String> = ALL_26_GAMES.map { it.gameId }.toSet()
}

@Keep
@IgnoreExtraProperties
data class GameStatistic(
    @DocumentId val id: String = "",
    val userId: String = "",
    val gameId: String = "",
    val wins: Long = 0L,
    val losses: Long = 0L,
    val draws: Long = 0L,
    val playDurationSeconds: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
) {
    val totalMatches: Long
        get() = wins + losses + draws

    val winRatePercent: Int
        get() = if (totalMatches > 0L) ((wins * 100L) / totalMatches).toInt().coerceIn(0, 100) else 0

    fun toCreateMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "gameId" to gameId,
        "wins" to wins.coerceIn(0L, 1_000_000L),
        "losses" to losses.coerceIn(0L, 1_000_000L),
        "draws" to draws.coerceIn(0L, 1_000_000L),
        "playDurationSeconds" to playDurationSeconds.coerceIn(0L, 100_000_000L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "wins" to wins.coerceIn(0L, 1_000_000L),
        "losses" to losses.coerceIn(0L, 1_000_000L),
        "draws" to draws.coerceIn(0L, 1_000_000L),
        "playDurationSeconds" to playDurationSeconds.coerceIn(0L, 100_000_000L),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

data class LocalGameStatSnapshot(
    val gameId: String,
    val wins: Long,
    val losses: Long,
    val draws: Long,
    val playDurationSeconds: Long,
)

data class AggregatedGameStatistics(
    val totalWins: Long = 0L,
    val totalLosses: Long = 0L,
    val totalDraws: Long = 0L,
    val totalPlayDurationSeconds: Long = 0L,
    val playedGamesCount: Int = 0,
    val allGames: List<GameStatistic> = emptyList(),
) {
    val totalMatches: Long
        get() = totalWins + totalLosses + totalDraws

    val overallWinRatePercent: Int
        get() = if (totalMatches > 0L) ((totalWins * 100L) / totalMatches).toInt().coerceIn(0, 100) else 0

    companion object {
        fun fromCloudList(cloudDocs: List<GameStatistic>, userId: String): AggregatedGameStatistics {
            val byId = cloudDocs.associateBy { it.gameId.ifBlank { it.id } }
            val fullList = StarChessGameCatalog.ALL_26_GAMES.map { meta ->
                byId[meta.gameId] ?: GameStatistic(
                    id = meta.gameId,
                    userId = userId,
                    gameId = meta.gameId,
                )
            }
            val totalWins = fullList.sumOf { it.wins }
            val totalLosses = fullList.sumOf { it.losses }
            val totalDraws = fullList.sumOf { it.draws }
            val totalDuration = fullList.sumOf { it.playDurationSeconds }
            val playedGames = fullList.count { it.totalMatches > 0L || it.playDurationSeconds > 0L }
            return AggregatedGameStatistics(
                totalWins = totalWins,
                totalLosses = totalLosses,
                totalDraws = totalDraws,
                totalPlayDurationSeconds = totalDuration,
                playedGamesCount = playedGames,
                allGames = fullList.sortedWith(
                    compareByDescending<GameStatistic> { it.totalMatches }
                        .thenByDescending { it.playDurationSeconds }
                        .thenBy { StarChessGameCatalog.ALL_26_GAMES.indexOfFirst { g -> g.gameId == it.gameId } }
                ),
            )
        }
    }
}

fun extractLocalGameStatistics(entries: Map<String, String>): Map<String, LocalGameStatSnapshot> {
    val winsMap = mutableMapOf<String, Long>()
    val lossesMap = mutableMapOf<String, Long>()
    val drawsMap = mutableMapOf<String, Long>()
    val durationMap = mutableMapOf<String, Long>()

    // 1. Primary per-game counters from starchess.v7.statsByGame
    entries["starchess.v7.statsByGame"]?.let { raw ->
        runCatching {
            val obj = JSONObject(raw)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val gameId = keys.next()
                if (gameId !in StarChessGameCatalog.SUPPORTED_IDS) continue
                val item = obj.optJSONObject(gameId) ?: continue
                winsMap[gameId] = item.optLong("win", 0L).coerceAtLeast(0L)
                lossesMap[gameId] = item.optLong("lose", 0L).coerceAtLeast(0L)
                drawsMap[gameId] = item.optLong("draw", 0L).coerceAtLeast(0L)
            }
        }
    }

    // 2. Fallback / match timer duration from starchess.records
    val recWins = mutableMapOf<String, Long>()
    val recLosses = mutableMapOf<String, Long>()
    val recDraws = mutableMapOf<String, Long>()
    val recDuration = mutableMapOf<String, Long>()
    entries["starchess.records"]?.let { raw ->
        runCatching {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val rec = arr.optJSONObject(i) ?: continue
                val gameId = rec.optString("game", "")
                if (gameId !in StarChessGameCatalog.SUPPORTED_IDS) continue
                when (rec.optString("result", "")) {
                    "win", "p1win" -> recWins[gameId] = (recWins[gameId] ?: 0L) + 1L
                    "lose", "p2win" -> recLosses[gameId] = (recLosses[gameId] ?: 0L) + 1L
                    else -> recDraws[gameId] = (recDraws[gameId] ?: 0L) + 1L
                }
                val p1 = rec.optLong("p1time", 0L).coerceAtLeast(0L)
                val p2 = rec.optLong("p2time", 0L).coerceAtLeast(0L)
                if (p1 + p2 > 0L) {
                    recDuration[gameId] = (recDuration[gameId] ?: 0L) + p1 + p2
                }
            }
        }
    }

    // 3. Live Android session play duration tracker per game
    entries["starchess.android.playDurationByGame"]?.let { raw ->
        runCatching {
            val obj = JSONObject(raw)
            val keys = obj.keys()
            while (keys.hasNext()) {
                val gameId = keys.next()
                if (gameId !in StarChessGameCatalog.SUPPORTED_IDS) continue
                val sec = obj.optLong(gameId, 0L).coerceAtLeast(0L)
                durationMap[gameId] = maxOf(durationMap[gameId] ?: 0L, sec)
            }
        }
    }

    val result = linkedMapOf<String, LocalGameStatSnapshot>()
    StarChessGameCatalog.SUPPORTED_IDS.forEach { gameId ->
        val w = maxOf(winsMap[gameId] ?: 0L, recWins[gameId] ?: 0L)
        val l = maxOf(lossesMap[gameId] ?: 0L, recLosses[gameId] ?: 0L)
        val d = maxOf(drawsMap[gameId] ?: 0L, recDraws[gameId] ?: 0L)
        val dur = maxOf(durationMap[gameId] ?: 0L, recDuration[gameId] ?: 0L)
        if (w > 0L || l > 0L || d > 0L || dur > 0L) {
            result[gameId] = LocalGameStatSnapshot(
                gameId = gameId,
                wins = w,
                losses = l,
                draws = d,
                playDurationSeconds = dur,
            )
        }
    }
    return result
}
