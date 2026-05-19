package com.kduniv.aimong.feature.gacha

import android.graphics.Bitmap
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import java.security.MessageDigest

/** Glide — 펫 스프라이트 투명 여백 제거 후 정사각 중앙 정렬 */
class PetSpriteNormalizeTransformation : BitmapTransformation() {

    private val idBytes = ID.toByteArray(Charsets.UTF_8)

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int,
    ): Bitmap {
        val normalized = PetSpriteBitmap.normalizeForDisplay(toTransform)
        if (normalized === toTransform) return toTransform

        val result = pool.get(normalized.width, normalized.height, Bitmap.Config.ARGB_8888)
            ?: Bitmap.createBitmap(normalized.width, normalized.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)
        canvas.drawColor(android.graphics.Color.TRANSPARENT)
        canvas.drawBitmap(normalized, 0f, 0f, null)
        if (normalized !== toTransform) {
            normalized.recycle()
        }
        return result
    }

    override fun equals(other: Any?): Boolean = other is PetSpriteNormalizeTransformation

    override fun hashCode(): Int = ID.hashCode()

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update(idBytes)
    }

    companion object {
        private const val ID = "com.kduniv.aimong.feature.gacha.PetSpriteNormalizeTransformation.v1"
        val INSTANCE = PetSpriteNormalizeTransformation()
    }
}
