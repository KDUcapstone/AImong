package com.kduniv.aimong.feature.home.presentation

import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.kduniv.aimong.R
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.core.util.setOnScaleTouchListener
import com.kduniv.aimong.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : BaseFragment<FragmentHomeBinding>(FragmentHomeBinding::inflate) {

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var homeLayoutBinder: HomeLayoutBinder

    override fun onResume() {
        super.onResume()
        viewModel.onHomeResumed()
    }

    override fun initView() {
        homeLayoutBinder = HomeLayoutBinder(
            binding = binding,
            layoutInflater = layoutInflater,
            getProfileLabel = { viewModel.getProfileLabel(it) },
            petNameDefault = getString(R.string.home_pet_name_default),
            onNavigateQuiz = { missionId ->
                findNavController().navigate(
                    HomeFragmentDirections.actionHomeFragmentToQuizFragment(missionId)
                )
            },
            onSelectLearningTab = {
                val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottom_nav)
                bottomNav.selectedItemId = R.id.learningFragment
            },
            onOpenQuest = { openQuestList() }
        )
        binding.tvChipTicket.setOnClickListener { openGacha() }
        binding.tvChipStreak.setOnClickListener { openStreakSheet() }
    }

    private fun openGacha() {
        findNavController().navigate(R.id.action_homeFragment_to_gachaFragment)
    }

    private fun openQuestList() {
        QuestListBottomSheet.newInstance().show(parentFragmentManager, "quest_list")
    }

    private fun openStreakSheet() {
        val streak = viewModel.uiState.value.streakDays
        StreakCalendarBottomSheet.newInstance(streak).show(parentFragmentManager, "streak_calendar")
    }

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.root.post {
                        homeLayoutBinder.bind(state)
                        state.errorMessage?.let { msg ->
                            Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                            viewModel.consumeError()
                        }
                        state.subtleNotice?.let { msg ->
                            Snackbar.make(binding.root, msg, Snackbar.LENGTH_SHORT).show()
                            viewModel.consumeSubtleNotice()
                        }
                    }
                }
            }
        }
    }
}
