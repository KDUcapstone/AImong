package com.kduniv.aimong.feature.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kduniv.aimong.databinding.ItemChatMessagePetBinding
import com.kduniv.aimong.databinding.ItemChatMessageUserBinding
import com.kduniv.aimong.feature.chat.presentation.ChatMessage

class ChatMessageAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(Diff) {

    var petDisplayName: String = "에이몽"
    var petStage: String = "GROWTH"

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).isMine) VIEW_TYPE_USER else VIEW_TYPE_PET

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_USER -> UserVh(
                ItemChatMessageUserBinding.inflate(inflater, parent, false)
            )
            else -> PetVh(
                ItemChatMessagePetBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is PetVh -> holder.bind(item, petStage)
            is UserVh -> holder.bind(item)
        }
    }

    private class PetVh(
        private val binding: ItemChatMessagePetBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChatMessage, stage: String) {
            binding.tvMessage.text = item.text
            val emoji = ChatPetUiHelper.stageEmoji(stage)
            val useEmoji = stage.equals("EGG", ignoreCase = true)
            binding.lavPetAvatar.visibility = if (useEmoji) View.GONE else View.VISIBLE
            binding.tvPetAvatarEmoji.visibility = if (useEmoji) View.VISIBLE else View.GONE
            if (useEmoji) {
                binding.tvPetAvatarEmoji.text = emoji
            }
        }
    }

    private class UserVh(
        private val binding: ItemChatMessageUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChatMessage) {
            binding.tvMessage.text = item.text
        }
    }

    companion object {
        private const val VIEW_TYPE_PET = 0
        private const val VIEW_TYPE_USER = 1

        private val Diff = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(a: ChatMessage, b: ChatMessage): Boolean = a.id == b.id

            override fun areContentsTheSame(a: ChatMessage, b: ChatMessage): Boolean = a == b
        }
    }
}
