package com.kduniv.aimong.feature.home.presentation

import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Rect
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.NestedScrollView
import com.google.android.material.card.MaterialCardView
import com.kduniv.aimong.feature.mission.domain.model.MissionStarLevel
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import com.kduniv.aimong.R
import com.kduniv.aimong.databinding.FragmentHomeBinding

/**
 * 스테이지 행 바로 아래에 삽입되는 난이도 선택 — 높이 펼침 애니메이션으로 아래 스테이지를 밀어 올림.
 * 스크롤 영역·상단 칩·섹션·FAB 밖을 누르면 접힘.
 */
class MissionDifficultyPicker(
    private val binding: FragmentHomeBinding,
    private val layoutInflater: LayoutInflater,
) {

    private var popupView: View? = null
    private var heightAnimator: ValueAnimator? = null

    fun isShowing(): Boolean = popupView != null

    fun dismissImmediate() {
        heightAnimator?.cancel()
        heightAnimator = null
        val v = popupView
        popupView = null
        clearOutsideDismiss()
        v?.let { child ->
            val parent = child.parent as? ViewGroup ?: return@let
            parent.removeView(child)
        }
    }

    fun dismissAnimated() {
        val v = popupView ?: return
        val lp = v.layoutParams as? LinearLayout.LayoutParams ?: return
        val startH = v.height.coerceAtLeast(lp.height)
        if (startH <= 0) {
            dismissImmediate()
            return
        }
        heightAnimator?.cancel()
        heightAnimator = ValueAnimator.ofInt(startH, 0).apply {
            duration = 180
            interpolator = FastOutSlowInInterpolator()
            addUpdateListener { a ->
                lp.height = a.animatedValue as Int
                v.layoutParams = lp
                (v.parent as? View)?.requestLayout()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    dismissImmediate()
                }
            })
            start()
        }
    }

    fun show(
        missionTitle: String,
        base: HomeQuizNavigation,
        starLevels: List<MissionStarLevel>,
        anchorRow: View,
        onPicked: (HomeQuizNavigation) -> Unit,
    ) {
        dismissImmediate()
        val parent = binding.layoutMissionPath
        val scroll = binding.scrollPath
        val idx = parent.indexOfChild(anchorRow)
        if (idx < 0) return

        val popup = layoutInflater.inflate(R.layout.mission_difficulty_popup, parent, false)
        popup.findViewById<TextView>(R.id.tv_mission_title).text = missionTitle

        val pick: (Int) -> Unit = { starLevel ->
            val nav = if (base.entrySetId.isNotBlank()) {
                base
            } else {
                base.copy(starLevel = starLevel, missionId = base.missionId)
            }
            dismissImmediate()
            onPicked(nav)
        }
        val useMissionStars = base.entrySetId.isBlank() && base.missionId.isNotBlank()
        bindDifficultyCard(
            popup.findViewById(R.id.card_diff_1),
            starLevel = 1,
            starLevels = starLevels,
            useMissionStars = useMissionStars,
            onPick = pick,
        )
        bindDifficultyCard(
            popup.findViewById(R.id.card_diff_2),
            starLevel = 2,
            starLevels = starLevels,
            useMissionStars = useMissionStars,
            onPick = pick,
        )
        bindDifficultyCard(
            popup.findViewById(R.id.card_diff_3),
            starLevel = 3,
            starLevels = starLevels,
            useMissionStars = useMissionStars,
            onPick = pick,
        )

        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            0,
        )
        parent.addView(popup, idx + 1, lp)
        popupView = popup

        val inner = popup.findViewById<View>(R.id.popup_difficulty_inner)

        popup.post {
            val w = parent.width
            popup.measure(
                MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED),
            )
            val targetH = popup.measuredHeight.coerceAtLeast(1)

            inner.pivotX = popup.width / 2f
            inner.pivotY = 0f
            inner.scaleX = 0.94f
            inner.scaleY = 0.94f

            scroll.post { scrollToShowPopup(scroll, anchorRow, targetH) }

            heightAnimator?.cancel()
            heightAnimator = ValueAnimator.ofInt(0, targetH).apply {
                duration = 260
                interpolator = FastOutSlowInInterpolator()
                addUpdateListener { a ->
                    lp.height = a.animatedValue as Int
                    popup.layoutParams = lp
                    parent.requestLayout()
                }
                start()
            }
            inner.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(260)
                .setInterpolator(FastOutSlowInInterpolator())
                .start()

            installOutsideDismiss()
        }
    }

    private fun installOutsideDismiss() {
        clearOutsideDismiss()
        val pathScroll = binding.scrollPath as? MissionPathNestedScrollView
        if (pathScroll != null) {
            pathScroll.popupBoundsInScroll = {
                popupView?.let { p -> rectRelativeToAncestor(p, pathScroll) }
            }
            pathScroll.onOutsidePopupAction = {
                if (popupView != null) dismissAnimated()
            }
        }

        val topChromeTouch = View.OnTouchListener { _, e ->
            if (popupView == null) return@OnTouchListener false
            if (e.actionMasked == MotionEvent.ACTION_DOWN) {
                dismissAnimated()
                return@OnTouchListener true
            }
            false
        }
        binding.scrollTopChips.setOnTouchListener(topChromeTouch)
        binding.cardFloatingSection.setOnTouchListener(topChromeTouch)
        binding.cardFloatPet.setOnTouchListener(topChromeTouch)
        binding.fabChildQuest.setOnTouchListener(topChromeTouch)
    }

    private fun clearOutsideDismiss() {
        (binding.scrollPath as? MissionPathNestedScrollView)?.apply {
            popupBoundsInScroll = null
            onOutsidePopupAction = null
        }
        binding.scrollTopChips.setOnTouchListener(null)
        binding.cardFloatingSection.setOnTouchListener(null)
        binding.cardFloatPet.setOnTouchListener(null)
        binding.fabChildQuest.setOnTouchListener(null)
    }

    private fun rectRelativeToAncestor(view: View, ancestor: ViewGroup): Rect {
        var l = 0
        var t = 0
        var v: View? = view
        while (v != null && v !== ancestor) {
            l += v.left
            t += v.top
            v = v.parent as? View
        }
        return Rect(l, t, l + view.width, t + view.height)
    }

    private fun bindDifficultyCard(
        card: View,
        starLevel: Int,
        starLevels: List<MissionStarLevel>,
        useMissionStars: Boolean,
        onPick: (Int) -> Unit,
    ) {
        val materialCard = card as? MaterialCardView ?: return
        val unlocked = if (!useMissionStars) {
            true
        } else {
            val sl = starLevels.firstOrNull { it.starLevel == starLevel }
            when {
                starLevels.isEmpty() -> starLevel == 1
                sl != null -> sl.isPlayable || sl.isReviewable
                else -> false
            }
        }
        materialCard.alpha = if (unlocked) 1f else 0.42f
        materialCard.isClickable = true
        materialCard.isFocusable = true
        materialCard.setOnClickListener {
            if (unlocked) {
                onPick(starLevel)
            } else {
                Toast.makeText(
                    card.context,
                    R.string.home_difficulty_locked,
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    private fun scrollToShowPopup(scroll: NestedScrollView, anchor: View, popupHeight: Int) {
        val content = scroll.getChildAt(0) ?: return
        var y = 0
        var v: View? = anchor
        while (v != null && v !== content) {
            y += v.top
            v = v.parent as? View
        }
        if (v !== content) return
        val anchorBottomInContent = y + anchor.height
        val pad = (16 * scroll.resources.displayMetrics.density).toInt()
        val viewport = scroll.height - scroll.paddingTop - scroll.paddingBottom
        val visibleBottom = scroll.scrollY + viewport
        val needBottom = anchorBottomInContent + popupHeight + pad
        val delta = needBottom - visibleBottom
        if (delta > 0) {
            scroll.smoothScrollBy(0, delta)
        }
    }
}
