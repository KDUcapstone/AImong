package com.kduniv.aimong.core.navigation

import androidx.annotation.IdRes
import com.kduniv.aimong.R
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions

/**
 * 자녀 하단 탭(홈·챗봇·수집·MY) 전환 규칙 — [MainActivity] 바텀 네비와 동일.
 */
object ChildTopLevelNav {

    private val topLevelActionIds = mapOf(
        R.id.homeFragment to R.id.action_global_child_home,
        R.id.chatFragment to R.id.action_global_child_chat,
        R.id.gachaFragment to R.id.action_global_child_gacha,
        R.id.myProfileFragment to R.id.action_global_child_myProfile,
    )

    /** 퀴즈 화면에서는 하단 탭을 숨긴다. */
    fun shouldHideBottomNav(@IdRes destinationId: Int?): Boolean =
        destinationId == R.id.quizFragment

    /** 퀴즈·알림 설정 등 — 하단 탭이 가리키는 최상위 destination */
    fun mapDestinationToTab(@IdRes destinationId: Int?): Int? = when (destinationId) {
        R.id.homeFragment,
        R.id.quizFragment,
        -> R.id.homeFragment
        R.id.chatFragment -> R.id.chatFragment
        R.id.gachaFragment -> R.id.gachaFragment
        R.id.myProfileFragment,
        R.id.notificationSettingsFragment,
        -> R.id.myProfileFragment
        else -> null
    }

    /**
     * 하단 탭 선택/재선택 공통 처리.
     * 탭 UI만 MY로 남고 실제 화면은 홈인 경우, popBackStack만 하면 MY가 스택에 없어 무시되므로 navigate로 폴백한다.
     */
    fun NavController.onChildBottomNavTap(@IdRes tabDestinationId: Int) {
        val currentId = currentDestination?.id
        val onThisTab = currentId == tabDestinationId ||
            mapDestinationToTab(currentId) == tabDestinationId
        if (onThisTab) {
            if (!popBackStack(tabDestinationId, false)) {
                navigateToChildTopLevel(tabDestinationId)
            }
        } else {
            navigateToChildTopLevel(tabDestinationId)
        }
    }

    fun NavController.navigateToChildTopLevel(@IdRes destinationId: Int) {
        val options = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setRestoreState(true)
            .setPopUpTo(graph.findStartDestination().id, false, true)
            .build()
        val actionId = topLevelActionIds[destinationId]
        try {
            if (actionId != null) {
                navigate(actionId, null, options)
            } else {
                navigate(destinationId, null, options)
            }
        } catch (_: IllegalArgumentException) {
            // 글로벌 action이 현재 destination에서 보이지 않을 때 직접 destination으로 폴백
            navigate(destinationId, null, options)
        }
    }
}
