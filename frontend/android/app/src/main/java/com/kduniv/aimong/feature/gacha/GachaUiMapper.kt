package com.kduniv.aimong.feature.gacha

import android.content.Context
import androidx.annotation.ColorInt
import com.kduniv.aimong.R
import com.kduniv.aimong.feature.pet.data.model.PetDto
import com.kduniv.aimong.feature.pet.domain.PetGrowthRules
import com.kduniv.aimong.feature.pet.presentation.PetStageLabels

object GachaUiMapper {

    /** 홈·도감·뽑기 연출 공통 — `petType` 기준 도감 표시명 (서버 `petName` 과 다를 수 있어 사용하지 않음) */
    fun resolvePetDisplayName(petType: String, petName: String?, grade: String): String =
        displayNameForPetType(petType, grade)

    fun displayName(pet: PetDto): String =
        displayNameForPetType(pet.petType, pet.grade)

    fun displayNameForPetType(petType: String, grade: String): String =
        GachaPetCatalog.displayNameFor(petType, grade)

    fun petEmoji(pet: PetDto): String =
        GachaPetCatalog.emojiFor(pet.petType, pet.grade)

    fun displayLevel(context: Context, pet: PetDto): String =
        PetStageLabels.label(context, pet.stage, pet.xp)

    /** 보유 펫 상세 — 등급 아래 단계·XP (아이몽은 단계만) */
    fun displayPetGrowthDetail(context: Context, pet: PetDto): String {
        val effectiveStage = PetGrowthRules.resolveEffectiveStageString(pet.stage, pet.xp)
        val stageLabel = PetStageLabels.label(context, effectiveStage)
        if (!PetGrowthRules.showsXpProgress(pet.stage, pet.xp)) {
            return stageLabel
        }
        val maxXp = PetGrowthRules.progressMaxXp(pet.grade, effectiveStage)
            ?: PetGrowthRules.EGG_EVOLUTION_XP
        return context.getString(
            R.string.gacha_pet_level_xp_fmt,
            stageLabel,
            pet.xp.coerceAtLeast(0),
            maxXp,
        )
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

    fun fragmentProgress(pet: PetDto, balance: GachaFragmentBalance): Pair<Int, Int> =
        balance.progressForExchange(pet.grade)

    fun fragmentProgressForGrade(grade: String, balance: GachaFragmentBalance): Pair<Int, Int> =
        balance.progressForExchange(grade)
}
