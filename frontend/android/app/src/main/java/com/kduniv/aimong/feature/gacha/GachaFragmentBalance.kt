package com.kduniv.aimong.feature.gacha

import com.kduniv.aimong.feature.gacha.data.model.FragmentGradeRow
import com.kduniv.aimong.feature.gacha.data.model.GachaFragmentsData

/**
 * 펫 조각은 등급·펫 종류와 무관한 **공통 보유량**이다.
 * 교환 시 해당 펫 등급의 [exchangeThreshold]만큼 공통 풀에서 차감한다.
 */
data class GachaFragmentBalance(
    val totalCount: Int,
    val thresholdsByGrade: Map<String, Int>,
) {
    fun thresholdFor(grade: String): Int =
        thresholdsByGrade[GachaPetCatalog.normalizeExchangeGrade(grade)]
            ?: defaultThreshold(grade)

    fun progressForExchange(grade: String): Pair<Int, Int> {
        val threshold = thresholdFor(grade).coerceAtLeast(1)
        return totalCount.coerceAtLeast(0) to threshold
    }

    fun canExchange(grade: String): Boolean = totalCount >= thresholdFor(grade)

    companion object {
        fun from(data: GachaFragmentsData?): GachaFragmentBalance {
            val rows = data?.fragments.orEmpty()
            val thresholds = buildThresholdMap(rows)
            val total = resolveTotalCount(data?.totalCount, rows)
            return GachaFragmentBalance(
                totalCount = total.coerceAtLeast(0),
                thresholdsByGrade = thresholds,
            )
        }

        private fun resolveTotalCount(
            apiTotal: Int?,
            rows: List<FragmentGradeRow>,
        ): Int {
            if (apiTotal != null) return apiTotal
            val pooled = rows.firstOrNull { row ->
                val g = row.grade.uppercase()
                g == "ALL" || g == "COMMON" || g == "TOTAL"
            }
            if (pooled != null) return pooled.count
            return rows
                .filter { row ->
                    row.grade.uppercase() !in setOf("ALL", "COMMON", "TOTAL")
                }
                .sumOf { it.count.coerceAtLeast(0) }
        }

        private fun buildThresholdMap(rows: List<FragmentGradeRow>): Map<String, Int> {
            val fromApi = rows
                .filter { row ->
                    val g = row.grade.uppercase()
                    g !in setOf("ALL", "COMMON", "TOTAL")
                }
                .associate {
                    GachaPetCatalog.normalizeExchangeGrade(it.grade) to
                        it.exchangeThreshold.coerceAtLeast(1)
                }
            if (fromApi.isNotEmpty()) return fromApi
            return defaultThresholds()
        }

        private fun defaultThresholds(): Map<String, Int> = mapOf(
            "NORMAL" to 10,
            "RARE" to 30,
            "EPIC" to 80,
            "LEGEND" to 200,
        )

        private fun defaultThreshold(grade: String): Int = when (GachaPetCatalog.normalizeExchangeGrade(grade)) {
            "RARE" -> 30
            "EPIC" -> 80
            "LEGEND" -> 200
            else -> 10
        }
    }
}
