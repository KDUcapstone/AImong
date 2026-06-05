package com.kduniv.aimong.feature.auth.presentation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.kduniv.aimong.R
import com.kduniv.aimong.core.network.model.ParentRegisterResponse
import com.kduniv.aimong.databinding.BottomSheetChildRegisterSuccessBinding

class ChildRegisterSuccessBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetChildRegisterSuccessBinding? = null
    private val binding get() = _binding!!

    var onConfirmClick: (() -> Unit)? = null

    override fun getTheme(): Int = R.style.AimongBottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetChildRegisterSuccessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isCancelable = false

        val code = arguments?.getString(ARG_CODE) ?: ""
        val starterTickets = arguments?.getInt(ARG_TICKETS, 0) ?: 0

        if (starterTickets > 0) {
            binding.tvStarterTickets.visibility = View.VISIBLE
            binding.tvStarterTickets.text =
                getString(R.string.auth_register_success_starter_tickets_fmt, starterTickets)
        } else {
            binding.tvStarterTickets.visibility = View.GONE
        }

        binding.tvConnectCode.text = code

        binding.btnCopyCode.setOnClickListener {
            val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("child_code", code))
            Snackbar.make(binding.root, R.string.child_register_code_copied, Snackbar.LENGTH_SHORT).show()
        }

        binding.btnConfirm.setOnClickListener {
            dismiss()
            onConfirmClick?.invoke()
        }

        val basePadBottom = binding.root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, basePadBottom + nav.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_NICKNAME = "nickname"
        private const val ARG_CODE = "code"
        private const val ARG_TICKETS = "tickets"

        fun newInstance(data: ParentRegisterResponse): ChildRegisterSuccessBottomSheet {
            return ChildRegisterSuccessBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_NICKNAME, data.nickname)
                    putString(ARG_CODE, data.code)
                    putInt(ARG_TICKETS, data.starterTickets)
                }
            }
        }
    }
}
