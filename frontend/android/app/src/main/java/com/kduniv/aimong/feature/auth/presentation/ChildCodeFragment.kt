package com.kduniv.aimong.feature.auth.presentation

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.kduniv.aimong.MainActivity
import com.kduniv.aimong.R
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.core.util.setOnScaleTouchListener
import com.kduniv.aimong.databinding.FragmentChildCodeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ChildCodeFragment : BaseFragment<FragmentChildCodeBinding>(FragmentChildCodeBinding::inflate) {

    private val viewModel: ChildLoginViewModel by viewModels()

    override fun initView() {
        binding.btnBack.apply {
            setOnScaleTouchListener()
            setOnClickListener {
                findNavController().popBackStack(R.id.roleSelectFragment, false)
            }
        }

        binding.btnLogin.apply {
            setOnScaleTouchListener()
            setOnClickListener { login() }
        }

        // 옵션 A: 실제 입력(et_code)은 유지하고, 6칸 박스는 표시용으로 동기화한다.
        installCodeBoxes()
    }

    private fun installCodeBoxes() {
        val boxes = listOf(
            binding.tvCodeBox1,
            binding.tvCodeBox2,
            binding.tvCodeBox3,
            binding.tvCodeBox4,
            binding.tvCodeBox5,
            binding.tvCodeBox6
        )

        fun syncBoxes(text: String) {
            val digits = text.filter { it.isDigit() }.take(6)
            for (i in 0 until 6) {
                boxes[i].text = digits.getOrNull(i)?.toString().orEmpty()
            }
            // 입력이 6자리일 때만 버튼이 '살아있는 느낌'이 나도록 투명도만 보정(동작은 기존 login()에서 검증)
            binding.btnLogin.alpha = if (digits.length == 6) 1f else 0.6f
        }

        syncBoxes(binding.etCode.text?.toString().orEmpty())
        binding.btnLogin.alpha = 0.6f

        binding.layoutCodeBoxes.setOnClickListener {
            binding.etCode.requestFocus()
            val imm = requireContext().getSystemService(InputMethodManager::class.java)
            imm?.showSoftInput(binding.etCode, 0)
        }

        binding.etCode.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                syncBoxes(s?.toString().orEmpty())
            }
        })
    }

    private fun login() {
        val code = binding.etCode.text?.toString()?.trim().orEmpty()
        if (code.length != 6 || code.any { !it.isDigit() }) {
            Snackbar.make(binding.root, R.string.auth_child_code_invalid, Snackbar.LENGTH_SHORT).show()
            return
        }
        binding.btnLogin.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loginWithCode(code).fold(
                onSuccess = {
                    binding.btnLogin.isEnabled = true
                    val intent = Intent(requireContext(), MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        putExtra(MainActivity.EXTRA_IS_RESTART, true)
                    }
                    startActivity(intent)
                },
                onFailure = { e ->
                    binding.btnLogin.isEnabled = true
                    Snackbar.make(
                        binding.root,
                        e.message ?: getString(R.string.auth_child_login_failed),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            )
        }
    }

    override fun initObserver() {}
}
