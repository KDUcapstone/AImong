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

    fun NavController.navigateToChildTopLevel(@IdRes destinationId: Int) {
        val options = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setRestoreState(true)
            .setPopUpTo(graph.findStartDestination().id, false, true)
            .build()
        val actionId = topLevelActionIds[destinationId]
        if (actionId != null) {
            navigate(actionId, null, options)
        } else {
            navigate(destinationId, null, options)
        }
    }
}
