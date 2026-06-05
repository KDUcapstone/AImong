package com.kduniv.aimong.feature.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kduniv.aimong.databinding.ItemChatMessagePetBinding
import com.kduniv.aimong.databinding.ItemChatMessageTypingBinding
import com.kduniv.aimong.databinding.ItemChatMessageUserBinding
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.kduniv.aimong.R
import com.kduniv.aimong.feature.chat.presentation.ChatMessage
import com.kduniv.aimong.feature.gacha.PetArtAssets

class ChatMessageAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(Diff) {

    var petAvatarEmoji: String = "✨"
    var petType: String = ""
    var petStage: String = "EGG"
    var onSaveImage: ((String) -> Unit)? = null

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
        onBindViewHolder(holder, position, mutableListOf())
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>,
    ) {
        val item = getItem(position)
        if (payloads.isNotEmpty() && payloads.any { it == PAYLOAD_PET_AVATAR }) {
            when (holder) {
                is PetVh -> holder.bindPetAvatar(petType, petStage, petAvatarEmoji)
                is TypingVh -> holder.bindPetAvatar(petType, petStage, petAvatarEmoji)
                else -> Unit
            }
            return
        }
        when (holder) {
            is PetVh -> holder.bind(item, petType, petStage, petAvatarEmoji, onSaveImage)
            is TypingVh -> holder.bind(item, petType, petStage, petAvatarEmoji)
            is UserVh -> holder.bind(item)
        }
    }

    /** 펫 아바타만 바뀔 때 해당 행만 갱신 — 전체 [notifyDataSetChanged] 대신 */
    fun updatePetAvatar(petType: String, petStage: String, petAvatarEmoji: String) {
        val changed = this.petType != petType ||
            this.petStage != petStage ||
            this.petAvatarEmoji != petAvatarEmoji
        if (!changed) return
        this.petType = petType
        this.petStage = petStage
        this.petAvatarEmoji = petAvatarEmoji
        for (i in 0 until itemCount) {
            when (getItemViewType(i)) {
                VIEW_TYPE_PET, VIEW_TYPE_TYPING -> notifyItemChanged(i, PAYLOAD_PET_AVATAR)
            }
        }
    }

    private class PetVh(
        private val binding: ItemChatMessagePetBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: ChatMessage,
            petType: String,
            petStage: String,
            avatarEmoji: String,
            onSave: ((String) -> Unit)?,
        ) {
            val imageUri = item.imageDataUri
            if (imageUri != null) {
                binding.layoutGeneratedImage.isVisible = true
                binding.tvMessage.isVisible = item.text.isNotBlank()
                binding.tvMessage.text = item.text
                Glide.with(binding.ivGeneratedImage)
                    .load(imageUri)
                    .into(binding.ivGeneratedImage)
                val longClickListener = View.OnLongClickListener { anchor ->
                    showImageSaveMenu(anchor, imageUri, onSave)
                    true
                }
                binding.layoutGeneratedImage.setOnLongClickListener(longClickListener)
                binding.ivGeneratedImage.setOnLongClickListener(longClickListener)
            } else {
                binding.layoutGeneratedImage.isVisible = false
                binding.layoutGeneratedImage.setOnLongClickListener(null)
                binding.ivGeneratedImage.setOnLongClickListener(null)
                Glide.with(binding.ivGeneratedImage).clear(binding.ivGeneratedImage)
                binding.tvMessage.isVisible = true
                binding.tvMessage.text = item.text
            }
            bindPetAvatar(petType, petStage, avatarEmoji)
        }

        fun bindPetAvatar(petType: String, petStage: String, avatarEmoji: String) {
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

        fun bind(item: ChatMessage, petType: String, petStage: String, avatarEmoji: String) {
            binding.tvTypingLabel.text = binding.root.context.getString(
                if (item.isImageTyping) R.string.chat_typing_image else R.string.chat_typing
            )
            bindPetAvatar(petType, petStage, avatarEmoji)
        }

        fun bindPetAvatar(petType: String, petStage: String, avatarEmoji: String) {
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
        private fun showImageSaveMenu(
            anchor: View,
            imageUri: String,
            onSave: ((String) -> Unit)?,
        ) {
            PopupMenu(anchor.context, anchor).apply {
                menu.add(0, MENU_SAVE_IMAGE, 0, anchor.context.getString(R.string.chat_image_download))
                setOnMenuItemClickListener { item ->
                    if (item.itemId == MENU_SAVE_IMAGE) {
                        onSave?.invoke(imageUri)
                        true
                    } else {
                        false
                    }
                }
            }.show()
        }

        private const val MENU_SAVE_IMAGE = 1
        private const val PAYLOAD_PET_AVATAR = "pet_avatar"
        private const val VIEW_TYPE_PET = 0
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_TYPING = 2

        private val Diff = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(a: ChatMessage, b: ChatMessage): Boolean = a.id == b.id

            override fun areContentsTheSame(a: ChatMessage, b: ChatMessage): Boolean = a == b
        }
    }
}
