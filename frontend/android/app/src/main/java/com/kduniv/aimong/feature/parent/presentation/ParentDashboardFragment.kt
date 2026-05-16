package com.kduniv.aimong.feature.parent.presentation

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.kduniv.aimong.R
import com.kduniv.aimong.core.network.model.ParentChildDetailData
import com.kduniv.aimong.core.network.model.ParentChildItem
import com.kduniv.aimong.core.network.model.PatchParentChildRequest
import com.kduniv.aimong.feature.parent.domain.ParentAuthPolicy
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.databinding.FragmentParentDashboardBinding
import com.kduniv.aimong.feature.parent.data.ParentRepository
import com.kduniv.aimong.feature.parent.data.model.ParentChildSummaryResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentPrivacyLogResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentWeakPointsResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentWeeklyStatsResponseData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

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
        if (code.isNotBlank()) {
            rich.tvDashboardChildCode.text = code
            rich.layoutDashboardChildCode.visibility = View.VISIBLE
        } else {
            rich.layoutDashboardChildCode.visibility = View.GONE
        }
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
        rich.rowDashboardChildSelector.setOnClickListener { showChildSelectSheet() }
        binding.cardEmptyChildren.setOnClickListener { showChildSelectSheet() }

        binding.includeDashboardRich.btnDashboardPrivacyMore.setOnClickListener {
            findNavController().navigate(R.id.action_parentDashboardFragment_to_privacyLogFragment)
        }

        binding.btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_parentDashboardFragment_to_parentSettingsFragment)
        }
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

    private fun applyRichPrivacy(p: ParentPrivacyLogResponseData?) {
        val rich = binding.includeDashboardRich
        if (p == null) return
        val first = p.events.firstOrNull()
        val base = getString(R.string.parent_dash_privacy_fmt, p.weeklyCount, p.totalCount)
        rich.tvParentDashPrivacySummary.text =
            if (first != null) "$base\n${getString(R.string.parent_dash_privacy_recent, first.detectedType)}"
            else base
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
            if (points.isEmpty()) View.VISIBLE else View.GONE
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

    private fun applyRichRecent(
        summary: ParentChildSummaryResponseData?,
        privacy: ParentPrivacyLogResponseData?,
    ) {
        val rich = binding.includeDashboardRich
        val container = rich.layoutDashboardRecentRows
        container.removeAllViews()
        val inflater = LayoutInflater.from(requireContext())
        var rowCount = 0

        privacy?.events?.take(2)?.forEach { event ->
            val row = inflater.inflate(R.layout.include_parent_recent_row_privacy, container, false)
            row.findViewById<TextView>(R.id.tv_recent_privacy_title)?.text =
                getString(R.string.parent_dashboard_recent_privacy_fmt, event.detectedType)
            row.findViewById<TextView>(R.id.tv_recent_privacy_time)?.text = event.detectedAt
            container.addView(row)
            rowCount++
        }

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
                        updateDashboardTitle()
                        updateRichChildLabel()
                    }
                }
                launch {
                    viewModel.childDetail.collect { d ->
                        latestChildDetail = d
                        updateRichChildLabel()
                        if (d == null) return@collect
                        val linked = d.lastActiveAt != null
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
                        applyRichSummary(s)
                        applyRichRecent(s, viewModel.privacyLog.value)
                        if (s == null) return@collect
                        binding.tvParentSummary.text =
                            "요약\n- 닉네임: ${s.nickname}\n- XP: ${s.totalXp}\n- 스트릭: ${s.continuousDays}일\n- 실드: ${s.shieldCount}\n- 주간 완료 세트: ${s.weeklyCompletedSetCount}\n- 총 완료 세트: ${s.totalCompletedSetCount}\n- 현재 레벨: ${s.currentLevelNo}\n- 마지막 활동: ${s.lastActiveAt ?: "-"}"
                    }
                }
                launch {
                    viewModel.weeklyStats.collect { w ->
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
                    viewModel.privacyLog.collect { p ->
                        applyRichPrivacy(p)
                        applyRichRecent(viewModel.childSummary.value, p)
                        if (p == null) return@collect
                        val lines = p.events.joinToString(separator = "\n") { e ->
                            "- ${e.detectedType} (masked=${e.masked}) @ ${e.detectedAt}"
                        }
                        val pagesNote =
                            if (p.totalPages > 0) "\n- totalPages: ${p.totalPages}" else ""
                        binding.tvParentPrivacyLog.text =
                            "개인정보 감지\n- weekly: ${p.weeklyCount}\n- total: ${p.totalCount}$pagesNote\n$lines"
                    }
                }
                launch {
                    viewModel.weakPoints.collect { wp ->
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
                    viewModel.messageEvent.collect { message ->
                        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
