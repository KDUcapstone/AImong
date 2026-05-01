package com.kduniv.aimong.core.util

import android.annotation.SuppressLint
import android.graphics.LinearGradient
import android.graphics.Shader
import android.view.MotionEvent
import android.view.View
import android.widget.TextView

@SuppressLint("ClickableViewAccessibility")
fun View.setOnScaleTouchListener() {
    this.setOnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            }
        }
        false
    }
}

/**
 * TextView에 수평 그라데이션 효과를 적용합니다.
 */
fun TextView.setGradientText(vararg colors: Int) {
    this.post {
        try {
            val paint = this.paint
            val textStr = this.text.toString()
            if (textStr.isEmpty()) return@post
            
            val width = paint.measureText(textStr)
            if (width <= 0) return@post
            
            val shader = LinearGradient(
                0f, 0f, width, 0f,
                colors,
                null,
                Shader.TileMode.CLAMP
            )
            this.paint.shader = shader
            this.invalidate()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
