package com.kduniv.aimong.feature.gacha

import android.content.Context
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.signature.ObjectKey
import com.kduniv.aimong.R
import java.util.concurrent.ConcurrentHashMap

/**
 * drawable-nodpi 의 `pet_{type}_{egg|growth|aimong}.png` 조회.
 * 없는 단계·오타 파일(`grwoth`)·접미사 없는 PNG 는 단계 폴백으로 처리.
 */
object PetArtAssets {

    /**
     * drawable-nodpi 가 아직 없는 petType → 동일 등급·비슷한 번호의 에셋으로 목업·도감 표시.
     * (예: pet_normal_001 → pet_normal_002)
     */
    private val ART_PET_TYPE_ALIASES = mapOf(
        "pet_normal_001" to "pet_normal_002",
        "pet_rare_002" to "pet_rare_001",
        "pet_epic_001" to "pet_epic_002",
        "pet_legend_001" to "pet_legend_002",
    )

    /** [ConcurrentHashMap] 은 null 값을 허용하지 않음 — 미발견은 0으로 캐시 */
    private const val DRAWABLE_CACHE_MISS = 0
    private val drawableResCache = ConcurrentHashMap<String, Int>()

    fun invalidateDrawableCache() {
        drawableResCache.clear()
    }

    fun resolveArtPetType(petType: String): String {
        val base = petType.trim().lowercase().replace('-', '_')
        if (base.isBlank()) return base
        return ART_PET_TYPE_ALIASES[base] ?: base
    }

    fun stageSuffix(stage: String?): String = when (stage?.trim()?.uppercase()) {
        "AIMONG", "ADULT", "MATURE", "FINAL" -> "aimong"
        "GROWTH", "HATCH", "BABY" -> "growth"
        else -> "egg"
    }

    fun drawableRes(
        context: Context,
        petType: String,
        stage: String?,
        allowStageFallback: Boolean = true,
    ): Int? {
        val base = resolveArtPetType(petType)
        if (base.isBlank()) return null
        val cacheKey = "$base|${stageSuffix(stage)}|fallback=$allowStageFallback"
        val cached = drawableResCache[cacheKey]
        if (cached != null) {
            return cached.takeIf { it != DRAWABLE_CACHE_MISS }
        }
        val resolved = if (allowStageFallback) {
            resolveDrawableResWithFallback(context, base, stage)
        } else {
            resolveDrawableResStrict(context, base, stage)
        } ?: DRAWABLE_CACHE_MISS
        drawableResCache[cacheKey] = resolved
        return resolved.takeIf { it != DRAWABLE_CACHE_MISS }
    }

    private fun resolveDrawableResStrict(context: Context, base: String, stage: String?): Int? {
        val suffix = stageSuffix(stage)
        resolveDrawableName(context, "${base}_$suffix")?.let { return it }
        return resolveDrawableName(context, base)
    }

    private fun resolveDrawableResWithFallback(context: Context, base: String, stage: String?): Int? {
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

    fun clearSprite(image: ImageView) {
        runCatching { Glide.with(image).clear(image) }
        image.setImageDrawable(null)
        image.setTag(R.id.pet_art_bind_key, null)
    }

    /** 모든 펫 스프라이트 ImageView 공통 — 프레임·리니어 부모 모두 중앙 정렬 */
    fun configureSpriteImageView(image: ImageView) {
        image.scaleType = ImageView.ScaleType.FIT_CENTER
        when (val lp = image.layoutParams) {
            is android.widget.FrameLayout.LayoutParams -> {
                lp.gravity = Gravity.CENTER
                image.layoutParams = lp
            }
            is android.widget.LinearLayout.LayoutParams -> {
                lp.gravity = Gravity.CENTER
                image.layoutParams = lp
            }
        }
    }

    private fun spriteTargetPx(image: ImageView): Int {
        val density = image.resources.displayMetrics.density
        val layoutH = image.layoutParams?.height?.takeIf { it > 0 }
        val measured = maxOf(image.width, image.height).takeIf { it > 0 }
        val px = when {
            measured != null && measured > 0 -> measured
            layoutH != null && layoutH != ViewGroup.LayoutParams.WRAP_CONTENT -> layoutH
            else -> (104 * density).toInt()
        }
        return (px * 1.15f).toInt().coerceIn(64, 320)
    }

    fun bindSprite(
        image: ImageView,
        emojiFallback: TextView,
        petType: String,
        stage: String?,
        emoji: String,
        allowStageFallback: Boolean = true,
    ) {
        val artType = resolveArtPetType(petType)
        val bindKey = "$artType|${stageSuffix(stage)}|fallback=$allowStageFallback|norm=v2"
        image.setTag(R.id.pet_art_bind_key, bindKey)
        clearSprite(image)
        configureSpriteImageView(image)

        val resId = drawableRes(image.context, petType, stage, allowStageFallback)
        if (resId != null) {
            emojiFallback.isVisible = false
            image.isVisible = true
            val px = spriteTargetPx(image)
            Glide.with(image)
                .load(resId)
                .signature(ObjectKey(bindKey))
                .override(px, px)
                .transform(PetSpriteNormalizeTransformation.INSTANCE)
                .fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                .into(image)
        } else {
            image.isVisible = false
            emojiFallback.text = emoji
            emojiFallback.isVisible = emoji.isNotBlank()
        }
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
            allowStageFallback = false,
        )
        lottie?.cancelAnimation()
        lottie?.isVisible = false
    }
}
