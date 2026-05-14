package com.kduniv.aimong.core.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

abstract class BaseFragment<VB : ViewBinding>(
    private val inflate: (LayoutInflater, ViewGroup?, Boolean) -> VB
) : Fragment() {

    protected var _binding: VB? = null
    protected val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (shouldApplySystemBarInsets()) {
            applySystemBarInsets(insetTargetView())
        }
        initView()
        initObserver()
    }

    /**
     * 시스템 상단바(상태바) 등에 UI가 가려지지 않도록 기본 인셋 패딩을 적용한다.
     * API/흐름과 무관한 UI 레이아웃 보정만 수행한다.
     */
    protected open fun shouldApplySystemBarInsets(): Boolean = true

    /**
     * 기본은 Fragment 루트 전체에 인셋을 적용한다.
     * 특정 화면은 오버라이드해서 툴바 컨테이너 등으로 좁힐 수 있다.
     */
    protected open fun insetTargetView(): View = binding.root

    private fun applySystemBarInsets(target: View) {
        val baseTop = target.paddingTop
        val baseBottom = target.paddingBottom
        val baseLeft = target.paddingLeft
        val baseRight = target.paddingRight

        ViewCompat.setOnApplyWindowInsetsListener(target) { v, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                baseLeft + sysBars.left,
                baseTop + sysBars.top,
                baseRight + sysBars.right,
                baseBottom + sysBars.bottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(target)
    }

    abstract fun initView()
    abstract fun initObserver()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
