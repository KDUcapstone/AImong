package com.kduniv.aimong.core.ui

import android.graphics.Rect
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import com.kduniv.aimong.R

/**
 * 대상 뷰 주변만 밝게 보이도록 4방향 딤 + 링 + 안내 카드.
 * 딤 영역 터치는 소비하고, 대상 뷰는 그대로 클릭 가능.
 */
class TutorialCoachmarkOverlay(
    private val host: ViewGroup,
) {
    private var root: FrameLayout? = null
    private var tapTarget: View? = null
    private var scrollListener: ViewTreeObserver.OnScrollChangedListener? = null
    private val highlightPaddingPx =
        (12 * host.resources.displayMetrics.density).toInt()

    fun show(target: View, message: CharSequence) {
        dismiss()
        val density = host.resources.displayMetrics.density
        val overlayRoot = FrameLayout(host.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            )
            // 전체 루트가 clickable이면 하이라이트 구멍 터치도 여기서 소비되어 대상 버튼에 닿지 않음
            isClickable = false
            isFocusable = false
        }
        root = overlayRoot

        val dimColor = 0xB3000000.toInt()
        val top = FrameLayout(host.context).apply { setBackgroundColor(dimColor); isClickable = true }
        val bottom = FrameLayout(host.context).apply { setBackgroundColor(dimColor); isClickable = true }
        val left = FrameLayout(host.context).apply { setBackgroundColor(dimColor); isClickable = true }
        val right = FrameLayout(host.context).apply { setBackgroundColor(dimColor); isClickable = true }
        overlayRoot.addView(top)
        overlayRoot.addView(bottom)
        overlayRoot.addView(left)
        overlayRoot.addView(right)

        val ring = View(host.context).apply {
            setBackgroundResource(R.drawable.bg_tutorial_coachmark_ring)
            isClickable = false
            isFocusable = false
        }
        overlayRoot.addView(ring)

        val tapTargetView = View(host.context).apply {
            isClickable = true
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            setOnClickListener { target.performClick() }
        }
        tapTarget = tapTargetView
        overlayRoot.addView(tapTargetView)

        val pad = (18 * density).toInt()
        val padV = (14 * density).toInt()
        val messageCard = MaterialCardView(host.context).apply {
            radius = 16f * density
            cardElevation = 8f * density
            setCardBackgroundColor(host.context.getColor(R.color.child_quest_sheet_bg))
            strokeColor = host.context.getColor(R.color.child_streak_accent_stroke)
            strokeWidth = (1 * density).toInt()
        }
        val messageView = TextView(host.context).apply {
            text = message
            gravity = Gravity.CENTER
            setTextColor(host.context.getColor(R.color.child_quest_sheet_text_primary))
            textSize = 15f
            setPadding(pad, padV, pad, padV)
        }
        messageCard.addView(
            messageView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
        val cardLp = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        ).apply {
            gravity = Gravity.BOTTOM
            val margin = (20 * density).toInt()
            leftMargin = margin
            rightMargin = margin
            bottomMargin = (24 * density).toInt()
        }
        overlayRoot.addView(messageCard, cardLp)

        host.addView(overlayRoot)

        val reposition = {
            positionAroundTarget(target, top, bottom, left, right, ring, tapTargetView, messageCard)
        }
        target.post(reposition)
        scrollListener = ViewTreeObserver.OnScrollChangedListener { reposition() }
        host.viewTreeObserver.addOnScrollChangedListener(scrollListener)
    }

    fun dismiss() {
        scrollListener?.let { listener ->
            if (host.viewTreeObserver.isAlive) {
                host.viewTreeObserver.removeOnScrollChangedListener(listener)
            }
        }
        scrollListener = null
        root?.let { host.removeView(it) }
        root = null
        tapTarget = null
    }

    private fun positionAroundTarget(
        target: View,
        top: View,
        bottom: View,
        left: View,
        right: View,
        ring: View,
        tapTarget: View,
        messageCard: View,
    ) {
        if (!target.isShown) return
        val hostLoc = IntArray(2)
        val targetLoc = IntArray(2)
        host.getLocationOnScreen(hostLoc)
        target.getLocationOnScreen(targetLoc)
        val hostW = host.width
        val hostH = host.height
        if (hostW <= 0 || hostH <= 0) return

        val leftX = (targetLoc[0] - hostLoc[0] - highlightPaddingPx).coerceAtLeast(0)
        val topY = (targetLoc[1] - hostLoc[1] - highlightPaddingPx).coerceAtLeast(0)
        val rightX = (targetLoc[0] - hostLoc[0] + target.width + highlightPaddingPx).coerceAtMost(hostW)
        val bottomY = (targetLoc[1] - hostLoc[1] + target.height + highlightPaddingPx).coerceAtMost(hostH)

        top.layoutParams = FrameLayout.LayoutParams(hostW, topY)
        bottom.layoutParams = FrameLayout.LayoutParams(hostW, hostH - bottomY).apply {
            topMargin = bottomY
        }
        left.layoutParams = FrameLayout.LayoutParams(leftX, bottomY - topY).apply {
            topMargin = topY
        }
        right.layoutParams = FrameLayout.LayoutParams(hostW - rightX, bottomY - topY).apply {
            topMargin = topY
            leftMargin = rightX
        }

        val highlightWidth = rightX - leftX
        val highlightHeight = bottomY - topY
        ring.layoutParams = FrameLayout.LayoutParams(highlightWidth, highlightHeight).apply {
            leftMargin = leftX
            topMargin = topY
        }
        tapTarget.layoutParams = FrameLayout.LayoutParams(highlightWidth, highlightHeight).apply {
            leftMargin = leftX
            topMargin = topY
        }

        val density = host.resources.displayMetrics.density
        val cardLp = messageCard.layoutParams as FrameLayout.LayoutParams
        val cardH = messageCard.height.takeIf { it > 0 } ?: (80 * density).toInt()
        if (bottomY > hostH - cardH - cardLp.bottomMargin - (16 * density).toInt()) {
            cardLp.gravity = Gravity.TOP
            cardLp.topMargin = (topY - cardH - (12 * density).toInt())
                .coerceAtLeast((8 * density).toInt())
            cardLp.bottomMargin = 0
        } else {
            cardLp.gravity = Gravity.BOTTOM
            cardLp.topMargin = 0
            cardLp.bottomMargin = (24 * density).toInt()
        }
        messageCard.requestLayout()
    }
}
