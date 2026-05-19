package com.kduniv.aimong.feature.gacha

import android.content.Context
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.airbnb.lottie.LottieAnimationView
import com.kduniv.aimong.R

/**
 * drawable-nodpi 의 `pet_{type}_{egg|growth|aimong}.png` 조회.
 * 없는 단계·오타 파일(`grwoth`)·접미사 없는 PNG 는 단계 폴백으로 처리.
 */
object PetArtAssets {

    fun stageSuffix(stage: String?): String = when (stage?.trim()?.uppercase()) {
        "AIMONG", "ADULT", "MATURE", "FINAL" -> "aimong"
        "GROWTH", "HATCH", "BABY" -> "growth"
        else -> "egg"
    }

    fun drawableRes(context: Context, petType: String, stage: String?): Int? {
        val base = petType.trim().lowercase().replace('-', '_')
        if (base.isBlank()) return null

        val primary = stageSuffix(stage)
        val suffixOrder = when (primary) {
            "aimong" -> listOf("aimong", "growth", "grwoth", "egg")
            "growth" -> listOf("growth", "grwoth", "egg", "aimong")
            else -> listOf("egg", "growth", "grwoth", "aimong")
        }

        for (suffix in suffixOrder) {
            resolveDrawableName(context, "${base}_$suffix")?.let { return it }
        }
        return resolveDrawableName(context, base)
    }

    private fun resolveDrawableName(context: Context, name: String): Int? {
        val id = context.resources.getIdentifier(name, "drawable", context.packageName)
        return id.takeIf { it != 0 }
    }

    fun emojiFor(petType: String, stage: String?, grade: String = "NORMAL"): String =
        if (petType.isNotBlank()) {
            GachaPetCatalog.emojiFor(petType, grade)
        } else {
            stageEmoji(stage)
        }

    fun stageEmoji(stage: String?): String = when (stage?.trim()?.uppercase()) {
        "EGG" -> "🥚"
        "HATCH", "BABY", "GROWTH" -> "🐣"
        "AIMONG", "ADULT", "MATURE", "FINAL" -> "✨"
        else -> "✨"
    }

    fun bindSprite(
        image: ImageView,
        emojiFallback: TextView,
        petType: String,
        stage: String?,
        @Suppress("UNUSED_PARAMETER") emoji: String,
    ) {
        val resId = drawableRes(image.context, petType, stage)
        if (resId != null) {
            image.setImageResource(resId)
        } else {
            image.setImageResource(R.drawable.ic_pet_placeholder)
        }
        image.isVisible = true
        emojiFallback.isVisible = false
    }

    fun bindEquipped(
        image: ImageView,
        emojiFallback: TextView,
        petType: String,
        stage: String?,
        grade: String = "NORMAL",
        lottie: LottieAnimationView? = null,
    ) {
        bindSprite(
            image = image,
            emojiFallback = emojiFallback,
            petType = petType,
            stage = stage,
            emoji = emojiFor(petType, stage, grade),
        )
        lottie?.cancelAnimation()
        lottie?.isVisible = false
    }
}
