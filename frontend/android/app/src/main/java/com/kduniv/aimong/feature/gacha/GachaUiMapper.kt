package com.kduniv.aimong.feature.gacha

import android.content.Context
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
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

    /** 펫 상세 다이얼로그 — 단계 라벨 + XP 진행 바 */
    fun bindPetDetailGrowth(
        stageView: TextView,
        xpLayout: View,
        xpProgressBar: ProgressBar,
        xpLabelView: TextView,
        context: Context,
        pet: PetDto,
    ) {
        val effectiveStage = PetGrowthRules.resolveEffectiveStageString(pet.stage, pet.xp)
        stageView.isVisible = true
        stageView.text = PetStageLabels.label(context, effectiveStage)

        val showXp = PetGrowthRules.showsXpProgress(pet.stage, pet.xp)
        xpLayout.isVisible = showXp
        if (!showXp) return

        val maxXp = (
            PetGrowthRules.progressMaxXp(pet.grade, effectiveStage)
                ?: PetGrowthRules.EGG_EVOLUTION_XP
            ).coerceAtLeast(1)
        val current = pet.xp.coerceAtLeast(0)
        xpProgressBar.progress =
            ((current.toFloat() / maxXp) * 100f).toInt().coerceIn(0, 100)
        xpLabelView.text = context.getString(R.string.home_pet_xp_fmt, current, maxXp)
    }

    /** 도감 카드 — 보유 펫 XP 바 (미보유는 조각 바) */
    fun bindPetCardXp(
        xpProgressBar: ProgressBar,
        xpLabelView: TextView,
        context: Context,
        pet: PetDto,
    ) {
        val effectiveStage = PetGrowthRules.resolveEffectiveStageString(pet.stage, pet.xp)
        val showXp = PetGrowthRules.showsXpProgress(pet.stage, pet.xp)
        xpProgressBar.isVisible = showXp
        if (showXp) {
            val maxXp = (
                PetGrowthRules.progressMaxXp(pet.grade, effectiveStage)
                    ?: PetGrowthRules.EGG_EVOLUTION_XP
                ).coerceAtLeast(1)
            val current = pet.xp.coerceAtLeast(0)
            xpProgressBar.progress =
                ((current.toFloat() / maxXp) * 100f).toInt().coerceIn(0, 100)
            xpLabelView.text = context.getString(R.string.home_pet_xp_fmt, current, maxXp)
            xpLabelView.setTextColor(ContextCompat.getColor(context, R.color.gacha_pet_xp_progress_end))
        } else {
            xpLabelView.text = PetStageLabels.label(context, effectiveStage)
            xpLabelView.setTextColor(ContextCompat.getColor(context, R.color.quiz_text_secondary))
        }
    }

    /** 장착 슬롯 등 한 줄 요약 */
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
