package com.kduniv.aimong.feature.home.presentation

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.kduniv.aimong.R
import com.kduniv.aimong.databinding.FragmentHomeBinding
import com.kduniv.aimong.databinding.ViewHomePathNodeCompletedBinding
import com.kduniv.aimong.databinding.ViewHomePathNodeLockedBinding
import com.kduniv.aimong.databinding.ViewHomePathNodeReviewBinding
import com.kduniv.aimong.databinding.ViewHomePathNodeStartBinding
import com.kduniv.aimong.core.util.setOnScaleTouchListener
import kotlin.math.abs
import kotlin.math.sin

/**
 * [FragmentHomeBinding] 갱신 로직 — [HomeFragment]와 목업 화면 공유.
 * 로드맵 구조: 중앙 세로선을 [bg_path_pill]이 걸치고, 원 노드는 안쪽에 붙음.
 */
class HomeLayoutBinder(
    private val binding: FragmentHomeBinding,
    private val layoutInflater: LayoutInflater,
    private val getProfileLabel: (String) -> String,
    private val petNameDefault: String,
    private val onNavigateQuiz: (String) -> Unit,
    private val onSelectLearningTab: () -> Unit,
    private val onOpenQuest: () -> Unit
) {
    private var lastPathItems: List<HomePathItem> = emptyList()

    fun bind(state: HomeUiState) {
        with(binding) {
            tvChipHeart.text = "❤️ ${state.heartCount}"
            tvChipXp.text = "⚡ ${state.topStatusXp}"
            tvChipTicket.text = "🎟 ${state.topTicketCount}"
            tvChipStreak.text = "🔥 ${state.streakDays}일"

            // 우측 미니 퀘스트 버튼 (알림 표시용)
            btnMiniQuest.setOnClickListener {
                onOpenQuest()
            }

            // 배경 스크롤/터치 시 툴팁 숨기기
            scrollPath.setOnTouchListener { _, _ ->
                layoutFloatingTooltip.isVisible = false
                false
            }
            containerMissionPath.setOnClickListener {
                layoutFloatingTooltip.isVisible = false
            }

            renderMissionPath(state)
        }
    }

    private fun renderMissionPath(state: HomeUiState) {
        binding.layoutMissionPath.removeAllViews()
        binding.layoutFloatingTooltip.isVisible = false
        val inflater = layoutInflater
        val items = state.pathItems
        lastPathItems = items
        
        binding.containerMissionPath.setOnClickListener {
            binding.layoutFloatingTooltip.isVisible = false
        }
        binding.scrollPath.setOnTouchListener { _, _ ->
            binding.layoutFloatingTooltip.isVisible = false
            false
        }

        val density = binding.root.context.resources.displayMetrics.density
        val amplitude = 60f * density // 지그재그 진폭 설정

        var nodeIndex = 0
        var sectionForRow: HomePathItem.SectionHeader? = null

        for (item in items) {
            val isHeader = item is HomePathItem.SectionHeader
            
            val rowLp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                if (isHeader) {
                    topMargin = 0
                    bottomMargin = 0
                } else {
                    topMargin = if (nodeIndex == 0) (8 * density).toInt() else (24 * density).toInt()
                    bottomMargin = (24 * density).toInt()
                }
            }

            if (isHeader) {
                nodeIndex = 0
                sectionForRow = item as HomePathItem.SectionHeader
                continue
            }
            
            val translation = (sin(nodeIndex.toDouble() * Math.PI / 2) * amplitude).toFloat()

            when (item) {
                is HomePathItem.SectionHeader -> {
                    // 리스트 렌더링에서 제외
                }
                is HomePathItem.Completed -> {
                    val row = ViewHomePathNodeCompletedBinding.inflate(inflater, binding.layoutMissionPath, false)
                    row.btnNode.translationX = translation
                    row.btnNode.text = item.icon
                    row.btnNode.setOnClickListener {
                        showTooltip(row.btnNode, item.title, "완료됨", null)
                    }
                    row.btnNode.setOnScaleTouchListener()
                    row.root.setTag(R.id.home_path_section_tag, sectionForRow)
                    binding.layoutMissionPath.addView(row.root, rowLp)
                    nodeIndex++
                }
                is HomePathItem.TodayStart -> {
                    val row = ViewHomePathNodeStartBinding.inflate(inflater, binding.layoutMissionPath, false)
                    row.btnNode.translationX = translation
                    row.lottiePet.translationX = translation
                    
                    row.btnNode.text = item.icon
                    row.btnNode.alpha = if (item.enabled) 1f else 0.5f
                    row.btnNode.setOnClickListener {
                        showTooltip(row.btnNode, item.missionTitle, "시작하기", if (item.enabled) item.missionId else null)
                    }
                    row.btnNode.setOnScaleTouchListener()
                    if (!row.lottiePet.isAnimating) row.lottiePet.playAnimation()
                    row.root.setTag(R.id.home_path_section_tag, sectionForRow)
                    binding.layoutMissionPath.addView(row.root, rowLp)
                    nodeIndex++
                }
                is HomePathItem.Review -> {
                    val row = ViewHomePathNodeReviewBinding.inflate(inflater, binding.layoutMissionPath, false)
                    row.btnNode.translationX = translation
                    row.btnNode.setOnClickListener {
                        showTooltip(row.btnNode, "복습 미션", item.subtitle, item.missionId)
                    }
                    row.btnNode.setOnScaleTouchListener()
                    row.root.setTag(R.id.home_path_section_tag, sectionForRow)
                    binding.layoutMissionPath.addView(row.root, rowLp)
                    nodeIndex++
                }
                is HomePathItem.Locked -> {
                    val row = ViewHomePathNodeLockedBinding.inflate(inflater, binding.layoutMissionPath, false)
                    row.btnNode.translationX = translation
                    row.btnNode.setOnClickListener {
                        showTooltip(row.btnNode, "잠김", item.hint, null)
                    }
                    row.btnNode.setOnScaleTouchListener()
                    row.root.setTag(R.id.home_path_section_tag, sectionForRow)
                    binding.layoutMissionPath.addView(row.root, rowLp)
                    nodeIndex++
                }
            }
        }

        binding.scrollPath.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            syncSectionBannerForScroll(scrollY)
        }
        binding.scrollPath.post {
            syncSectionBannerForScroll(binding.scrollPath.scrollY)
        }
    }

    private fun syncSectionBannerForScroll(scrollY: Int) {
        val scroll = binding.scrollPath
        val path = binding.layoutMissionPath
        if (path.childCount == 0) {
            applyTopSectionBanner(lastPathItems.filterIsInstance<HomePathItem.SectionHeader>().firstOrNull())
            return
        }

        val viewportCenter = scrollY + scroll.height / 2
        var bestSection: HomePathItem.SectionHeader? = null
        var bestDist = Int.MAX_VALUE

        for (i in 0 until path.childCount) {
            val row = path.getChildAt(i)
            val section = row.getTag(R.id.home_path_section_tag) as? HomePathItem.SectionHeader ?: continue
            val rowCenterY = offsetTopInScrollContent(row) + row.height / 2
            val dist = abs(rowCenterY - viewportCenter)
            if (dist < bestDist) {
                bestDist = dist
                bestSection = section
            }
        }

        if (bestSection == null) {
            bestSection = lastPathItems.filterIsInstance<HomePathItem.SectionHeader>().firstOrNull()
        }
        applyTopSectionBanner(bestSection)
    }

    private fun applyTopSectionBanner(section: HomePathItem.SectionHeader?) {
        if (section == null) return
        binding.tvHomeBrand.text = "섹션 ${section.stage}"
        binding.tvHomeTitle.text = section.title
    }

    private fun offsetTopInScrollContent(view: View): Int {
        var y = 0
        var v: View? = view
        val anchor = binding.frameClickArea
        while (v != null && v != anchor) {
            y += v.top
            v = v.parent as? View
        }
        return y
    }

    private fun showTooltip(
        nodeView: View,
        title: String,
        subtitle: String?,
        missionId: String?
    ) {
        val tooltip = binding.layoutFloatingTooltip
        tooltip.isVisible = true
        binding.tvTooltipTitle.text = title
        
        if (subtitle != null) {
            binding.tvTooltipSubtitle.isVisible = true
            binding.tvTooltipSubtitle.text = subtitle
        } else {
            binding.tvTooltipSubtitle.isVisible = false
        }
        
        if (missionId != null) {
            binding.btnTooltipStart.isEnabled = true
            binding.btnTooltipStart.alpha = 1.0f
            binding.btnTooltipStart.setOnClickListener {
                onNavigateQuiz(missionId)
                tooltip.isVisible = false
            }
        } else {
            binding.btnTooltipStart.isEnabled = false
            binding.btnTooltipStart.alpha = 0.5f
            binding.btnTooltipStart.setOnClickListener(null)
        }

        // 화면 밖 스크롤 방지를 위해 post 사용
        nodeView.post {
            val loc = IntArray(2)
            nodeView.getLocationInWindow(loc)
            val containerLoc = IntArray(2)
            binding.containerMissionPath.getLocationInWindow(containerLoc)
            
            val nodeX = loc[0] - containerLoc[0]
            val nodeY = loc[1] - containerLoc[1]
            
            tooltip.post {
                tooltip.x = nodeX + nodeView.width / 2f - tooltip.width / 2f
                // 팝업 방향을 아래로 변경: 노드 Y + 노드 높이 + 8dp 여백
                tooltip.y = nodeY + nodeView.height + (8 * nodeView.context.resources.displayMetrics.density)
            }
        }
    }
}
