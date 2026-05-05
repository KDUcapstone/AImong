package com.kduniv.aimong.feature.auth.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
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
        
        // 클릭 외 바깥 영역 터치나 스와이프로 닫히지 않게 강제
        isCancelable = false

        val nickname = arguments?.getString(ARG_NICKNAME) ?: ""
        val code = arguments?.getString(ARG_CODE) ?: ""
        val tickets = arguments?.getInt(ARG_TICKETS) ?: 0

        binding.tvNickname.text = nickname
        binding.tvConnectCode.text = code
        binding.tvTickets.text = "${tickets}장"

        binding.btnConfirm.setOnClickListener {
            dismiss()
            onConfirmClick?.invoke()
        }
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
