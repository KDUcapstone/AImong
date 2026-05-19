package com.kduniv.aimong.feature.gacha

import androidx.annotation.ColorInt
import com.kduniv.aimong.R
import com.kduniv.aimong.feature.gacha.data.model.FragmentGradeRow
import com.kduniv.aimong.feature.pet.data.model.PetDto

object GachaUiMapper {

    /** API `petName` 우선, 없으면 도감 표시명 */
    fun resolvePetDisplayName(petType: String, petName: String?, grade: String): String =
        petName?.trim()?.takeIf { it.isNotEmpty() }
            ?: GachaPetCatalog.displayNameFor(petType, grade)

    fun displayName(pet: PetDto): String =
        GachaPetCatalog.displayNameFor(pet.petType, pet.grade)

    fun petEmoji(pet: PetDto): String =
        GachaPetCatalog.emojiFor(pet.petType, pet.grade)

    fun displayLevel(pet: PetDto): String {
        val level = (pet.xp / 10).coerceAtLeast(1)
        return "Lv.$level"
    }

    fun gradeLabel(grade: String): String = when (grade.uppercase()) {
        "LEGEND", "LEGENDARY" -> "전설"
        "EPIC" -> "영웅"
        "RARE" -> "레어"
        else -> "일반"
    }

    @ColorInt
    fun rarityStrokeColorRes(grade: String): Int = when (grade.uppercase()) {
        "LEGEND", "LEGENDARY" -> R.color.gacha_rarity_legend
        "EPIC" -> R.color.gacha_rarity_epic
        "RARE" -> R.color.gacha_rarity_rare
        else -> R.color.gacha_rarity_normal
    }

    fun fragmentProgress(
        pet: PetDto,
        rows: List<FragmentGradeRow>
    ): Pair<Int, Int> {
        val row = rows.firstOrNull { it.grade.equals(pet.grade, ignoreCase = true) }
            ?: return 0 to defaultThreshold(pet.grade)
        val threshold = row.exchangeThreshold.coerceAtLeast(1)
        return row.count.coerceAtLeast(0) to threshold
    }

    fun fragmentProgressForGrade(
        grade: String,
        rows: List<FragmentGradeRow>
    ): Pair<Int, Int> {
        val row = rows.firstOrNull { it.grade.equals(grade, ignoreCase = true) }
        val threshold = row?.exchangeThreshold?.coerceAtLeast(1) ?: defaultThreshold(grade)
        return 0 to threshold
    }

    private fun defaultThreshold(grade: String): Int = when (grade.uppercase()) {
        "RARE" -> 30
        "EPIC" -> 80
        "LEGEND", "LEGENDARY" -> 200
        else -> 10
    }
}
