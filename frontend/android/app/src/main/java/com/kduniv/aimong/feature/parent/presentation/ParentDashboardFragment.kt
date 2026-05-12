package com.kduniv.aimong.feature.parent.presentation

import android.content.Intent
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.kduniv.aimong.MainActivity
import com.kduniv.aimong.core.local.SessionManager
import com.kduniv.aimong.R
import com.kduniv.aimong.feature.parent.data.ParentRepository
import com.kduniv.aimong.core.network.model.ParentChildItem
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.databinding.FragmentParentDashboardBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@AndroidEntryPoint
class ParentDashboardFragment : BaseFragment<FragmentParentDashboardBinding>(FragmentParentDashboardBinding::inflate) {

    private val viewModel: ParentDashboardViewModel by viewModels()
    private lateinit var adapter: ParentChildAdapter

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
        binding.tvTitle.text = titleForParentDashboard(childNickname)
    }

    override fun initView() {
        adapter = ParentChildAdapter(
            onSelectChild = { childId -> viewModel.selectChild(childId) },
            onRegenerateCode = { childId -> viewModel.regenerateChildCode(childId) }
        )

        binding.rvChildren.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChildren.adapter = adapter

        binding.btnSyncChildren.setOnClickListener { viewModel.syncChildren() }
        binding.btnFetchSummary.setOnClickListener { viewModel.fetchSummary() }
        binding.btnFetchWeeklyStats.setOnClickListener { viewModel.fetchWeeklyStats() }
        binding.btnFetchPrivacyLog.setOnClickListener { viewModel.fetchPrivacyLog() }
        binding.btnFetchWeakPoints.setOnClickListener { viewModel.fetchWeakPoints() }

        binding.btnParentMe.setOnClickListener { viewModel.fetchParentMe() }
        binding.btnAddChild.setOnClickListener { showAddChildDialog() }

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

    private fun showAddChildDialog() {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.parent_add_child_hint)
        }
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.parent_add_child)
            .setView(input)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = input.text?.toString()?.trim().orEmpty()
                if (name.isNotEmpty()) viewModel.addChild(name)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.children.collect { children ->
                        latestChildren = children
                        adapter.submitList(children)
                        updateDashboardTitle()
                    }
                }
                launch {
                    viewModel.selectedChildId.collect { id ->
                        latestSelectedChildId = id
                        updateDashboardTitle()
                    }
                }
                launch {
                    viewModel.summary.collect { s ->
                        if (s == null) return@collect
                        binding.tvParentSummary.text =
                            "요약\n- 닉네임: ${s.nickname}\n- XP: ${s.totalXp}\n- 스트릭: ${s.continuousDays}일\n- 실드: ${s.shieldCount}\n- 주간 완료 세트: ${s.weeklyCompletedSetCount}\n- 총 완료 세트: ${s.totalCompletedSetCount}\n- 현재 레벨: ${s.currentLevelNo}\n- 마지막 활동: ${s.lastActiveAt ?: "-"}"
                    }
                }
                launch {
                    viewModel.weeklyStats.collect { w ->
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
                        if (wp == null) return@collect
                        val lines = wp.weakPoints.joinToString(separator = "\n") { it ->
                            val title = it.setTitle ?: it.missionTitle ?: "-"
                            val stage = it.stage?.let { s -> "S$s" } ?: "-"
                            val diff = it.starLevel?.let { sl -> "★$sl" }
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

