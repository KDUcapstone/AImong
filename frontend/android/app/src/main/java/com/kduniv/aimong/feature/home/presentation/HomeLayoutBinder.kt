package com.kduniv.aimong.feature.home.presentation

import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.core.view.isVisible
import com.kduniv.aimong.R
import com.kduniv.aimong.core.util.setOnScaleTouchListener
import com.kduniv.aimong.databinding.FragmentHomeBinding
import com.kduniv.aimong.databinding.ViewHomePathNodeCompletedBinding
import com.kduniv.aimong.databinding.ViewHomePathNodeLockedBinding
import com.kduniv.aimong.databinding.ViewHomePathNodeReviewBinding
import com.kduniv.aimong.databinding.ViewHomePathNodeStartBinding
import com.kduniv.aimong.databinding.ViewHomePathStageRewardChestBinding
import com.kduniv.aimong.feature.gacha.PetArtAssets
import androidx.core.content.ContextCompat
import kotlin.math.sin

/**
 * [FragmentHomeBinding] 갱신 로직 — [HomeFragment]와 목업 화면 공유.
 * 섬 배너(상단 고정) + 지그재그 미션 노드; 난이도는 스테이지 아래 인라인 팝업에서 선택 후 퀴즈로 이동.
 */
class HomeLayoutBinder(
    private val binding: FragmentHomeBinding,
    private val layoutInflater: LayoutInflater,
    private val onMissionPathWillRebuild: () -> Unit,
    private val onOpenDifficultyPicker: (String, HomeQuizNavigation, View, DifficultyUnlockMode) -> Unit,
    private val onNavigateToQuiz: (HomeQuizNavigation, DifficultyUnlockMode) -> Unit,
    private val onShowMissionHint: (String) -> Unit,
    private val onStageRewardChestClick: (StageRewardUi) -> Unit,
) {

    private var scrollHooked = false
    private var pathItemsForScroll: List<HomePathItem> = emptyList()
    private var lastPathStructureKey: String? = null
    private var lastPathStarsKey: String? = null
    private var lastTopChipsKey: String? = null
    private var lastPetVisualKey: String? = null
    private var lastQuestBadgeKey: String? = null

    fun bind(state: HomeUiState) {
        pathItemsForScroll = state.pathItems
        val topChipsKey = topChipsKey(state)
        if (topChipsKey != lastTopChipsKey) {
            bindTopChips(state)
            lastTopChipsKey = topChipsKey
        }
        val petVisualKey = petVisualKey(state)
        if (petVisualKey != lastPetVisualKey) {
            bindPetArea(state)
            lastPetVisualKey = petVisualKey
        }
        val questBadgeKey = questBadgeKey(state)
        if (questBadgeKey != lastQuestBadgeKey) {
            bindQuestBadge(state)
            lastQuestBadgeKey = questBadgeKey
        }
        bindMissionPath(state)
        with(binding) {
            firstSectionFromPath(state.pathItems)?.let { applyFloatingSection(it) }
            binding.root.post { updateFloatingSectionForScroll(binding.scrollPath.scrollY) }
            if (!scrollHooked) {
                scrollHooked = true
                binding.scrollPath.setOnScrollChangeListener { _, _, scrollY, _, _ ->
                    updateFloatingSectionForScroll(scrollY)
                }
            }
        }
    }

    private fun topChipsKey(state: HomeUiState): String =
        "${state.energyCurrent}/${state.energyMax}|${state.gearBalance}|${state.topStatusXp}|" +
            "${state.topTicketCount}|${state.streakDays}"

    private fun petVisualKey(state: HomeUiState): String =
        "${state.equippedPetType}|${state.petStage}|${state.equippedPetGrade}|${state.homeState}"

    private fun questBadgeKey(state: HomeUiState): String =
        "${state.shouldShowQuestFabBadge()}|${state.questNotificationCount()}"

    private fun bindTopChips(state: HomeUiState) {
        with(binding) {
            tvChipEnergy.text = "${state.energyCurrent}/${state.energyMax}"
            tvChipGear.text = state.gearBalance.toString()
            tvChipXp.text = state.topStatusXp.toString()
            tvChipTicket.text = state.topTicketCount.toString()
            tvChipStreak.text = root.context.getString(R.string.home_chip_streak_fmt, state.streakDays)
        }
    }

    private fun bindPetArea(state: HomeUiState) {
        with(binding) {
            PetArtAssets.bindEquipped(
                image = ivFloatPetSprite,
                emojiFallback = tvFloatPetEmoji,
                petType = state.equippedPetType,
                stage = state.petStage,
                grade = state.equippedPetGrade,
                lottie = lottiePetHome,
            )
            HomePetMoodVisual.apply(ivFloatPetSprite, tvFloatPetEmoji, state.homeState)
        }
    }

    private fun bindQuestBadge(state: HomeUiState) {
        val showQuestBadge = state.shouldShowQuestFabBadge()
        binding.tvQuestBadge.isVisible = showQuestBadge
        if (showQuestBadge) {
            binding.tvQuestBadge.text = state.questNotificationCount().coerceAtMost(99).toString()
        }
    }

    private fun bindMissionPath(state: HomeUiState) {
        val structureKey = state.pathItems.pathStructureKey()
        val starsKey = state.pathItems.pathStarsKey()
        val hasPath = binding.layoutMissionPath.childCount > 0
        val pathOutOfSync = state.pathItems.isNotEmpty() && !hasPath
        val popupOpen = MissionDifficultyPicker.isPopupOpenIn(binding.layoutMissionPath)

        if (popupOpen) {
            val structureUnchanged = structureKey == lastPathStructureKey && hasPath && !pathOutOfSync
            if (!structureUnchanged) {
                onMissionPathWillRebuild()
                renderMissionPath(state)
                lastPathStructureKey = structureKey
                lastPathStarsKey = starsKey
            } else if (starsKey != lastPathStarsKey) {
                patchPathStarLevels(state.pathItems)
                lastPathStarsKey = starsKey
            }
            return
        }

        val structureUnchanged = structureKey == lastPathStructureKey && hasPath && !pathOutOfSync
        val starsUnchanged = starsKey == lastPathStarsKey
        when {
            pathOutOfSync || !structureUnchanged -> {
                onMissionPathWillRebuild()
                renderMissionPath(state)
                lastPathStructureKey = structureKey
                lastPathStarsKey = starsKey
            }
            !starsUnchanged -> {
                patchPathStarLevels(state.pathItems)
                lastPathStarsKey = starsKey
            }
        }
    }

    private fun firstSectionFromPath(items: List<HomePathItem>): HomePathItem.SectionHeader? =
        items.firstOrNull { it is HomePathItem.SectionHeader } as? HomePathItem.SectionHeader

    private fun updateFloatingSectionForScroll(scrollY: Int) {
        val parent = binding.layoutMissionPath
        var cumulative = 0
        var chosen: HomePathItem.SectionHeader? = firstSectionFromPath(pathItemsForScroll)
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            if (child.visibility != View.VISIBLE) continue
            val lp = child.layoutParams as LinearLayout.LayoutParams
            val top = cumulative + lp.topMargin
            val bottom = top + child.height
            val tag = child.getTag(R.id.home_path_section_tag) as? HomePathItem.SectionHeader
            if (tag != null && top <= scrollY) {
                chosen = tag
            }
            cumulative = bottom + lp.bottomMargin
        }
        chosen?.let { applyFloatingSection(it) }
    }

    private fun applyFloatingSection(section: HomePathItem.SectionHeader) {
        binding.ivFloatingIslandIcon.setImageResource(section.islandIconRes)
        binding.tvFloatingSectionTitle.text = section.themeHint
        binding.tvFloatingSectionSubtitle.text = binding.root.context.getString(
            R.string.home_island_progress_fmt,
            section.progressCompleted,
            section.progressTotal
        )
        binding.tvFloatingSectionTheme.isVisible = false
        binding.layoutFloatingBannerInner.setBackgroundResource(section.bannerDrawableRes)
    }

    private fun patchPathStarLevels(items: List<HomePathItem>) {
        val starsByMission = mutableMapOf<String, Int>()
        items.forEach { item ->
            val missionId = item.missionIdForPath() ?: return@forEach
            when (item) {
                is HomePathItem.Completed -> starsByMission[missionId] = item.starsFilled
                is HomePathItem.TodayStart -> starsByMission[missionId] = item.starsFilled
                is HomePathItem.Start -> starsByMission[missionId] = item.starsFilled
                is HomePathItem.Review -> starsByMission[missionId] = item.starsFilled
                else -> Unit
            }
        }
        val parent = binding.layoutMissionPath
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val missionId = child.getTag(R.id.home_path_mission_id_tag) as? String ?: continue
            val newStars = starsByMission[missionId] ?: continue
            val oldStars = child.getTag(R.id.home_path_stars_tag) as? Int
            if (oldStars == newStars) continue
            child.setTag(R.id.home_path_stars_tag, newStars)
            child.findViewById<LinearLayout>(R.id.layout_stars)?.let { starsRow ->
                MissionPathUiHelper.bindStarRow(starsRow, newStars)
            }
        }
    }

    private fun tagMissionPathRow(row: View, missionId: String, starsFilled: Int) {
        if (missionId.isNotBlank()) {
            row.setTag(R.id.home_path_mission_id_tag, missionId)
            row.setTag(R.id.home_path_stars_tag, starsFilled)
        }
    }

    private fun renderMissionPath(state: HomeUiState) {
        onMissionPathWillRebuild()
        binding.layoutMissionPath.removeAllViews()
        lastPathStructureKey = state.pathItems.pathStructureKey()
        val inflater = layoutInflater
        val items = state.pathItems

        val density = binding.root.context.resources.displayMetrics.density
        val amplitude = 56f * density

        var nodeIndex = 0
        var sectionForRow: HomePathItem.SectionHeader? = null

        for (item in items) {
            val isHeader = item is HomePathItem.SectionHeader

            val rowLp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                if (isHeader) {
                    topMargin = (8 * density).toInt()
                    bottomMargin = (4 * density).toInt()
                } else if (item is HomePathItem.InterStageRewardChest) {
                    topMargin = (4 * density).toInt()
                    bottomMargin = (4 * density).toInt()
                } else {
                    topMargin = if (nodeIndex == 0) (8 * density).toInt() else (20 * density).toInt()
                    bottomMargin = (20 * density).toInt()
                }
            }

            if (isHeader) {
                nodeIndex = 0
                sectionForRow = item as HomePathItem.SectionHeader
                continue
            }

            if (item is HomePathItem.InterStageRewardChest) {
                addInterStageRewardChest(item, density)
                continue
            }

            val translation = (sin(nodeIndex.toDouble() * Math.PI / 2) * amplitude).toFloat()

            when (item) {
                is HomePathItem.Completed -> {
                    val row = ViewHomePathNodeCompletedBinding.inflate(inflater, binding.layoutMissionPath, false)
                    row.root.translationX = translation
                    row.btnNode.root.setBackgroundResource(R.drawable.bg_mission_node_circle)
                    MissionPathUiHelper.bindNodeIcon(row.btnNode.root, MissionPathUiHelper.ICON_PLAY)
                    MissionPathUiHelper.bindStarRow(row.layoutStars, item.starsFilled)
                    row.tvMissionCaption.text = item.title
                    val go = {
                        if (item.quizNav.entrySetId.isNotBlank() || item.quizNav.missionId.isNotBlank()) {
                            onOpenDifficultyPicker(
                                item.title,
                                item.quizNav,
                                row.root,
                                DifficultyUnlockMode.PER_STAR,
                            )
                        }
                    }
                    row.btnNode.root.setOnClickListener { go() }
                    row.tvMissionCaption.setOnClickListener { go() }
                    row.btnNode.root.setOnScaleTouchListener()
                    row.root.setTag(R.id.home_path_section_tag, sectionForRow)
                    tagMissionPathRow(row.root, item.missionId, item.starsFilled)
                    binding.layoutMissionPath.addView(row.root, rowLp)
                    nodeIndex++
                }
                is HomePathItem.TodayStart -> {
                    val row = ViewHomePathNodeStartBinding.inflate(inflater, binding.layoutMissionPath, false)
                    row.root.translationX = translation
                    row.btnNode.root.setBackgroundResource(R.drawable.bg_mission_node_start)
                    MissionPathUiHelper.bindNodeIcon(row.btnNode.root, MissionPathUiHelper.ICON_PLAY)
                    row.btnNode.root.alpha = 1f
                    MissionPathUiHelper.bindStarRow(row.layoutStars, item.starsFilled)
                    row.tvMissionCaption.text = item.missionTitle
                    val go = {
                        onOpenDifficultyPicker(
                            item.missionTitle,
                            item.quizNav,
                            row.root,
                            item.unlockMode,
                        )
                    }
                    row.btnNode.root.setOnClickListener { go() }
                    row.tvMissionCaption.setOnClickListener { go() }
                    row.btnNode.root.setOnScaleTouchListener()
                    row.root.setTag(R.id.home_path_section_tag, sectionForRow)
                    tagMissionPathRow(row.root, item.quizNav.missionId, item.starsFilled)
                    binding.layoutMissionPath.addView(row.root, rowLp)
                    nodeIndex++
                }
                is HomePathItem.Start -> {
                    val row = ViewHomePathNodeStartBinding.inflate(inflater, binding.layoutMissionPath, false)
                    row.root.translationX = translation
                    row.btnNode.root.setBackgroundResource(R.drawable.bg_mission_node_start)
                    MissionPathUiHelper.bindNodeIcon(row.btnNode.root, MissionPathUiHelper.ICON_PLAY)
                    row.btnNode.root.alpha = if (item.enabled) 1f else 0.5f
                    row.tvMissionCaption.text = item.missionTitle
                    MissionPathUiHelper.bindStarRow(row.layoutStars, item.starsFilled)
                    val go = {
                        if (!item.enabled) {
                            onShowMissionHint(binding.root.context.getString(R.string.home_today_mission_locked_hint))
                        } else {
                            onOpenDifficultyPicker(
                                item.missionTitle,
                                item.quizNav,
                                row.root,
                                DifficultyUnlockMode.NEW_PLAY,
                            )
                        }
                    }
                    row.btnNode.root.setOnClickListener { go() }
                    row.tvMissionCaption.setOnClickListener { go() }
                    row.btnNode.root.setOnScaleTouchListener()
                    row.root.setTag(R.id.home_path_section_tag, sectionForRow)
                    tagMissionPathRow(row.root, item.quizNav.missionId, item.starsFilled)
                    binding.layoutMissionPath.addView(row.root, rowLp)
                    nodeIndex++
                }
                is HomePathItem.Review -> {
                    val row = ViewHomePathNodeReviewBinding.inflate(inflater, binding.layoutMissionPath, false)
                    row.root.translationX = translation
                    row.btnNode.root.setBackgroundResource(R.drawable.bg_mission_node_circle)
                    MissionPathUiHelper.bindNodeIcon(row.btnNode.root, MissionPathUiHelper.ICON_REPLAY)
                    MissionPathUiHelper.bindStarRow(row.layoutStars, item.starsFilled)
                    row.tvMissionCaption.text = item.subtitle
                    val go = {
                        if (item.quizNav.entrySetId.isNotBlank() || item.quizNav.missionId.isNotBlank()) {
                            onOpenDifficultyPicker(
                                item.subtitle,
                                item.quizNav,
                                row.root,
                                DifficultyUnlockMode.REVIEW,
                            )
                        }
                    }
                    row.btnNode.root.setOnClickListener { go() }
                    row.tvMissionCaption.setOnClickListener { go() }
                    row.btnNode.root.setOnScaleTouchListener()
                    row.root.setTag(R.id.home_path_section_tag, sectionForRow)
                    tagMissionPathRow(row.root, item.quizNav.missionId, item.starsFilled)
                    binding.layoutMissionPath.addView(row.root, rowLp)
                    nodeIndex++
                }
                is HomePathItem.Locked -> {
                    val row = ViewHomePathNodeLockedBinding.inflate(inflater, binding.layoutMissionPath, false)
                    row.root.translationX = translation
                    row.btnNode.root.setBackgroundResource(R.drawable.bg_mission_node_locked)
                    val lockTint = ContextCompat.getColor(binding.root.context, R.color.child_quest_sheet_text_secondary)
                    MissionPathUiHelper.bindNodeIcon(row.btnNode.root, MissionPathUiHelper.ICON_LOCK, lockTint)
                    row.tvMissionCaption.text = item.hint
                    row.btnNode.root.setOnClickListener { onShowMissionHint(item.hint) }
                    row.tvMissionCaption.setOnClickListener { onShowMissionHint(item.hint) }
                    row.btnNode.root.setOnScaleTouchListener()
                    row.root.setTag(R.id.home_path_section_tag, sectionForRow)
                    binding.layoutMissionPath.addView(row.root, rowLp)
                    nodeIndex++
                }
                else -> Unit
            }
        }
    }

    private fun addInterStageRewardChest(item: HomePathItem.InterStageRewardChest, density: Float) {
        val row = ViewHomePathStageRewardChestBinding.inflate(layoutInflater, binding.layoutMissionPath, false)
        row.dotParentReward.isVisible = item.reward.hasParentPromise
        row.btnStageRewardChest.setOnClickListener { onStageRewardChestClick(item.reward) }
        row.btnStageRewardChest.setOnScaleTouchListener()
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin = (8 * density).toInt()
            bottomMargin = (8 * density).toInt()
        }
        binding.layoutMissionPath.addView(row.root, lp)
    }
}
