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
import com.kduniv.aimong.feature.mission.domain.model.normalizeToThreeLevels
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

    companion object {
        /** 경로에 남은 난이도 팝업을 모두 제거(인스턴스 분실·빠른 연타 대비) */
        fun dismissAllPopupsInPath(missionPath: ViewGroup) {
            for (i in missionPath.childCount - 1 downTo 0) {
                val child = missionPath.getChildAt(i)
                if (child.findViewById<View>(R.id.mission_diff_popup_root) != null) {
                    missionPath.removeViewAt(i)
                }
            }
        }
    }

    private var popupView: View? = null
    private var openMissionKey: String? = null
    private var heightAnimator: ValueAnimator? = null
    private var onPickedCallback: ((HomeQuizNavigation, DifficultyUnlockMode) -> Unit)? = null

    fun isShowing(): Boolean = popupView != null

    /** 같은 미션 노드를 다시 눌렀을 때 토글 닫기용 */
    fun isShowingForMission(missionKey: String): Boolean =
        popupView != null && !missionKey.isBlank() && openMissionKey == missionKey

    fun dismissImmediate() {
        heightAnimator?.cancel()
        heightAnimator = null
        popupView = null
        openMissionKey = null
        onPickedCallback = null
        clearOutsideDismiss()
        dismissAllPopupsInPath(binding.layoutMissionPath)
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
        unlockMode: DifficultyUnlockMode,
        anchorRow: View,
        missionKey: String,
        onPicked: (HomeQuizNavigation, DifficultyUnlockMode) -> Unit,
    ) {
        heightAnimator?.cancel()
        heightAnimator = null
        popupView = null
        openMissionKey = null
        onPickedCallback = null
        clearOutsideDismiss()
        dismissAllPopupsInPath(binding.layoutMissionPath)
        val parent = binding.layoutMissionPath
        val scroll = binding.scrollPath
        val idx = parent.indexOfChild(anchorRow)
        if (idx < 0) return

        openMissionKey = missionKey.takeIf { it.isNotBlank() }

        val popup = layoutInflater.inflate(R.layout.mission_difficulty_popup, parent, false)
        popup.findViewById<TextView>(R.id.tv_mission_title).text = missionTitle
        onPickedCallback = onPicked
        bindDifficultyCards(popup, base, starLevels, unlockMode)

        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )
        parent.addView(popup, idx + 1, lp)
        popupView = popup
        installOutsideDismiss()

        popup.post {
            val popupHeight = popup.height.coerceAtLeast(popup.measuredHeight)
            if (popupHeight > 0) {
                scrollToShowPopup(scroll, anchorRow, popupHeight)
            }
        }
    }

    fun updateStarLevels(
        starLevels: List<MissionStarLevel>,
        unlockMode: DifficultyUnlockMode,
        base: HomeQuizNavigation,
    ) {
        val popup = popupView ?: return
        bindDifficultyCards(popup, base, starLevels, unlockMode)
    }

    private fun bindDifficultyCards(
        popup: View,
        base: HomeQuizNavigation,
        starLevels: List<MissionStarLevel>,
        unlockMode: DifficultyUnlockMode,
    ) {
        val useMissionStars = base.missionId.isNotBlank()
        val onPicked = onPickedCallback ?: return
        val pick: (Int) -> Unit = { starLevel ->
            val nav = if (base.missionId.isNotBlank()) {
                // 난이도를 직접 고른 경우에는 추천 setId가 있어도 missionId+starLevel 진입을 우선한다.
                base.copy(entrySetId = "", starLevel = starLevel)
            } else {
                base.copy(starLevel = starLevel)
            }
            val resolvedMode = if (unlockMode == DifficultyUnlockMode.PER_STAR && useMissionStars) {
                starLevels.firstOrNull { it.starLevel == starLevel }?.resolveUnlockModeForPick()
                    ?: unlockMode
            } else {
                unlockMode
            }
            dismissImmediate()
            onPicked(nav, resolvedMode)
        }
        bindDifficultyCard(
            popup.findViewById(R.id.card_diff_1),
            starLevel = 1,
            starLevels = starLevels,
            useMissionStars = useMissionStars,
            unlockMode = unlockMode,
            onPick = pick,
        )
        bindDifficultyCard(
            popup.findViewById(R.id.card_diff_2),
            starLevel = 2,
            starLevels = starLevels,
            useMissionStars = useMissionStars,
            unlockMode = unlockMode,
            onPick = pick,
        )
        bindDifficultyCard(
            popup.findViewById(R.id.card_diff_3),
            starLevel = 3,
            starLevels = starLevels,
            useMissionStars = useMissionStars,
            unlockMode = unlockMode,
            onPick = pick,
        )
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
        unlockMode: DifficultyUnlockMode,
        onPick: (Int) -> Unit,
    ) {
        val materialCard = card as? MaterialCardView ?: return
        val levels = starLevels.normalizeToThreeLevels()
        val unlocked = if (!useMissionStars) {
            true
        } else {
            levels.isNotEmpty() && levels.isPickerUnlocked(starLevel, unlockMode)
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
