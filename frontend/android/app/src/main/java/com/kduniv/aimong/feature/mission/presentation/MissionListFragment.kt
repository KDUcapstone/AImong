package com.kduniv.aimong.feature.mission.presentation

import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.databinding.FragmentMissionListBinding
import com.kduniv.aimong.feature.mission.domain.model.Mission
import com.kduniv.aimong.feature.mission.domain.model.MissionStarLevel
import com.kduniv.aimong.feature.mission.domain.model.MissionProgress
import com.kduniv.aimong.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MissionListFragment : BaseFragment<FragmentMissionListBinding>(FragmentMissionListBinding::inflate) {

    private val viewModel: MissionListViewModel by viewModels()
    private lateinit var missionAdapter: MissionListAdapter

    override fun initView() {
        initRecyclerView()

        binding.btnRetryMissions.setOnClickListener {
            binding.layoutErrorState.visibility = View.GONE
            viewModel.refreshMissions()
        }
    }

    private fun initRecyclerView() {
        missionAdapter = MissionListAdapter { mission -> showStarLevelPicker(mission) }
        binding.rvMissions.apply {
            adapter = missionAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun showStarLevelPicker(mission: Mission) {
        val candidates = mission.starLevels.filter { it.isPlayable || it.isReviewOnly }
            .sortedBy { it.starLevel }
        if (candidates.isEmpty()) {
            Toast.makeText(requireContext(), R.string.mission_no_playable_star_level, Toast.LENGTH_SHORT).show()
            return
        }
        val labels = candidates.map { starLabel(it) }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(mission.title)
            .setItems(labels) { _, which ->
                val sl = candidates[which]
                navigateToQuiz(mission.missionId, sl.starLevel)
            }
            .show()
    }

    private fun starLabel(sl: MissionStarLevel): String {
        val ctx = requireContext()
        return "${sl.label} (${sl.completedSetCount}/${sl.totalSetCount})"
    }

    private fun navigateToQuiz(missionId: String, starLevel: Int) {
        findNavController().navigate(
            R.id.quizFragment,
            bundleOf(
                "entrySetId" to "",
                "missionId" to missionId,
                "starLevel" to starLevel
            )
        )
    }

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is MissionListUiState.Loading -> {
                            binding.pbLoading.visibility = View.VISIBLE
                            binding.rvMissions.visibility = View.GONE
                            binding.layoutEmptyState.visibility = View.GONE
                            binding.layoutErrorState.visibility = View.GONE
                        }
                        is MissionListUiState.Success -> {
                            binding.pbLoading.visibility = View.GONE
                            binding.layoutErrorState.visibility = View.GONE
                            val missions = state.missions
                            if (missions.isEmpty()) {
                                binding.rvMissions.visibility = View.GONE
                                binding.layoutEmptyState.visibility = View.VISIBLE
                                missionAdapter.submitList(emptyList())
                            } else {
                                binding.rvMissions.visibility = View.VISIBLE
                                binding.layoutEmptyState.visibility = View.GONE
                                missionAdapter.submitList(missions)
                            }
                        }
                        is MissionListUiState.Error -> {
                            binding.pbLoading.visibility = View.GONE
                            binding.rvMissions.visibility = View.GONE
                            binding.layoutEmptyState.visibility = View.GONE
                            binding.layoutErrorState.visibility = View.VISIBLE
                            binding.tvErrorMessage.text = state.message
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun bindStageProgress(progress: MissionProgress) {
        // Mock stage progress functionality removed based on UI update
    }
}
