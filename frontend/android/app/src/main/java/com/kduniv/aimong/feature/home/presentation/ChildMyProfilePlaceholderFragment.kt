package com.kduniv.aimong.feature.home.presentation

import com.kduniv.aimong.feature.home.presentation.my.ChildMyProfileFragment
import dagger.hilt.android.AndroidEntryPoint

/**
 * 이전 빌드의 FragmentManager 복원 호환용.
 * 신규 화면은 [ChildMyProfileFragment]를 사용한다.
 */
@AndroidEntryPoint
class ChildMyProfilePlaceholderFragment : ChildMyProfileFragment()
