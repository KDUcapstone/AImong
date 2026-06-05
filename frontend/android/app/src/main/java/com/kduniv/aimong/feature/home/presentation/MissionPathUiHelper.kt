package com.kduniv.aimong.feature.home.presentation

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.kduniv.aimong.R

object MissionPathUiHelper {

    @DrawableRes
    val ICON_PLAY: Int = R.drawable.ic_play_arrow_white

    @DrawableRes
    val ICON_REPLAY: Int = R.drawable.ic_replay

    @DrawableRes
    val ICON_LOCK: Int = R.drawable.ic_lock

    fun bindNodeIcon(
        root: View,
        @DrawableRes iconRes: Int,
        @ColorInt tint: Int = Color.WHITE,
    ) {
        root.findViewById<ImageView>(R.id.iv_node_icon)?.apply {
            setImageResource(iconRes)
            imageTintList = ColorStateList.valueOf(tint)
        }
    }

    fun bindStarRow(layout: LinearLayout, filled: Int) {
        layout.removeAllViews()
        val ctx = layout.context
        val density = ctx.resources.displayMetrics.density
        val size = (14f * density).toInt().coerceAtLeast(1)
        val gap = (3f * density).toInt()
        repeat(3) { index ->
            val iv = ImageView(ctx).apply {
                setImageResource(
                    if (index < filled) R.drawable.ic_star_filled else R.drawable.ic_star_outline,
                )
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    if (index < 2) marginEnd = gap
                }
            }
            layout.addView(iv)
        }
    }
}
