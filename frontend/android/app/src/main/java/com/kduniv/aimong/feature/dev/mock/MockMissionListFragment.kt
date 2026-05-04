package com.kduniv.aimong.feature.dev.mock

import android.view.View
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.kduniv.aimong.R
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.databinding.FragmentMissionListBinding
import com.kduniv.aimong.feature.mission.domain.model.Mission
import com.kduniv.aimong.feature.mission.domain.model.MissionProgress
import com.kduniv.aimong.feature.mission.presentation.MissionListAdapter

/** [MissionListFragment]와 동일 레이아웃 — 고정 미션 목록(목업). */
class MockMissionListFragment : BaseFragment<FragmentMissionListBinding>(FragmentMissionListBinding::inflate) {

    private lateinit var missionAdapter: MissionListAdapter

    override fun initView() {
        binding.pbLoading.visibility = View.GONE
        binding.layoutErrorState.visibility = View.GONE
        binding.layoutEmptyState.visibility = View.GONE
        binding.rvMissions.visibility = View.VISIBLE

        missionAdapter = MissionListAdapter { mission ->
            findNavController().navigate(
                MockMissionListFragmentDirections.actionLearningFragmentToQuizFragment(mission.id)
            )
        }
        binding.rvMissions.apply {
            adapter = missionAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
        missionAdapter.submitList(mockMissions())

        binding.btnRetryMissions.setOnClickListener {
            Toast.makeText(requireContext(), R.string.stub_mock_retry_toast, Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindStageProgress(progress: MissionProgress) {
        // Mock stage progress functionality removed based on UI update
    }

    private fun mockMissions(): List<Mission> = listOf(
        Mission(
            id = "mock-mission-1",
            stage = 1,
            title = "입문: AI 친구 만나기",
            description = "목업 미션입니다. 실제 연동 시 서버 문구가 표시됩니다.",
            isUnlocked = true,
            isCompleted = false,
            completedAt = null,
            isReviewable = false
        ),
        Mission(
            id = "mock-mission-2",
            stage = 1,
            title = "탐험: 단어 맞추기",
            description = "복습 가능한 목업 카드 예시입니다.",
            isUnlocked = true,
            isCompleted = true,
            completedAt = "2026-05-01",
            isReviewable = true
        ),
        Mission(
            id = "mock-mission-3",
            stage = 2,
            title = "도전: 문장 완성",
            description = "잠금 해제된 2단계 목업입니다.",
            isUnlocked = true,
            isCompleted = false,
            completedAt = null,
            isReviewable = false
        ),
        Mission(
            id = "mock-mission-locked",
            stage = 3,
            title = "잠금: 고급 미션",
            description = "아직 열리지 않은 목업입니다.",
            isUnlocked = false,
            isCompleted = false,
            completedAt = null,
            isReviewable = false
        )
    )

    override fun initObserver() {}
}
