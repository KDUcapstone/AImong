package com.kduniv.aimong.feature.chat

import android.text.Editable
import android.text.TextWatcher
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.kduniv.aimong.R
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.databinding.FragmentChatBinding
import com.kduniv.aimong.feature.chat.presentation.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ChatFragment : BaseFragment<FragmentChatBinding>(FragmentChatBinding::inflate) {

    private val viewModel: ChatViewModel by viewModels()

    @Inject
    lateinit var chatForegroundTracker: ChatForegroundTracker

    private val chatAdapter = ChatMessageAdapter()

    override fun initView() {
        binding.rvChat.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvChat.adapter = chatAdapter

        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val len = s?.length ?: 0
                viewModel.onInputChanged(len)
                binding.tvCharCounter.isVisible = len >= 150
                if (binding.tvCharCounter.isVisible) {
                    binding.tvCharCounter.text = getString(R.string.chat_char_counter_fmt, len)
                }
            }
        })

        binding.btnSend.setOnClickListener {
            viewModel.sendMessage(binding.etMessage.text?.toString().orEmpty())
            binding.etMessage.text = null
        }
    }

    override fun onResume() {
        super.onResume()
        chatForegroundTracker.isChatVisible = true
    }

    override fun onPause() {
        chatForegroundTracker.isChatVisible = false
        super.onPause()
    }

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    chatAdapter.submitList(state.messages) {
                        if (state.messages.isNotEmpty()) {
                            binding.rvChat.scrollToPosition(state.messages.size - 1)
                        }
                    }
                    binding.btnSend.isEnabled = state.sendEnabled
                    binding.btnSend.alpha = if (state.sendEnabled) 1f else 0.45f
                    binding.etMessage.isEnabled = !state.isLoading

                    val rc = state.remainingCalls
                    binding.tvRemainingCalls.isVisible = true
                    binding.tvRemainingCalls.text = when {
                        rc == null -> getString(R.string.chat_remaining_calls_unknown)
                        rc == 0 -> getString(R.string.chat_remaining_calls_zero)
                        else -> getString(R.string.chat_remaining_calls_fmt, rc)
                    }

                    state.errorMessage?.let { msg ->
                        Snackbar.make(binding.root, msg, Snackbar.LENGTH_LONG).show()
                        viewModel.clearError()
                    }
                }
            }
        }
    }
}
