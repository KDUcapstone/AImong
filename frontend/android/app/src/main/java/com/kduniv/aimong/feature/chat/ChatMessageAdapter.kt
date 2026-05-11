package com.kduniv.aimong.feature.chat

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kduniv.aimong.R
import com.kduniv.aimong.databinding.ItemChatMessageBinding
import com.kduniv.aimong.feature.chat.presentation.ChatMessage

class ChatMessageAdapter : ListAdapter<ChatMessage, ChatMessageAdapter.Vh>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Vh {
        val binding = ItemChatMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Vh(binding)
    }

    override fun onBindViewHolder(holder: Vh, position: Int) {
        holder.bind(getItem(position))
    }

    class Vh(private val binding: ItemChatMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ChatMessage) {
            binding.tvMessage.text = item.text
            val ctx = binding.root.context
            if (item.isMine) {
                binding.tvMessage.setBackgroundResource(R.drawable.bg_chat_bubble_mine)
                binding.tvMessage.setTextColor(Color.WHITE)
            } else {
                binding.tvMessage.setBackgroundResource(R.drawable.bg_chat_bubble_other)
                binding.tvMessage.setTextColor(ContextCompat.getColor(ctx, R.color.text_white))
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(a: ChatMessage, b: ChatMessage): Boolean = a.id == b.id

        override fun areContentsTheSame(a: ChatMessage, b: ChatMessage): Boolean = a == b
    }
}
