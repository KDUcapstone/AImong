package com.kduniv.aimong.feature.parent.presentation

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.kduniv.aimong.R
import com.kduniv.aimong.core.network.model.ParentChildDetailData
import com.kduniv.aimong.core.network.model.ParentChildItem
import com.kduniv.aimong.core.network.model.ParentRegisterResponse
import com.kduniv.aimong.core.network.model.PatchParentChildRequest
import com.kduniv.aimong.feature.auth.presentation.ChildRegisterSuccessBottomSheet
import com.kduniv.aimong.feature.parent.domain.ParentAuthPolicy
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.core.util.UiPerfLog
import com.kduniv.aimong.databinding.FragmentParentDashboardBinding
import com.kduniv.aimong.feature.parent.data.model.ParentChildSummaryResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentCustomQuestDto
import com.kduniv.aimong.feature.parent.data.model.ParentStageRewardDto
import com.kduniv.aimong.feature.parent.data.model.ParentWeakPointsResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentWeeklyStatsResponseData
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

private data class PastQuestUiState(
    val quests: List<ParentCustomQuestDto>,
    val expanded: Boolean,
    val hasNext: Boolean,
    val isLoading: Boolean
)

private enum class QuestExpiresPreset {
    TODAY,
    WEEK,
    CUSTOM,
}

