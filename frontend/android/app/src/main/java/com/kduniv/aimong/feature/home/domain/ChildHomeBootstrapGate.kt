package com.kduniv.aimong.feature.home.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 홈 첫 로드(파란 부트스트랩) 동안 [MainActivity] 하단 탭을 숨긴다.
 * Fragment 오버레이는 NavHost 영역만 덮으므로, 탭은 Activity에서 같이 제어한다.
 */
@Singleton
class ChildHomeBootstrapGate @Inject constructor() {

    private val _suppressChildBottomNav = MutableStateFlow(true)
    val suppressChildBottomNav: StateFlow<Boolean> = _suppressChildBottomNav.asStateFlow()

    fun setSuppressChildBottomNav(suppress: Boolean) {
        _suppressChildBottomNav.value = suppress
    }
}
