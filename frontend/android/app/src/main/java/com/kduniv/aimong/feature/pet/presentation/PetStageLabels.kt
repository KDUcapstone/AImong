package com.kduniv.aimong.feature.pet.presentation

import android.content.Context
import com.kduniv.aimong.R
import com.kduniv.aimong.feature.pet.domain.PetGrowthRules

/** 펫 성장 단계 UI 라벨 — 알 · 몽 · 아이몽 (Lv.1~3 대신) */
object PetStageLabels {

    fun label(context: Context, stage: String, xp: Int = 0): String =
        label(context, PetGrowthRules.resolveEffectiveStage(stage, xp))

    fun label(context: Context, stage: PetGrowthRules.PetStage): String =
        when (stage) {
            PetGrowthRules.PetStage.EGG -> context.getString(R.string.gacha_stage_egg)
            PetGrowthRules.PetStage.GROWTH -> context.getString(R.string.gacha_stage_growth)
            PetGrowthRules.PetStage.AIMONG -> context.getString(R.string.gacha_stage_aimong)
        }
}
