package com.kduniv.aimong.feature.chat

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kduniv.aimong.databinding.ItemChatMessagePetBinding
import com.kduniv.aimong.databinding.ItemChatMessageTypingBinding
import com.kduniv.aimong.databinding.ItemChatMessageUserBinding
import com.kduniv.aimong.feature.chat.presentation.ChatMessage
import com.kduniv.aimong.feature.gacha.PetArtAssets

class ChatMessageAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(Diff) {

    var petAvatarEmoji: String = "✨"
    var petType: String = ""
    var petStage: String = "EGG"

    override fun getItemViewType(position: Int): Int = when {
        getItem(position).isTyping -> VIEW_TYPE_TYPING
        getItem(position).isMine -> VIEW_TYPE_USER
        else -> VIEW_TYPE_PET
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_USER -> UserVh(
                ItemChatMessageUserBinding.inflate(inflater, parent, false)
            )
            VIEW_TYPE_TYPING -> TypingVh(
                ItemChatMessageTypingBinding.inflate(inflater, parent, false)
            )
            else -> PetVh(
                ItemChatMessagePetBinding.inflate(inflater, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is PetVh -> holder.bind(item, petType, petStage, petAvatarEmoji)
            is TypingVh -> holder.bind(petType, petStage, petAvatarEmoji)
            is UserVh -> holder.bind(item)
        }
    }

    private class PetVh(
        private val binding: ItemChatMessagePetBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChatMessage, petType: String, petStage: String, avatarEmoji: String) {
            binding.tvMessage.text = item.text
            PetArtAssets.bindSprite(
                image = binding.ivPetAvatarSprite,
                emojiFallback = binding.tvPetAvatarEmoji,
                petType = petType,
                stage = petStage,
                emoji = avatarEmoji,
            )
        }
    }

    private class TypingVh(
        private val binding: ItemChatMessageTypingBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(petType: String, petStage: String, avatarEmoji: String) {
            PetArtAssets.bindSprite(
                image = binding.ivPetAvatarSprite,
                emojiFallback = binding.tvPetAvatarEmoji,
                petType = petType,
                stage = petStage,
                emoji = avatarEmoji,
            )
        }
    }

    private class UserVh(
        private val binding: ItemChatMessageUserBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ChatMessage) {
            binding.tvMessage.text = item.text
        }
    }

    companion object {
        private const val VIEW_TYPE_PET = 0
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_TYPING = 2

        private val Diff = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(a: ChatMessage, b: ChatMessage): Boolean = a.id == b.id

            override fun areContentsTheSame(a: ChatMessage, b: ChatMessage): Boolean = a == b
        }
    }
}
