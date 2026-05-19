package com.kduniv.aimong.feature.home.presentation

import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import com.kduniv.aimong.R
import com.kduniv.aimong.feature.gacha.PetArtAssets
import com.kduniv.aimong.feature.pet.domain.PetGrowthRules

/** 홈·목업 공통 — 펫 통계 바텀시트 바인딩 */
object PetStatsSheetUi {

    fun bind(
        root: View,
        state: HomeUiState,
        petNameFallback: String,
    ) {
        val ivSprite = root.findViewById<android.widget.ImageView>(R.id.iv_pet_sprite)
        val tvEmoji = root.findViewById<TextView>(R.id.tv_pet_emoji)
        val tvName = root.findViewById<TextView>(R.id.tv_pet_name)
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
        tvLevel.text = root.context.getString(R.string.home_pet_level_fmt, state.petLevel)

        val showXp = state.showPetXpProgress
        progress.isVisible = showXp
        tvXp.isVisible = showXp
        if (showXp) {
            val maxXp = state.petMaxXp.coerceAtLeast(1)
            val pct = ((state.petXp.toFloat() / maxXp) * 100f).toInt().coerceIn(0, 100)
            progress.progress = pct
            tvXp.text = root.context.getString(R.string.home_pet_xp_fmt, state.petXp, maxXp)
        } else if (state.petCrownUnlocked) {
            tvXp.isVisible = true
            tvXp.text = root.context.getString(R.string.home_pet_aimong_crown_hint)
        }
    }
}
