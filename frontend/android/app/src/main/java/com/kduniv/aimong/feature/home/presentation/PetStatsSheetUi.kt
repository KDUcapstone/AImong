package com.kduniv.aimong.feature.home.presentation

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView
import com.kduniv.aimong.R
import com.kduniv.aimong.feature.gacha.PetArtAssets
import com.kduniv.aimong.feature.pet.domain.PetGrowthRules
import com.kduniv.aimong.feature.pet.presentation.PetStageLabels

/** 홈·목업 공통 — 펫 통계 바텀시트 바인딩 */
object PetStatsSheetUi {

    fun bind(
        root: View,
        state: HomeUiState,
        petNameFallback: String,
    ) {
        val ivSprite = root.findViewById<ImageView>(R.id.iv_pet_sprite)
        val tvEmoji = root.findViewById<TextView>(R.id.tv_pet_emoji)
        val tvName = root.findViewById<TextView>(R.id.tv_pet_name)
        val ivMood = root.findViewById<ImageView>(R.id.tv_pet_mood_icon)
        val tvStageChip = root.findViewById<TextView>(R.id.tv_pet_stage_chip)
        val cardMessage = root.findViewById<MaterialCardView>(R.id.card_pet_message)
        val tvMessage = root.findViewById<TextView>(R.id.tv_pet_message)
        val layoutXpCard = root.findViewById<LinearLayout>(R.id.layout_pet_xp_card)
        val tvLevel = root.findViewById<TextView>(R.id.tv_pet_level)
        val progress = root.findViewById<ProgressBar>(R.id.progress_pet_xp)
        val tvXp = root.findViewById<TextView>(R.id.tv_pet_xp_label)

        PetArtAssets.bindEquipped(
            image = ivSprite,
            emojiFallback = tvEmoji,
            petType = state.equippedPetType,
            stage = state.petStage,
            grade = state.equippedPetGrade,
        )
        HomePetMoodVisual.apply(ivSprite, tvEmoji, state.homeState)

        tvName.text = state.petName.ifBlank { petNameFallback }
        ivMood.isVisible = state.homeState == HomeState.SAD_LIGHT || state.homeState == HomeState.SAD_DEEP
        val effectiveStage = PetGrowthRules.resolveEffectiveStageString(state.petStage, state.petXp)
        tvStageChip.text = stageLabel(root, effectiveStage)

        val message = state.petMessage.trim()
        cardMessage.isVisible = message.isNotEmpty()
        tvMessage.text = message

        tvLevel.text = root.context.getString(
            R.string.home_pet_level_fmt,
            PetStageLabels.label(root.context, effectiveStage),
        )

        val showXp = state.showPetXpProgress
        layoutXpCard.isVisible = showXp
        if (showXp) {
            val maxXp = state.petMaxXp.coerceAtLeast(1)
            val pct = ((state.petXp.toFloat() / maxXp) * 100f).toInt().coerceIn(0, 100)
            progress.progress = pct
            tvXp.text = root.context.getString(R.string.home_pet_xp_fmt, state.petXp, maxXp)
        }
    }

    private fun stageLabel(root: View, stage: String): String =
        PetStageLabels.label(root.context, stage)
}
