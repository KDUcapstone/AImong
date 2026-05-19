package com.kduniv.aimong.feature.gacha

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import kotlin.math.max

/**
 * 펫 PNG 에셋마다 투명 패딩이 달라 [fitCenter]만으로는 시각 중심이 어긋남.
 * 불투명 영역을 잘라 정사각 캔버스 중앙에 올려 모든 화면에서 동일하게 맞춘다.
 */
internal object PetSpriteBitmap {

    private const val ALPHA_THRESHOLD = 16

    fun normalizeForDisplay(source: Bitmap): Bitmap {
        val trimmed = trimTransparentBounds(source) ?: return source
        val squared = centerOnSquareCanvas(trimmed, source.config ?: Bitmap.Config.ARGB_8888)
        if (trimmed !== source && trimmed !== squared) {
            trimmed.recycle()
        }
        return squared
    }

    private fun trimTransparentBounds(bitmap: Bitmap): Bitmap? {
        val width = bitmap.width
        val height = bitmap.height
        if (width == 0 || height == 0) return null

        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (bitmap.getPixel(x, y) ushr 24 > ALPHA_THRESHOLD) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        if (maxX < minX || maxY < minY) return null

        val contentW = maxX - minX + 1
        val contentH = maxY - minY + 1
        if (contentW == width && contentH == height) return bitmap
        return Bitmap.createBitmap(bitmap, minX, minY, contentW, contentH)
    }

    private fun centerOnSquareCanvas(content: Bitmap, config: Bitmap.Config): Bitmap {
        val side = max(content.width, content.height).coerceAtLeast(1)
        if (content.width == side && content.height == side) return content

        val squared = Bitmap.createBitmap(side, side, config)
        val canvas = Canvas(squared)
        canvas.drawColor(Color.TRANSPARENT)
        val left = (side - content.width) / 2f
        val top = (side - content.height) / 2f
        canvas.drawBitmap(content, left, top, null)
        return squared
    }
}
