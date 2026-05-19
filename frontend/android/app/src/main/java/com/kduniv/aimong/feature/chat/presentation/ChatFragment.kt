package com.kduniv.aimong.feature.chat.presentation

import android.text.Editable
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.BackgroundColorSpan
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.kduniv.aimong.R
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.databinding.FragmentChatBinding
import com.kduniv.aimong.feature.chat.ChatForegroundTracker
import com.kduniv.aimong.feature.chat.ChatMessageAdapter
import com.kduniv.aimong.feature.gacha.PetArtAssets
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

private class ChatPrivacyHighlightSpan(color: Int) : BackgroundColorSpan(color)

@AndroidEntryPoint
class ChatFragment : BaseFragment<FragmentChatBinding>(FragmentChatBinding::inflate) {

    private val viewModel: ChatViewModel by viewModels()

    @Inject
    lateinit var chatForegroundTracker: ChatForegroundTracker

    private val chatAdapter = ChatMessageAdapter()

    private var suppressInputCallback = false

    private var privacyDialog: AlertDialog? = null

    override fun shouldApplySystemBarInsets(): Boolean = false

    override fun initView() {
        binding.rvChat.layoutManager = LinearLayoutManager(requireContext()).apply {
            stackFromEnd = true
        }
        binding.rvChat.adapter = chatAdapter
        applyChatImeInsets()

        binding.etMessage.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) scrollChatToBottom()
        }
        binding.etMessage.setOnClickListener { scrollChatToBottom() }

        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (suppressInputCallback) return
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
        }
    }

    override fun onResume() {
        super.onResume()
        chatForegroundTracker.isChatVisible = true
        viewModel.refreshEquippedPet()
    }

    override fun onPause() {
        chatForegroundTracker.isChatVisible = false
        super.onPause()
    }

    override fun initObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val prevEmoji = chatAdapter.petAvatarEmoji
                    chatAdapter.petAvatarEmoji = state.petAvatarEmoji
                    chatAdapter.petType = state.petType
                    chatAdapter.petStage = state.petStage
                    binding.tvChatTitle.text = state.petDisplayName
                    PetArtAssets.bindEquipped(
                        image = binding.ivHeaderPetSprite,
                        emojiFallback = binding.tvHeaderPetEmoji,
                        petType = state.petType,
                        stage = state.petStage,
                        grade = state.petGrade,
                        lottie = binding.lavHeaderPet,
                    )
                    if (prevEmoji != state.petAvatarEmoji) {
                        chatAdapter.notifyDataSetChanged()
                    }
                    chatAdapter.submitList(state.messages) {
                        scrollChatToBottom()
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

                    if (state.pendingInputClear) {
                        binding.etMessage.text = null
                        viewModel.acknowledgeInputClear()
                    }

                    if (state.privacyPrompt != null && privacyDialog?.isShowing != true) {
                            var choiceMade = false
                            privacyDialog = MaterialAlertDialogBuilder(requireContext())
                                .setTitle(R.string.chat_privacy_dialog_title)
                                .setMessage(R.string.chat_privacy_dialog_message)
                                .setNegativeButton(R.string.chat_privacy_action_cancel) { _, _ ->
                                    choiceMade = true
                                    viewModel.onPrivacySendCancelled()
                                }
                                .setPositiveButton(R.string.chat_privacy_action_mask) { _, _ ->
                                    choiceMade = true
                                    viewModel.onPrivacyMaskedSend()
                                }
                                .setOnDismissListener {
                                    if (!choiceMade) {
                                        viewModel.onPrivacySendCancelled()
                                    }
                                    privacyDialog = null
                                }
                                .create()
                            privacyDialog?.show()
                    }

                    val highlightRanges = state.privacyPrompt?.highlightRanges
                        ?: state.privacyHighlightRanges
                    if (highlightRanges.isEmpty()) {
                        removePrivacyHighlightsFromInput()
                    } else {
                        applyPrivacyHighlights(highlightRanges)
                    }
                }
            }
        }
    }

    private fun applyChatImeInsets() {
        val header = binding.layoutChatHeader
        val inputColumn = binding.layoutInputColumn
        val headerPadH = header.paddingLeft
        val headerPadBottom = header.paddingBottom
        val inputPadH = inputColumn.paddingLeft
        val inputPadTop = inputColumn.paddingTop
        val inputBaseBottom = resources.getDimensionPixelSize(R.dimen.chat_input_column_padding_bottom)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())

            header.updatePadding(
                left = headerPadH,
                top = statusBars.top,
                right = headerPadH,
                bottom = headerPadBottom,
            )

            val keyboardLift = (ime.bottom - navBars.bottom).coerceAtLeast(0)
            inputColumn.updatePadding(
                left = inputPadH,
                top = inputPadTop,
                right = inputPadH,
                bottom = inputBaseBottom + keyboardLift,
            )

            if (keyboardLift > 0) {
                binding.root.post { scrollChatToBottom() }
            }
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    private fun scrollChatToBottom() {
        val count = chatAdapter.itemCount
        if (count > 0) {
            binding.rvChat.scrollToPosition(count - 1)
        }
    }

    private fun removePrivacyHighlightsFromInput() {
        val editable = binding.etMessage.text as? Editable ?: return
        suppressInputCallback = true
        try {
            editable.getSpans(0, editable.length, ChatPrivacyHighlightSpan::class.java)
                .forEach { editable.removeSpan(it) }
        } finally {
            suppressInputCallback = false
        }
    }

    private fun applyPrivacyHighlights(ranges: List<IntRange>) {
        val editable = binding.etMessage.text as? Editable ?: return
        suppressInputCallback = true
        try {
            editable.getSpans(0, editable.length, ChatPrivacyHighlightSpan::class.java)
                .forEach { editable.removeSpan(it) }
            val color = ContextCompat.getColor(requireContext(), R.color.chat_privacy_highlight)
            val n = editable.length
            for (range in ranges) {
                val start = range.first.coerceIn(0, n)
                val end = (range.last + 1).coerceIn(start, n)
                if (start < end) {
                    editable.setSpan(
                        ChatPrivacyHighlightSpan(color),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
        } finally {
            suppressInputCallback = false
        }
    }
}
