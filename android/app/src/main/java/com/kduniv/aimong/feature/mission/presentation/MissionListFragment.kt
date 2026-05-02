package com.kduniv.aimong.feature.mission.presentation

import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.databinding.FragmentMissionListBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MissionListFragment : BaseFragment<FragmentMissionListBinding>(FragmentMissionListBinding::inflate) {

    private val viewModel: MissionListViewModel by viewModels()
    private lateinit var missionAdapter: MissionListAdapter

    override fun initView() {
        initRecyclerView()
        
        binding.cardAiChat.setOnClickListener {
            // AI 챗봇 화면으로 이동
            findNavController().navigate(MissionListFragmentDirections.actionLearningFragmentToChatFragment())
        }

        binding.btnDummyPreview.setOnClickListener {
            findNavController().navigate(MissionListFragmentDirections.actionLearningFragmentToDummyQuizFragment())
        }

        binding.btnDummyQuiz.setOnClickListener {
            findNavController().navigate(MissionListFragmentDirections.actionLearningFragmentToDummyQuizFragment())
        }
    }

    private fun initRecyclerView() {
        missionAdapter = MissionListAdapter { mission ->
            // 미션 클릭 시 퀴즈 화면으로 이동
            val action = MissionListFragmentDirections.actionLearningFragmentToQuizFragment(mission.id)
            findNavController().navigate(action)
        }
        binding.rvMissions.apply {
            adapter = missionAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is MissionListUiState.Loading -> {
                            binding.pbLoading.visibility = View.VISIBLE
                            binding.rvMissions.visibility = View.GONE
                            binding.tvEmptyMessage.visibility = View.GONE
                        }
                        is MissionListUiState.Success -> {
                            binding.pbLoading.visibility = View.GONE
                            val missions = if (state.missions.isEmpty()) {
                                createDummyMissions()
                            } else {
                                state.missions
                            }
                            
                            binding.rvMissions.visibility = View.VISIBLE
                            binding.layoutEmptyState.visibility = View.GONE
                            missionAdapter.submitList(missions)
                        }
                        is MissionListUiState.Error -> {
                            binding.pbLoading.visibility = View.GONE
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun createDummyMissions(): List<com.kduniv.aimong.feature.mission.domain.model.Mission> {
        return listOf(
            com.kduniv.aimong.feature.mission.domain.model.Mission(
                "1", 1, "AI가 뭐예요?", "AI의 개념과 환각 현상을 배워요",
                isUnlocked = true, isCompleted = true, completedAt = "2024-03-20", isReviewable = true
            ),
            com.kduniv.aimong.feature.mission.domain.model.Mission(
                "2", 2, "개인정보 보호", "소중한 내 정보를 지키는 방법",
                isUnlocked = true, isCompleted = false, completedAt = null, isReviewable = false
            ),
            com.kduniv.aimong.feature.mission.domain.model.Mission(
                "3", 3, "프롬프트 마스터", "AI와 대화하는 멋진 방법",
                isUnlocked = false, isCompleted = false, completedAt = null, isReviewable = false
            ),
            com.kduniv.aimong.feature.mission.domain.model.Mission(
                "4", 4, "디지털 예절", "온라인에서 지켜야 할 약속",
                isUnlocked = false, isCompleted = false, completedAt = null, isReviewable = false
            ),
            com.kduniv.aimong.feature.mission.domain.model.Mission(
                "5", 5, "팩트체크의 중요성", "진짜와 가짜를 구별해봐요",
                isUnlocked = false, isCompleted = false, completedAt = null, isReviewable = false
            )
        )
    }
}
