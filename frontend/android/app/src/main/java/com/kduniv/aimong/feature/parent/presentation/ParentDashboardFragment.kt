package com.kduniv.aimong.feature.parent.presentation

import android.content.Intent
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.kduniv.aimong.MainActivity
import com.kduniv.aimong.R
import com.kduniv.aimong.core.local.SessionManager
import com.kduniv.aimong.core.network.model.ParentChildItem
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.databinding.FragmentParentDashboardBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ParentDashboardFragment : BaseFragment<FragmentParentDashboardBinding>(FragmentParentDashboardBinding::inflate) {

    private val viewModel: ParentDashboardViewModel by viewModels()
    private lateinit var adapter: ParentChildAdapter

    @Inject
    lateinit var sessionManager: SessionManager

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
        adapter = ParentChildAdapter(
            onSelectChild = { childId -> viewModel.selectChild(childId) },
            onRegenerateCode = { childId -> viewModel.regenerateChildCode(childId) }
        )

        binding.rvChildren.layoutManager = LinearLayoutManager(requireContext())
        binding.rvChildren.adapter = adapter

        binding.btnSyncChildren.setOnClickListener { viewModel.syncChildren() }
        binding.btnFetchSummary.setOnClickListener { viewModel.fetchChildDetail() }
        binding.btnFetchWeeklyStats.setOnClickListener { viewModel.fetchChildDetail() }
        binding.btnFetchPrivacyLog.setOnClickListener { viewModel.fetchChildDetail() }
        binding.btnFetchWeakPoints.setOnClickListener { viewModel.fetchChildDetail() }

        binding.includeDashboardRich.btnDashboardPrivacyMore.setOnClickListener {
            findNavController().navigate(R.id.action_parentDashboardFragment_to_privacyLogFragment)
        }

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            viewLifecycleOwner.lifecycleScope.launch {
                sessionManager.clearSession()
                val intent = Intent(requireContext(), MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra(MainActivity.EXTRA_IS_RESTART, true)
                }
                startActivity(intent)
            }
        }
    }

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.children.collect { children ->
                        latestChildren = children
                        adapter.submitList(children)
                        val empty = children.isEmpty()
                        binding.cardEmptyChildren.visibility = if (empty) View.VISIBLE else View.GONE
                        binding.includeDashboardRich.root.visibility = if (empty) View.GONE else View.VISIBLE
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

                        binding.tvParentWeeklyStats.text = "주간 통계: (v2 전환 중)"
                        binding.tvParentPrivacyLog.text = "개인정보 감지: (v2 전환 중)"
                        binding.tvParentWeakPoints.text = "약점 분석: (v2 전환 중)"
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
