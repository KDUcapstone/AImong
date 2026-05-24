package com.kduniv.aimong.feature.gacha

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 펫 PNG 에셋마다 투명 패딩·그림자·비대칭 꼬리로 [fitCenter]만으로는 시각 중심이 어긋남.
 * 1) 불투명 영역만 잘라내고 2) 알파 가중 질량 중심을 정사각 캔버스 한가운데에 맞춘다.
 */
internal object PetSpriteBitmap {

    /** 잘라내기 — 옅은 그림자·글로우는 제외 */
    private const val TRIM_ALPHA_THRESHOLD = 48

    /** 질량 중심 — 몸통 위주(알파 제곱 가중) */
    private const val CENTROID_ALPHA_THRESHOLD = 20

    fun normalizeForDisplay(source: Bitmap): Bitmap {
        val bounds = findOpaqueBounds(source, TRIM_ALPHA_THRESHOLD) ?: return source
        val content = crop(source, bounds)
        val squared = centerByVisualMass(
            content = content,
            config = source.config ?: Bitmap.Config.ARGB_8888,
        )
        if (content !== source && content !== squared) {
            content.recycle()
        }
        return squared
    }

    private class Bounds(
        val minX: Int,
        val minY: Int,
        val maxX: Int,
        val maxY: Int,
    ) {
        val width: Int get() = maxX - minX + 1
        val height: Int get() = maxY - minY + 1
    }

    private fun findOpaqueBounds(bitmap: Bitmap, alphaThreshold: Int): Bounds? {
        val width = bitmap.width
        val height = bitmap.height
        if (width == 0 || height == 0) return null

        var minX = width
        var minY = height
        var maxX = -1
        var maxY = -1

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (bitmap.getPixel(x, y) ushr 24 > alphaThreshold) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        if (maxX < minX || maxY < minY) return null
        return Bounds(minX, minY, maxX, maxY)
    }

    private fun crop(bitmap: Bitmap, bounds: Bounds): Bitmap {
        if (bounds.minX == 0 && bounds.minY == 0 &&
            bounds.width == bitmap.width && bounds.height == bitmap.height
        ) {
            return bitmap
        }
        return Bitmap.createBitmap(
            bitmap,
            bounds.minX,
            bounds.minY,
            bounds.width,
            bounds.height,
        )
    }

    /** 알파² 가중 centroid — 꼬리·그림자보다 몸통 쪽으로 중심이 잡힌다 */
    private fun computeVisualCentroid(bitmap: Bitmap): Pair<Float, Float> {
        val width = bitmap.width
        val height = bitmap.height
        var sumX = 0.0
        var sumY = 0.0
        var sumW = 0.0

        for (y in 0 until height) {
            for (x in 0 until width) {
                val alpha = bitmap.getPixel(x, y) ushr 24
                if (alpha <= CENTROID_ALPHA_THRESHOLD) continue
                val w = (alpha / 255.0) * (alpha / 255.0)
                sumX += x * w
                sumY += y * w
                sumW += w
            }
        }

        if (sumW <= 0.0) {
            return width / 2f to height / 2f
        }
        return (sumX / sumW).toFloat() to (sumY / sumW).toFloat()
    }

    private fun centerByVisualMass(content: Bitmap, config: Bitmap.Config): Bitmap {
        val (centroidX, centroidY) = computeVisualCentroid(content)
        val side = max(content.width, content.height).coerceAtLeast(1)
        val pad = (side * 0.04f).roundToInt().coerceAtLeast(1)
        val canvasSide = side + pad * 2

        val squared = Bitmap.createBitmap(canvasSide, canvasSide, config)
        val canvas = Canvas(squared)
        canvas.drawColor(Color.TRANSPARENT)

        val left = canvasSide / 2f - centroidX
        val top = canvasSide / 2f - centroidY
        canvas.drawBitmap(content, left, top, null)

        return squared
    }
}
