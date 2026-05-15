package com.kduniv.aimong.feature.parent.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kduniv.aimong.R
import com.kduniv.aimong.core.network.model.ParentChildItem
import com.kduniv.aimong.databinding.ItemParentChildSheetRowBinding

class ParentChildSheetAdapter(
    private val onSelectChild: (String) -> Unit
) : ListAdapter<ParentChildSheetRow, ParentChildSheetAdapter.ViewHolder>(DiffCallback) {

    var selectedChildId: String? = null
        set(value) {
            if (field == value) return
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemParentChildSheetRowBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemParentChildSheetRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(row: ParentChildSheetRow) {
            val item = row.item
            val ctx = binding.root.context
            val selected = item.childId == selectedChildId

            binding.tvChildName.text = item.nickname
            binding.tvChildSubtitle.text = row.subtitle
            binding.ivSelectedCheck.visibility = if (selected) View.VISIBLE else View.GONE
            binding.rootChildSheetRow.setBackgroundResource(
                if (selected) R.drawable.bg_parent_child_sheet_row_selected
                else R.drawable.bg_parent_child_sheet_row
            )

            binding.root.setOnClickListener { onSelectChild(item.childId) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<ParentChildSheetRow>() {
            override fun areItemsTheSame(oldItem: ParentChildSheetRow, newItem: ParentChildSheetRow): Boolean =
                oldItem.item.childId == newItem.item.childId

            override fun areContentsTheSame(oldItem: ParentChildSheetRow, newItem: ParentChildSheetRow): Boolean =
                oldItem == newItem
        }
    }
}

data class ParentChildSheetRow(
    val item: ParentChildItem,
    val subtitle: String
)
