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
    fun bind(state: HomeUiState) {
        with(binding) {
            tvChipHeart.text = "❤️ ${state.heartCount}"
            tvChipXp.text = "⚡ ${state.topStatusXp}"
            tvChipTicket.text = "🎟 ${state.topTicketCount}"
            tvChipStreak.text = "🔥 ${state.streakDays}일"

            tvHomeSubtitle.text = "Lv.${state.userLevel} ${getProfileLabel(state.profileType)}"

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
        
        binding.containerMissionPath.setOnClickListener {
            binding.layoutFloatingTooltip.isVisible = false
        }
        binding.scrollPath.setOnTouchListener { _, _ ->
            binding.layoutFloatingTooltip.isVisible = false
            false
        }

        val density = binding.root.context.resources.displayMetrics.density
        val amplitude = 60f * density // 지그재그 진폭 설정

        for ((index, item) in items.withIndex()) {
            val rowLp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = if (index == 0) (8 * density).toInt() else (24 * density).toInt()
                bottomMargin = (24 * density).toInt()
            }
            
            val translation = (sin(index.toDouble() * Math.PI / 2) * amplitude).toFloat()

            when (item) {
                is HomePathItem.Completed -> {
                    val row = ViewHomePathNodeCompletedBinding.inflate(inflater, binding.layoutMissionPath, false)
                    row.btnNode.translationX = translation
                    row.btnNode.text = item.icon
                    row.btnNode.setOnClickListener {
                        showTooltip(row.btnNode, item.title, "완료됨", null)
                    }
                    binding.layoutMissionPath.addView(row.root, rowLp)
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
                    if (!row.lottiePet.isAnimating) row.lottiePet.playAnimation()
                    binding.layoutMissionPath.addView(row.root, rowLp)
                }
                is HomePathItem.Review -> {
                    val row = ViewHomePathNodeReviewBinding.inflate(inflater, binding.layoutMissionPath, false)
                    row.btnNode.translationX = translation
                    row.btnNode.setOnClickListener {
                        showTooltip(row.btnNode, "복습 미션", item.subtitle, item.missionId)
                    }
                    binding.layoutMissionPath.addView(row.root, rowLp)
                }
                is HomePathItem.Locked -> {
                    val row = ViewHomePathNodeLockedBinding.inflate(inflater, binding.layoutMissionPath, false)
                    row.btnNode.translationX = translation
                    row.btnNode.setOnClickListener {
                        showTooltip(row.btnNode, "잠김", item.hint, null)
                    }
                    binding.layoutMissionPath.addView(row.root, rowLp)
                }
            }
        }
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
                tooltip.y = nodeY - tooltip.height - (8 * nodeView.context.resources.displayMetrics.density)
            }
        }
    }
}
