package com.kduniv.aimong.feature.dev.mock

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.kduniv.aimong.MainActivity
import com.kduniv.aimong.R
import com.kduniv.aimong.core.local.SessionManager
import com.kduniv.aimong.core.ui.BaseFragment
import com.kduniv.aimong.core.util.setOnScaleTouchListener
import com.kduniv.aimong.databinding.FragmentChildCodeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/** [ChildCodeFragment]와 동일 레이아웃 — 서버 로그인 없이 세션만 저장(목업). */
@AndroidEntryPoint
class MockChildCodeFragment : BaseFragment<FragmentChildCodeBinding>(FragmentChildCodeBinding::inflate) {

    @Inject
    lateinit var sessionManager: SessionManager

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
            sessionManager.saveSession("CHILD", 1, MockChildSession.TOKEN)
            binding.btnLogin.isEnabled = true
            val intent = Intent(requireContext(), MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(MainActivity.EXTRA_IS_RESTART, true)
            }
            startActivity(intent)
        }
    }

    override fun initObserver() {}
}
