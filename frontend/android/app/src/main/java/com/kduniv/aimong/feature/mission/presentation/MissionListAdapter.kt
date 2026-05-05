package com.kduniv.aimong.feature.mission.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kduniv.aimong.R
import com.kduniv.aimong.databinding.ItemMissionBinding
import com.kduniv.aimong.feature.mission.domain.model.Mission

class MissionListAdapter(
    private val onMissionClick: (Mission) -> Unit
) : ListAdapter<Mission, MissionListAdapter.MissionViewHolder>(MissionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MissionViewHolder {
        val binding = ItemMissionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MissionViewHolder(binding)
    }

    override fun onBindViewHolder(parent: MissionViewHolder, position: Int) {
        parent.bind(getItem(position))
    }

    inner class MissionViewHolder(private val binding: ItemMissionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(mission: Mission) {
            binding.tvStage.text = binding.root.context.getString(R.string.quiz_stage_label, mission.stage)
            binding.tvTitle.text = mission.title
            binding.tvDescription.text = mission.description

            binding.tvReviewBadge.visibility =
                if (mission.isUnlocked && mission.isReviewable) View.VISIBLE else View.GONE

            // 상태 아이콘 및 레이아웃 처리
            if (!mission.isUnlocked) {
                binding.viewLockOverlay.visibility = View.VISIBLE
                binding.ivLock.visibility = View.VISIBLE
                binding.ivLock.setImageResource(R.drawable.ic_lock)
                binding.ivStatus.visibility = View.GONE
                binding.root.isClickable = false
                binding.root.setOnClickListener(null)
            } else {
                binding.viewLockOverlay.visibility = View.GONE
                binding.ivLock.visibility = View.GONE
                binding.ivStatus.visibility = View.VISIBLE

                if (mission.isCompleted) {
                    binding.ivStatus.setImageResource(R.drawable.ic_check_circle)
                    binding.ivStatus.setColorFilter(ContextCompat.getColor(binding.root.context, R.color.quiz_mint))
                } else {
                    binding.ivStatus.setImageResource(R.drawable.ic_play_arrow)
                    binding.ivStatus.setColorFilter(ContextCompat.getColor(binding.root.context, R.color.white))
                }

                binding.root.setOnClickListener { onMissionClick(mission) }
            }
        }
    }

    class MissionDiffCallback : DiffUtil.ItemCallback<Mission>() {
        override fun areItemsTheSame(oldItem: Mission, newItem: Mission): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Mission, newItem: Mission): Boolean =
            oldItem == newItem
    }
}
