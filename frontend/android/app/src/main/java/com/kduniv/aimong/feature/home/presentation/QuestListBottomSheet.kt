package com.kduniv.aimong.feature.home.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kduniv.aimong.R
import com.kduniv.aimong.databinding.DialogQuestListBinding

class QuestListBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogQuestListBinding? = null
    private val binding get() = _binding!!

    override fun getTheme(): Int = R.style.AimongBottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogQuestListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val dummyQuests = listOf(
            QuestItemUiState("q1", "출석하기", "+10 XP", null, isCompleted = true, canStart = false),
            QuestItemUiState("q2", "친구와 대화", "+15 XP", null, isCompleted = false, canStart = true),
            QuestItemUiState("q3", "복습 미션", "+20 XP", null, isCompleted = false, canStart = false)
        )

        binding.rvQuests.layoutManager = LinearLayoutManager(requireContext())
        binding.rvQuests.adapter = QuestListAdapter(dummyQuests)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): QuestListBottomSheet {
            return QuestListBottomSheet()
        }
    }
}