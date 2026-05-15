package com.kduniv.aimong.feature.parent.presentation

import android.content.Intent
import android.util.TypedValue
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.kduniv.aimong.MainActivity
import com.kduniv.aimong.R
import com.kduniv.aimong.core.local.SessionManager
import com.kduniv.aimong.core.network.model.ParentChildItem
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.databinding.FragmentParentDashboardBinding
import com.kduniv.aimong.feature.parent.data.ParentRepository
import com.kduniv.aimong.feature.parent.data.model.ParentChildSummaryResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentPrivacyLogResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentWeakPointsResponseData
import com.kduniv.aimong.feature.parent.data.model.ParentWeeklyStatsResponseData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@AndroidEntryPoint
class ParentDashboardFragment :
    BaseFragment<FragmentParentDashboardBinding>(FragmentParentDashboardBinding::inflate),
    ParentChildSelectBottomSheet.Listener {

    private val viewModel: ParentDashboardViewModel by viewModels()

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var parentRepository: ParentRepository

    private fun titleForParentDashboard(nickname: String?): String {
        val n = nickname?.trim().orEmpty()
        return if (n.isNotEmpty()) getString(R.string.parent_dashboard_title_with_nickname, n)
        else getString(R.string.parent_dashboard_title_default)
    }

    private var latestChildren: List<ParentChildItem> = emptyList()
    private var latestSelectedChildId: String? = null

    private fun updateDashboardTitle() {
        val childNickname = latestSelectedChildId
            ?.let { id -> latestChildren.firstOrNull { it.childId == id }?.nickname }
        binding.tvBrandTitle.text = titleForParentDashboard(childNickname)
    }

    private fun updateRichChildLabel() {
        val nick = latestSelectedChildId
            ?.let { id -> latestChildren.firstOrNull { it.childId == id }?.nickname?.trim() }
            ?.takeIf { it.isNotEmpty() }
            ?: latestChildren.firstOrNull()?.nickname?.trim()?.takeIf { it.isNotEmpty() }
        binding.includeDashboardRich.tvDashboardSelectedChild.text =
            nick ?: getString(R.string.parent_dashboard_child_select_placeholder)
    }

    override fun initView() {
        val rich = binding.includeDashboardRich
        rich.rowDashboardChildSelector.setOnClickListener { showChildSelectSheet() }
        binding.cardEmptyChildren.setOnClickListener { showChildSelectSheet() }

        binding.includeDashboardRich.btnDashboardPrivacyMore.setOnClickListener {
            findNavController().navigate(R.id.action_parentDashboardFragment_to_privacyLogFragment)
        }

        binding.btnLogout.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                val user = FirebaseAuth.getInstance().currentUser
                runCatching {
                    val token = user?.getIdToken(false)?.await()?.token
                    if (!token.isNullOrBlank()) {
                        parentRepository.deleteParentFcmToken(token)
                    }
                }
                FirebaseAuth.getInstance().signOut()
                sessionManager.clearSession()
                val intent = Intent(requireContext(), MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra(MainActivity.EXTRA_IS_RESTART, true)
                }
                startActivity(intent)
            }
        }
    }

    private fun showChildSelectSheet() {
        ParentChildSelectBottomSheet.newInstance()
            .show(childFragmentManager, ParentChildSelectBottomSheet.TAG)
    }

    override fun onAddChildRequested() {
        showAddChildDialog()
    }

    private fun showAddChildDialog() {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.parent_add_child_hint)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.parent_add_child_dialog_title)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = input.text?.toString()?.trim().orEmpty()
                if (name.isNotEmpty()) viewModel.addChild(name)
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
        val top = wp?.weakPoints?.firstOrNull()
        if (top == null) {
            rich.tvParentDashWeak1Title.setText(R.string.parent_dashboard_weak_ai_ethics)
            rich.tvParentDashWeak1Rate.text = "—"
            return
        }
        val title = when {
            !top.missionTitle.isNullOrBlank() && !top.setTitle.isNullOrBlank() ->
                "${top.missionTitle} — ${top.setTitle}"
            !top.setTitle.isNullOrBlank() -> top.setTitle!!
            !top.missionTitle.isNullOrBlank() -> top.missionTitle!!
            else -> "—"
        }
        val diffPart = when {
            !top.difficulty.isNullOrBlank() -> top.difficulty!!
            top.levelNo != null -> "Lv${top.levelNo}"
            else -> ""
        }
        rich.tvParentDashWeak1Title.text = if (diffPart.isNotEmpty()) "$title · $diffPart" else title
        val pct = ((top.incorrectRate * 100.0).toInt()).coerceIn(0, 100)
        rich.tvParentDashWeak1Rate.text = "$pct%"
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
