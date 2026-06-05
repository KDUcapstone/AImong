package com.kduniv.aimong.feature.gacha

/**
 * 뽑기 횟수 구간별 기본 확률표 (NORMAL 티켓, 기능 명세 9-3).
 * 표시 등급: 일반 · 레어 · 영웅(EPIC) · 전설(LEGEND).
 */
object GachaProbabilityTable {

    const val MIN_LEVEL = 1
    const val MAX_LEVEL = 4

    data class Tier(
        val grade: String,
        val label: String,
        /** 0~100, 소수 첫째 자리까지 표시 가능 */
        val percent: Double,
    )

    data class Level(
        val level: Int,
        val pullRangeLabel: String,
        val tiers: List<Tier>,
    )

    private val levels: List<Level> = listOf(
        Level(
            level = 1,
            pullRangeLabel = "0~19회",
            tiers = listOf(
                Tier("NORMAL", "일반", 75.0),
                Tier("RARE", "레어", 21.0),
                Tier("EPIC", "영웅", 3.5),
                Tier("LEGEND", "전설", 0.5),
            ),
        ),
        Level(
            level = 2,
            pullRangeLabel = "20~49회",
            tiers = listOf(
                Tier("NORMAL", "일반", 66.0),
                Tier("RARE", "레어", 24.0),
                Tier("EPIC", "영웅", 7.5),
                Tier("LEGEND", "전설", 2.5),
            ),
        ),
        Level(
            level = 3,
            pullRangeLabel = "50~99회",
            tiers = listOf(
                Tier("NORMAL", "일반", 56.0),
                Tier("RARE", "레어", 27.0),
                Tier("EPIC", "영웅", 13.0),
                Tier("LEGEND", "전설", 4.0),
            ),
        ),
        Level(
            level = 4,
            pullRangeLabel = "100회+",
            tiers = listOf(
                Tier("NORMAL", "일반", 44.0),
                Tier("RARE", "레어", 29.0),
                Tier("EPIC", "영웅", 22.0),
                Tier("LEGEND", "전설", 5.0),
            ),
        ),
    )

    fun levelFromPullCount(pullCount: Int): Int = when {
        pullCount >= 100 -> 4
        pullCount >= 50 -> 3
        pullCount >= 20 -> 2
        else -> 1
    }.coerceIn(MIN_LEVEL, MAX_LEVEL)

    fun levelData(level: Int): Level =
        levels.firstOrNull { it.level == level.coerceIn(MIN_LEVEL, MAX_LEVEL) }
            ?: levels.first()

    fun trendDelta(level: Int, grade: String): Double? {
        if (level <= MIN_LEVEL) return null
        val cur = tierPercent(level, grade) ?: return null
        val prev = tierPercent(level - 1, grade) ?: return null
        return cur - prev
    }

    private fun tierPercent(level: Int, grade: String): Double? =
        levelData(level).tiers.firstOrNull { it.grade == grade }?.percent

    fun formatPercent(value: Double): String =
        if (value == value.toLong().toDouble()) "${value.toLong()}%"
        else {
            val oneDecimal = (kotlin.math.round(value * 10) / 10.0)
            if (oneDecimal == oneDecimal.toLong().toDouble()) "${oneDecimal.toLong()}%"
            else "$oneDecimal%"
        }

    data class LevelProgress(
        val level: Int,
        /** 현재 레벨 구간에서 채운 뽑기 횟수 */
        val currentInLevel: Int,
        /** 다음 레벨까지 필요한 구간 길이 */
        val requiredInLevel: Int,
        val isMaxLevel: Boolean,
    )

    fun levelProgress(pullCount: Int): LevelProgress {
        val safe = pullCount.coerceAtLeast(0)
        val level = levelFromPullCount(safe)
        val rangeStart = when (level) {
            1 -> 0
            2 -> 20
            3 -> 50
            else -> 100
        }
        val rangeEnd = when (level) {
            1 -> 20
            2 -> 50
            3 -> 100
            else -> null
        }
        if (rangeEnd == null) {
            return LevelProgress(
                level = level,
                currentInLevel = safe - rangeStart,
                requiredInLevel = 0,
                isMaxLevel = true,
            )
        }
        return LevelProgress(
            level = level,
            currentInLevel = safe - rangeStart,
            requiredInLevel = rangeEnd - rangeStart,
            isMaxLevel = false,
        )
    }
}