@AndroidEntryPoint
class ParentDashboardFragment :
    BaseFragment<FragmentParentDashboardBinding>(FragmentParentDashboardBinding::inflate),
    ParentChildSelectBottomSheet.Listener {

    private val viewModel: ParentDashboardViewModel by viewModels()

    private fun titleForParentDashboard(nickname: String?): String {
        val n = nickname?.trim().orEmpty()
        return if (n.isNotEmpty()) getString(R.string.parent_dashboard_title_with_nickname, n)
        else getString(R.string.parent_dashboard_title_default)
    }

    private var latestChildren: List<ParentChildItem> = emptyList()
    private var latestSelectedChildId: String? = null
    private var latestChildDetail: ParentChildDetailData? = null
    private var parentDashboardPerfMark: Long? = null
    private var isSelectedChildLinked: Boolean = false

    private val questExpiresDisplayFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")

    private var questExpiresDatePicker: DatePickerDialog? = null
    private var questExpiresTimePicker: TimePickerDialog? = null

    private fun updateDashboardTitle() {
        val childNickname = latestSelectedChildId
            ?.let { id -> latestChildren.firstOrNull { it.childId == id }?.nickname }
        binding.tvBrandTitle.text = titleForParentDashboard(childNickname)
    }

    private fun updateRichChildLabel() {
        val id = latestSelectedChildId
        val child = id?.let { cid -> latestChildren.firstOrNull { it.childId == cid } }
        val nick = child?.nickname?.trim()?.takeIf { it.isNotEmpty() }
            ?: latestChildren.firstOrNull()?.nickname?.trim()?.takeIf { it.isNotEmpty() }
        binding.includeDashboardRich.tvDashboardSelectedChild.text =
            nick ?: getString(R.string.parent_dashboard_child_select_placeholder)

        val code = resolveChildLoginCode(id, child)
        val rich = binding.includeDashboardRich
        val showCode = id != null && (code.isNotBlank() || !isSelectedChildLinked)
        if (showCode && code.isNotBlank()) {
            rich.tvDashboardChildCode.text = code
            rich.layoutDashboardChildCode.visibility = View.VISIBLE
        } else if (showCode) {
            rich.tvDashboardChildCode.text = "—"
            rich.layoutDashboardChildCode.visibility = View.VISIBLE
        } else {
            rich.layoutDashboardChildCode.visibility = View.GONE
        }
    }

    private fun isChildLinked(detail: ParentChildDetailData?): Boolean =
        !detail?.lastActiveAt.isNullOrBlank()

    private fun applyChildLinkedState(linked: Boolean) {
        isSelectedChildLinked = linked
        val rich = binding.includeDashboardRich
        rich.cardDashboardNotLinked.visibility = if (linked) View.GONE else View.VISIBLE
        rich.layoutDashboardLinkedBody.alpha = if (linked) 1f else 0.45f
        rich.btnParentAddCustomQuest.isEnabled = linked
        rich.btnParentAddCustomQuest.alpha = if (linked) 1f else 0.45f
        if (!linked) {
            clearLinkedDashboardContent()
        }
    }

    private fun clearLinkedDashboardContent() {
        applyRichSummary(null)
        applyRichWeekly(null)
        applyRichWeak(null)
        applyCustomQuests(emptyList())
        applyPastCustomQuests(emptyList(), expanded = false, hasNext = false, isLoading = false)
        applyStageRewards(emptyList())
        val rich = binding.includeDashboardRich
        rich.layoutDashboardRecentRows.removeAllViews()
        rich.tvParentDashRecentEmpty.visibility = View.VISIBLE
    }

    private fun resolveChildLoginCode(childId: String?, child: ParentChildItem?): String {
        val fromDetail = latestChildDetail
            ?.takeIf { it.childId == childId }
            ?.code
            ?.trim()
            .orEmpty()
        if (fromDetail.isNotBlank()) return fromDetail
        return child?.code?.trim().orEmpty()
    }

    override fun initView() {
        val rich = binding.includeDashboardRich
        binding.swipeParentDashboardRefresh.setColorSchemeColors(
            ContextCompat.getColor(requireContext(), R.color.parent_mock_indigo),
            ContextCompat.getColor(requireContext(), R.color.parent_mock_green),
        )
        binding.swipeParentDashboardRefresh.setOnRefreshListener {
            viewModel.refreshSelectedDashboardFromPull()
        }

        rich.rowDashboardChildSelector.setOnClickListener { showChildSelectSheet() }
        binding.cardEmptyChildren.setOnClickListener { showChildSelectSheet() }

        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_parentDashboardFragment_to_parentSettingsFragment)
        }

        rich.btnParentAddCustomQuest.setOnClickListener {
            if (!isSelectedChildLinked) {
                Snackbar.make(binding.root, R.string.parent_dashboard_not_linked_title, Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showCreateCustomQuestDialog()
        }
        rich.btnParentTogglePastQuests.setOnClickListener {
            if (!isSelectedChildLinked) {
                Snackbar.make(binding.root, R.string.parent_dashboard_not_linked_title, Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            viewModel.togglePastCustomQuests()
        }
        rich.btnParentLoadMorePastQuests.setOnClickListener {
            viewModel.loadMorePastCustomQuests()
        }
    }

    override fun onResume() {
        super.onResume()
        parentDashboardPerfMark = UiPerfLog.mark("parent_dashboard_first_paint")
        viewModel.refreshCustomQuestsOnResume()
    }

    override fun onDestroyView() {
        dismissQuestExpiresPickers()
        super.onDestroyView()
    }

    private fun showChildSelectSheet() {
        ParentChildSelectBottomSheet.newInstance(
            onChildLongPress = { showChildManageDialog(it) }
        ).show(childFragmentManager, ParentChildSelectBottomSheet.TAG)
    }

    override fun onAddChildRequested() {
        showAddChildDialog()
    }

    private fun showAddChildDialog() {
        if (latestChildren.size >= ParentAuthPolicy.MAX_CHILDREN) {
            Snackbar.make(
                binding.root,
                getString(R.string.parent_child_limit_exceeded),
                Snackbar.LENGTH_LONG
            ).show()
            return
        }
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.parent_add_child_hint)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.parent_add_child_dialog_title)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = input.text?.toString()?.trim().orEmpty()
                when {
                    name.isBlank() ->
                        Snackbar.make(binding.root, R.string.auth_error_nickname_empty, Snackbar.LENGTH_SHORT).show()
                    name.length > 20 ->
                        Snackbar.make(binding.root, R.string.auth_error_nickname_length, Snackbar.LENGTH_SHORT).show()
                    else -> viewModel.addChild(name)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showChildManageDialog(child: ParentChildItem) {
        val options = arrayOf(
            getString(R.string.parent_child_manage_edit_nickname),
            getString(R.string.parent_child_manage_regenerate_code),
            getString(R.string.parent_child_manage_delete)
        )
        AlertDialog.Builder(requireContext())
            .setTitle(child.nickname)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditNicknameDialog(child)
                    1 -> confirmRegenerateCode(child.childId)
                    2 -> confirmDeleteChild(child)
                }
            }
            .show()
    }

    private fun showEditNicknameDialog(child: ParentChildItem) {
        val input = EditText(requireContext()).apply {
            setText(child.nickname)
            hint = getString(R.string.parent_add_child_hint)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.parent_child_manage_edit_nickname)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = input.text?.toString()?.trim().orEmpty()
                when {
                    name.isBlank() ->
                        Snackbar.make(binding.root, R.string.auth_error_nickname_empty, Snackbar.LENGTH_SHORT).show()
                    name.length > 20 ->
                        Snackbar.make(binding.root, R.string.auth_error_nickname_length, Snackbar.LENGTH_SHORT).show()
                    else -> viewModel.updateChildNickname(child.childId, name)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun confirmRegenerateCode(childId: String) {
        AlertDialog.Builder(requireContext())
            .setMessage(R.string.parent_child_regenerate_code_confirm)
            .setPositiveButton(android.R.string.ok) { _, _ -> viewModel.regenerateChildCode(childId) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun confirmDeleteChild(child: ParentChildItem) {
        AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.parent_child_delete_confirm, child.nickname))
            .setPositiveButton(R.string.parent_child_manage_delete) { _, _ ->
                viewModel.deleteChild(child.childId)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun applyRichSummary(s: ParentChildSummaryResponseData?) {
        val rich = binding.includeDashboardRich
        if (s == null) {
            rich.tvParentDashWeeklySets.text = "—"
            rich.tvParentDashTotalXp.text = "—"
            return
        }
        rich.tvParentDashWeeklySets.text = "${s.weeklyCompletedSetCount}"
        rich.tvParentDashTotalXp.text = "${s.totalXp}"
    }

    private fun applyRichWeekly(w: ParentWeeklyStatsResponseData?) {
        val rich = binding.includeDashboardRich
        val dm = resources.displayMetrics
        val minPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, dm).toInt()
        val maxPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 72f, dm).toInt()
        val bars = listOf(
            rich.barWeek0, rich.barWeek1, rich.barWeek2, rich.barWeek3,
            rich.barWeek4, rich.barWeek5, rich.barWeek6
        )
        if (w == null) {
            bars.forEach { v ->
                v.layoutParams = v.layoutParams.also { lp -> lp.height = minPx }
                v.requestLayout()
            }
            return
        }
        rich.tvParentDashWeeklyXpLine.text = getString(
            R.string.parent_dash_weekly_xp_fmt,
            w.totalWeeklyXp,
            w.totalWeeklyMissions
        )
        val stats = w.dailyStats.take(7)
        val maxCount = stats.maxOfOrNull { it.completedSetCount }?.coerceAtLeast(1) ?: 1
        stats.forEachIndexed { i, d ->
            val bar = bars.getOrNull(i) ?: return@forEachIndexed
            val h = minPx + (maxPx - minPx) * d.completedSetCount / maxCount
            bar.layoutParams = bar.layoutParams.also { lp -> lp.height = h }
            bar.requestLayout()
        }
        for (j in stats.size until bars.size) {
            val bar = bars[j]
            bar.layoutParams = bar.layoutParams.also { lp -> lp.height = minPx }
            bar.requestLayout()
        }
    }

    private fun applyRichWeak(wp: ParentWeakPointsResponseData?) {
        val rich = binding.includeDashboardRich
        val points = wp?.weakPoints.orEmpty().take(3)
        val rows = listOf(
            Triple(rich.rowParentDashWeak1, rich.tvParentDashWeak1Title, rich.tvParentDashWeak1Rate),
            Triple(rich.rowParentDashWeak2, rich.tvParentDashWeak2Title, rich.tvParentDashWeak2Rate),
            Triple(rich.rowParentDashWeak3, rich.tvParentDashWeak3Title, rich.tvParentDashWeak3Rate),
        )
        rows.forEachIndexed { index, (row, titleTv, rateTv) ->
            val top = points.getOrNull(index)
            if (top == null) {
                row.visibility = View.GONE
            } else {
                row.visibility = View.VISIBLE
                titleTv.text = weakPointTitle(top)
                rateTv.text = weakPointRate(top)
            }
        }
        rich.tvParentDashWeakEmpty.visibility =
            when {
                !isSelectedChildLinked -> View.GONE
                points.isEmpty() -> View.VISIBLE
                else -> View.GONE
            }
    }

    private fun weakPointTitle(top: com.kduniv.aimong.feature.parent.data.model.ParentWeakPointDto): String {
        val title = when {
            !top.missionTitle.isNullOrBlank() && !top.setTitle.isNullOrBlank() ->
                "${top.missionTitle} — ${top.setTitle}"
            !top.setTitle.isNullOrBlank() -> top.setTitle!!
            !top.missionTitle.isNullOrBlank() -> top.missionTitle!!
            else -> "—"
        }
        val diffPart = when {
            !top.difficulty.isNullOrBlank() -> top.difficulty!!
            top.starLevel != null -> "★${top.starLevel}"
            top.levelNo != null -> "Lv${top.levelNo}"
            else -> ""
        }
        return if (diffPart.isNotEmpty()) "$title · $diffPart" else title
    }

    private fun weakPointRate(top: com.kduniv.aimong.feature.parent.data.model.ParentWeakPointDto): String {
        val pct = ((top.incorrectRate * 100.0).toInt()).coerceIn(0, 100)
        return "$pct%"
    }

    private fun applyCustomQuests(quests: List<ParentCustomQuestDto>) {
        val rich = binding.includeDashboardRich
        val container = rich.layoutParentCustomQuestList
        container.removeAllViews()
        rich.btnParentTogglePastQuests.visibility =
            if (isSelectedChildLinked) View.VISIBLE else View.GONE
        if (quests.isEmpty()) {
            rich.tvParentCustomQuestEmpty.visibility =
                if (isSelectedChildLinked) View.VISIBLE else View.GONE
            return
        }
        rich.tvParentCustomQuestEmpty.visibility = View.GONE
        val inflater = LayoutInflater.from(requireContext())
        quests.forEach { quest ->
            val row = inflater.inflate(R.layout.item_parent_custom_quest, container, false)
            bindCustomQuestRow(row, quest, showActions = true)
            container.addView(row)
        }
    }

    private fun applyPastCustomQuests(
        quests: List<ParentCustomQuestDto>,
        expanded: Boolean,
        hasNext: Boolean,
        isLoading: Boolean
    ) {
        val rich = binding.includeDashboardRich
        rich.btnParentTogglePastQuests.text = getString(
            if (expanded) R.string.parent_custom_quest_toggle_past_hide
            else R.string.parent_custom_quest_toggle_past_show
        )
        val sectionVisible = expanded && isSelectedChildLinked
        rich.tvParentPastQuestsTitle.visibility = if (sectionVisible) View.VISIBLE else View.GONE
        rich.layoutParentPastCustomQuestList.visibility = if (sectionVisible) View.VISIBLE else View.GONE
        rich.btnParentLoadMorePastQuests.visibility = View.GONE
        if (!sectionVisible) {
            rich.tvParentPastCustomQuestEmpty.visibility = View.GONE
            rich.layoutParentPastCustomQuestList.removeAllViews()
            return
        }
        val container = rich.layoutParentPastCustomQuestList
        container.removeAllViews()
        if (quests.isEmpty() && !isLoading) {
            rich.tvParentPastCustomQuestEmpty.visibility = View.VISIBLE
            return
        }
        rich.tvParentPastCustomQuestEmpty.visibility = View.GONE
        val inflater = LayoutInflater.from(requireContext())
        quests.forEach { quest ->
            val row = inflater.inflate(R.layout.item_parent_custom_quest, container, false)
            bindCustomQuestRow(row, quest, showActions = false)
            container.addView(row)
        }
        if (hasNext || isLoading) {
            rich.btnParentLoadMorePastQuests.visibility = View.VISIBLE
            rich.btnParentLoadMorePastQuests.isEnabled = !isLoading
            rich.btnParentLoadMorePastQuests.text = getString(
                if (isLoading) R.string.parent_custom_quest_past_loading
                else R.string.parent_custom_quest_past_load_more
            )
        }
    }

    private fun bindCustomQuestRow(row: View, quest: ParentCustomQuestDto, showActions: Boolean) {
        row.findViewById<TextView>(R.id.tv_custom_quest_title).text = quest.title
        row.findViewById<TextView>(R.id.tv_custom_quest_reward).text =
            getString(R.string.parent_custom_quest_reward_fmt, quest.rewardText)
        val descTv = row.findViewById<TextView>(R.id.tv_custom_quest_desc)
        val desc = quest.description?.trim().orEmpty()
        if (desc.isNotEmpty()) {
            descTv.text = desc
            descTv.visibility = View.VISIBLE
        } else {
            descTv.visibility = View.GONE
        }
        val expiresTv = row.findViewById<TextView>(R.id.tv_custom_quest_expires)
        formatQuestExpires(quest.expiresAt)?.let { label ->
            expiresTv.text = getString(R.string.parent_custom_quest_expires_fmt, label)
            expiresTv.visibility = View.VISIBLE
        } ?: run { expiresTv.visibility = View.GONE }

        val statusTv = row.findViewById<TextView>(R.id.tv_custom_quest_status)
        when (quest.status.uppercase()) {
            "PENDING_CONFIRM" -> {
                statusTv.text = getString(R.string.parent_custom_quest_status_pending)
                statusTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.parent_mock_indigo))
            }
            "COMPLETED" -> {
                statusTv.text = getString(R.string.parent_custom_quest_status_completed)
                statusTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.parent_mock_green))
            }
            "CANCELLED" -> {
                statusTv.text = getString(R.string.parent_custom_quest_status_cancelled)
                statusTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.parent_mock_text_muted))
            }
            else -> {
                statusTv.text = getString(R.string.parent_custom_quest_status_active)
                statusTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.parent_mock_green))
            }
        }
        val confirmBtn = row.findViewById<TextView>(R.id.btn_custom_quest_confirm)
        val cancelBtn = row.findViewById<TextView>(R.id.btn_custom_quest_cancel)
        if (!showActions) {
            confirmBtn.visibility = View.GONE
            cancelBtn.visibility = View.GONE
            return
        }
        if (quest.status.equals("PENDING_CONFIRM", ignoreCase = true)) {
            confirmBtn.visibility = View.VISIBLE
            confirmBtn.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setMessage(R.string.parent_custom_quest_confirm_dialog)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        viewModel.confirmCustomQuest(quest.questId)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
            cancelBtn.visibility = View.GONE
        } else if (quest.status.equals("ACTIVE", ignoreCase = true)) {
            confirmBtn.visibility = View.GONE
            cancelBtn.visibility = View.VISIBLE
            cancelBtn.setOnClickListener {
                AlertDialog.Builder(requireContext())
                    .setMessage(R.string.parent_custom_quest_cancel_dialog)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        viewModel.cancelCustomQuest(quest.questId)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        } else {
            confirmBtn.visibility = View.GONE
            cancelBtn.visibility = View.GONE
        }
    }

    private fun formatQuestExpires(expiresAt: String?): String? {
        val raw = expiresAt?.trim().orEmpty()
        if (raw.isEmpty()) return null
        return runCatching {
            Instant.parse(raw).atZone(ZoneId.systemDefault()).format(questExpiresDisplayFormatter)
        }.getOrNull()
    }

    private fun applyStageRewards(stages: List<ParentStageRewardDto>) {
        val rich = binding.includeDashboardRich
        val container = rich.layoutParentStageRewards
        container.removeAllViews()
        if (!isSelectedChildLinked) return

        val inflater = LayoutInflater.from(requireContext())
        val displayStages = if (stages.isEmpty()) {
            (1..3).map { stage ->
                ParentStageRewardDto(stageNumber = stage, rewardText = null, isTriggered = false)
            }
        } else {
            stages
        }
        displayStages.forEach { stage ->
            val row = inflater.inflate(R.layout.item_parent_stage_reward, container, false)
            row.findViewById<TextView>(R.id.tv_stage_label).text =
                getString(R.string.parent_stage_reward_stage_fmt, stage.stageNumber)
            val progress = stage.missionProgress
            row.findViewById<TextView>(R.id.tv_stage_progress).text =
                if (progress != null && progress.total > 0) {
                    getString(
                        R.string.parent_stage_reward_progress_fmt,
                        progress.completed,
                        progress.total
                    )
                } else {
                    "—"
                }
            val progressBar = row.findViewById<android.widget.ProgressBar>(R.id.progress_stage_mission)
            if (progress != null && progress.total > 0) {
                progressBar.max = progress.total
                progressBar.progress = progress.completed.coerceIn(0, progress.total)
                progressBar.visibility = View.VISIBLE
            } else {
                progressBar.visibility = View.GONE
            }
            val rewardTv = row.findViewById<TextView>(R.id.tv_stage_reward_text)
            val rewardText = stage.rewardText?.trim().orEmpty()
            rewardTv.text = if (rewardText.isNotEmpty()) {
                getString(R.string.parent_stage_reward_promise_fmt, rewardText)
            } else {
                getString(R.string.parent_stage_reward_unset)
            }
            row.findViewById<TextView>(R.id.tv_stage_default_rewards).text = getString(
                R.string.parent_stage_reward_default_fmt,
                stage.defaultGearReward,
                stage.normalTicketReward
            )
            val triggeredTv = row.findViewById<TextView>(R.id.tv_stage_triggered)
            val actionBtn = row.findViewById<TextView>(R.id.btn_stage_reward_edit)
            val hasReward = !stage.rewardText.isNullOrBlank()
            if (stage.isTriggered) {
                triggeredTv.visibility = View.VISIBLE
                actionBtn.visibility = View.GONE
            } else {
                triggeredTv.visibility = View.GONE
                actionBtn.visibility = View.VISIBLE
                actionBtn.text = getString(
                    if (hasReward) R.string.parent_stage_reward_edit_row
                    else R.string.parent_stage_reward_add_row
                )
                actionBtn.setOnClickListener { showStageRewardDialog(stage) }
            }
            container.addView(row)
        }
    }

    private fun showParentFormCardDialog(
        contentView: View,
        onDismiss: () -> Unit = {},
    ): Pair<Dialog, View> {
        val dialog = Dialog(requireContext()).apply {
            setContentView(contentView)
            setCancelable(true)
            setOnDismissListener { onDismiss() }
        }
        ParentFormDialogWindow.apply(dialog, requireContext())
        return dialog to (dialog.window?.decorView ?: binding.root)
    }

    private fun updateQuestExpiresPresetChips(
        todayBtn: MaterialButton,
        weekBtn: MaterialButton,
        preset: QuestExpiresPreset,
    ) {
        todayBtn.isSelected = preset == QuestExpiresPreset.TODAY
        weekBtn.isSelected = preset == QuestExpiresPreset.WEEK
    }

    private fun showCreateCustomQuestDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_parent_custom_quest, null, false)
        val formRoot = dialogView.findViewById<View>(R.id.include_quest_form)
        val titleInput = formRoot.findViewById<TextInputEditText>(R.id.input_quest_title)
        val descInput = formRoot.findViewById<TextInputEditText>(R.id.input_quest_desc)
        val rewardInput = formRoot.findViewById<TextInputEditText>(R.id.input_quest_reward)
        val expiresValueTv = formRoot.findViewById<TextView>(R.id.tv_quest_expires_value)
        val expiresRow = formRoot.findViewById<View>(R.id.btn_quest_expires_at)
        val presetTodayBtn = formRoot.findViewById<MaterialButton>(R.id.btn_expires_preset_today)
        val presetWeekBtn = formRoot.findViewById<MaterialButton>(R.id.btn_expires_preset_week)
        val zone = ZoneId.systemDefault()
        var selectedExpires = questExpiresPresetOneWeekLater(zone)
        var activePreset = QuestExpiresPreset.WEEK
        fun updateExpiresLabel() {
            expiresValueTv.text = selectedExpires.format(questExpiresDisplayFormatter)
        }
        fun refreshPresetChips() {
            updateQuestExpiresPresetChips(presetTodayBtn, presetWeekBtn, activePreset)
        }
        updateExpiresLabel()
        refreshPresetChips()
        expiresRow.setOnClickListener {
            showQuestExpiresPicker(selectedExpires) { picked ->
                selectedExpires = picked
                activePreset = QuestExpiresPreset.CUSTOM
                refreshPresetChips()
                updateExpiresLabel()
            }
        }
        presetTodayBtn.setOnClickListener {
            dismissQuestExpiresPickers()
            selectedExpires = questExpiresPresetEndOfToday(zone)
            activePreset = QuestExpiresPreset.TODAY
            refreshPresetChips()
            updateExpiresLabel()
        }
        presetWeekBtn.setOnClickListener {
            dismissQuestExpiresPickers()
            selectedExpires = questExpiresPresetOneWeekLater(zone)
            activePreset = QuestExpiresPreset.WEEK
            refreshPresetChips()
            updateExpiresLabel()
        }
        val (dialog, snackbarAnchor) = showParentFormCardDialog(dialogView) { dismissQuestExpiresPickers() }
        dialogView.findViewById<View>(R.id.btn_quest_dialog_cancel).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<View>(R.id.btn_quest_dialog_confirm).setOnClickListener {
            val title = titleInput.text?.toString()?.trim().orEmpty()
            val reward = rewardInput.text?.toString()?.trim().orEmpty()
            val desc = descInput.text?.toString()?.trim().orEmpty()
            when {
                title.isBlank() || reward.isBlank() ->
                    Snackbar.make(snackbarAnchor, R.string.parent_custom_quest_validation_required, Snackbar.LENGTH_SHORT).show()
                title.length > 80 || reward.length > 100 ->
                    Snackbar.make(snackbarAnchor, R.string.parent_custom_quest_validation_length, Snackbar.LENGTH_SHORT).show()
                selectedExpires.isBefore(ZonedDateTime.now(zone)) ->
                    Snackbar.make(snackbarAnchor, R.string.parent_custom_quest_expires_invalid, Snackbar.LENGTH_SHORT).show()
                else -> {
                    dialog.dismiss()
                    viewModel.createCustomQuest(
                        title,
                        desc,
                        reward,
                        selectedExpires.toInstant().toString()
                    )
                }
            }
        }
        dialog.show()
    }

    private fun questExpiresPresetEndOfToday(zone: ZoneId): ZonedDateTime {
        val now = ZonedDateTime.now(zone)
        return try {
            now.withHour(23).withMinute(59).withSecond(0).withNano(0)
        } catch (_: DateTimeException) {
            now.plusHours(1)
        }
    }

    private fun questExpiresPresetOneWeekLater(zone: ZoneId): ZonedDateTime {
        val base = ZonedDateTime.now(zone).plusDays(7)
        return try {
            base.withHour(18).withMinute(0).withSecond(0).withNano(0)
        } catch (_: DateTimeException) {
            base.plusHours(1)
        }
    }

    private fun dismissQuestExpiresPickers() {
        questExpiresDatePicker?.dismiss()
        questExpiresTimePicker?.dismiss()
        questExpiresDatePicker = null
        questExpiresTimePicker = null
        listOf("quest_expires_date", "quest_expires_time").forEach { tag ->
            childFragmentManager.findFragmentByTag(tag)?.let { fragment ->
                if (fragment.isAdded) {
                    childFragmentManager.beginTransaction().remove(fragment).commitAllowingStateLoss()
                }
            }
        }
    }

    private fun showQuestExpiresPicker(
        initial: ZonedDateTime,
        onSelected: (ZonedDateTime) -> Unit
    ) {
        if (!isAdded) return
        dismissQuestExpiresPickers()
        val zone = ZoneId.systemDefault()
        val localDate = initial.toLocalDate()
        val activity = requireActivity()
        questExpiresDatePicker = DatePickerDialog(
            activity,
            { _, year, month, dayOfMonth ->
                val pickedDate = LocalDate.of(year, month + 1, dayOfMonth)
                questExpiresTimePicker = TimePickerDialog(
                    activity,
                    { _, hour, minute ->
                        onSelected(ZonedDateTime.of(pickedDate, LocalTime.of(hour, minute), zone))
                        questExpiresTimePicker = null
                    },
                    initial.hour,
                    initial.minute,
                    true
                ).also { picker ->
                    picker.setOnDismissListener { questExpiresTimePicker = null }
                    picker.show()
                }
                questExpiresDatePicker = null
            },
            localDate.year,
            localDate.monthValue - 1,
            localDate.dayOfMonth
        ).also { picker ->
            picker.setOnDismissListener { questExpiresDatePicker = null }
            picker.show()
        }
    }

    private fun showStageRewardDialog(stage: ParentStageRewardDto) {
        if (stage.isTriggered) return
        val hasReward = !stage.rewardText.isNullOrBlank()
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_parent_stage_reward, null, false)
        val titleRes = if (hasReward) {
            R.string.parent_stage_reward_edit_title
        } else {
            R.string.parent_stage_reward_add_title
        }
        dialogView.findViewById<TextView>(R.id.tv_stage_reward_dialog_title).text =
            getString(titleRes, stage.stageNumber)
        val input = dialogView.findViewById<TextInputEditText>(R.id.input_stage_reward)
        input.setText(stage.rewardText.orEmpty())
        val (dialog, snackbarAnchor) = showParentFormCardDialog(dialogView)
        dialogView.findViewById<View>(R.id.btn_stage_reward_dialog_cancel).setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<View>(R.id.btn_stage_reward_dialog_confirm).setOnClickListener {
            val text = input.text?.toString()?.trim().orEmpty()
            if (text.isBlank()) {
                Snackbar.make(snackbarAnchor, R.string.parent_custom_quest_validation_required, Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (text.length > 100) {
                Snackbar.make(snackbarAnchor, R.string.parent_custom_quest_validation_length, Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            dialog.dismiss()
            viewModel.saveStageReward(stage.stageNumber, text, hasExistingReward = hasReward)
        }
        dialog.show()
    }

    private fun applyRichRecent(summary: ParentChildSummaryResponseData?) {
        val rich = binding.includeDashboardRich
        val container = rich.layoutDashboardRecentRows
        container.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        var rowCount = 0

        if (summary != null && summary.lastActiveAt != null) {
            val row = inflater.inflate(R.layout.include_parent_recent_row_mission, container, false)
            row.findViewById<TextView>(R.id.tv_recent_mission_title)?.text =
                getString(
                    R.string.parent_dashboard_recent_summary_fmt,
                    summary.totalXp,
                    summary.continuousDays,
                )
            row.findViewById<TextView>(R.id.tv_recent_mission_time)?.text =
                summary.lastActiveAt ?: "—"
            row.findViewById<TextView>(R.id.tv_recent_mission_score)?.text =
                getString(R.string.parent_dashboard_recent_weekly_sets_fmt, summary.weeklyCompletedSetCount)
            container.addView(row)
            rowCount++
        }

        rich.tvParentDashRecentEmpty.visibility =
            if (rowCount == 0) View.VISIBLE else View.GONE
    }

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.syncChildren()
                launch {
                    viewModel.children.collect { children ->
                        latestChildren = children
                        val empty = children.isEmpty()
                        binding.cardEmptyChildren.visibility = if (empty) View.VISIBLE else View.GONE
                        binding.includeDashboardRich.root.visibility = if (empty) View.GONE else View.VISIBLE
                        if (!empty && viewModel.selectedChildId.value.isNullOrBlank()) {
                            viewModel.selectChild(children.first().childId)
                        }
                        updateDashboardTitle()
                        updateRichChildLabel()
                    }
                }
                launch {
                    viewModel.selectedChildId.collect { id ->
                        latestSelectedChildId = id
                        latestChildDetail = null
                        applyChildLinkedState(linked = false)
                        updateDashboardTitle()
                        updateRichChildLabel()
                    }
                }
                launch {
                    viewModel.childDetail.collect { d ->
                        latestChildDetail = d
                        updateRichChildLabel()
                        val linked = isChildLinked(d)
                        applyChildLinkedState(linked)
                        if (d == null) return@collect
                        binding.tvParentSummary.text =
                            if (linked) {
                                "자녀 상태\n- 닉네임: ${d.nickname}\n- XP: ${d.totalXp}\n- 연동: 완료\n- 마지막 활동: ${d.lastActiveAt}"
                            } else {
                                "자녀 상태\n아직 자녀가 코드를 입력하지 않았어요!\n- 닉네임: ${d.nickname}\n- 코드: ${d.code}\n- 연동: 대기"
                            }
                    }
                }
                launch {
                    viewModel.childSummary.collect { s ->
                        if (!isSelectedChildLinked) return@collect
                        applyRichSummary(s)
                        applyRichRecent(s)
                        if (s == null) return@collect
                        parentDashboardPerfMark?.let { started ->
                            parentDashboardPerfMark = null
                            UiPerfLog.measureFrom(
                                "parent_dashboard_first_paint",
                                started,
                                "parent_dashboard_entry_to_summary_ms",
                            )
                        }
                        binding.tvParentSummary.text =
                            "요약\n- 닉네임: ${s.nickname}\n- XP: ${s.totalXp}\n- 스트릭: ${s.continuousDays}일\n- 실드: ${s.shieldCount}\n- 주간 완료 세트: ${s.weeklyCompletedSetCount}\n- 총 완료 세트: ${s.totalCompletedSetCount}\n- 현재 레벨: ${s.currentLevelNo}\n- 마지막 활동: ${s.lastActiveAt ?: "-"}"
                    }
                }
                launch {
                    viewModel.weeklyStats.collect { w ->
                        if (!isSelectedChildLinked) return@collect
                        applyRichWeekly(w)
                        if (w == null) return@collect
                        val lines = w.dailyStats.joinToString(separator = "\n") { d ->
                            "- ${d.dayOfWeek}(${d.date}): 완료 ${d.completedSetCount}, XP ${d.xpEarned}"
                        }
                        binding.tvParentWeeklyStats.text =
                            "주간 통계 (${w.weekStart ?: "-"} ~ ${w.weekEnd ?: "-"})\n- 주간 XP: ${w.totalWeeklyXp}\n- 주간 완료 세트: ${w.totalWeeklyMissions}\n$lines"
                    }
                }
                launch {
                    viewModel.customQuests.collect { quests ->
                        if (!isSelectedChildLinked) {
                            applyCustomQuests(emptyList())
                            return@collect
                        }
                        applyCustomQuests(quests)
                    }
                }
                launch {
                    combine(
                        viewModel.pastCustomQuests,
                        viewModel.pastQuestsExpanded,
                        viewModel.pastQuestsHasNext,
                        viewModel.pastQuestsLoading,
                    ) { past, expanded, hasNext, loading ->
                        PastQuestUiState(past, expanded, hasNext, loading)
                    }.collect { state ->
                        if (!isSelectedChildLinked) {
                            applyPastCustomQuests(emptyList(), expanded = false, hasNext = false, isLoading = false)
                            return@collect
                        }
                        applyPastCustomQuests(
                            state.quests,
                            state.expanded,
                            state.hasNext,
                            state.isLoading
                        )
                    }
                }
                launch {
                    viewModel.stageRewards.collect { stages ->
                        if (!isSelectedChildLinked) {
                            applyStageRewards(emptyList())
                            return@collect
                        }
                        applyStageRewards(stages)
                    }
                }
                launch {
                    viewModel.weakPoints.collect { wp ->
                        if (!isSelectedChildLinked) return@collect
                        applyRichWeak(wp)
                        if (wp == null) return@collect
                        val lines = wp.weakPoints.joinToString(separator = "\n") {
                            val title = it.setTitle ?: it.missionTitle ?: "-"
                            val stage = it.stage?.let { st -> "S$st" } ?: "-"
                            val diff = it.difficulty?.takeIf { d -> d.isNotBlank() }
                                ?: it.starLevel?.let { sl -> "★$sl" }
                                ?: it.levelNo?.let { l -> "L$l" }
                                ?: "-"
                            "- $title ($diff/$stage) 오답률 ${it.incorrectRate}, 시도 ${it.attemptCount}"
                        }
                        val wpPages =
                            if (wp.totalPages > 0) "\n- totalPages: ${wp.totalPages}" else ""
                        binding.tvParentWeakPoints.text =
                            "약점 분석 (${wp.analyzedPeriod ?: "최근"})\n- total: ${wp.totalCount}$wpPages\n$lines"
                    }
                }
                launch {
                    viewModel.dashboardRefreshing.collect { refreshing ->
                        binding.swipeParentDashboardRefresh.isRefreshing = refreshing
                    }
                }
                launch {
                    viewModel.messageEvent.collect { message ->
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                    }
                }
                launch {
                    viewModel.childRegisteredEvent.collect { data ->
                        showChildRegisterSuccessSheet(data)
                    }
                }
            }
        }
    }

    private fun showChildRegisterSuccessSheet(data: ParentRegisterResponse) {
        val bottomSheet = ChildRegisterSuccessBottomSheet.newInstance(data)
        bottomSheet.onConfirmClick = {
            updateDashboardTitle()
            updateRichChildLabel()
        }
        bottomSheet.show(childFragmentManager, "child_register_success")
    }
}
