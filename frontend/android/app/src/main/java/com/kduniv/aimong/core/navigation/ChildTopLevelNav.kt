package com.kduniv.aimong.core.navigation

import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavOptions

/**
 * 자녀 하단 탭(홈·챗봇·수집·MY) 전환 규칙 — [MainActivity] 바텀 네비와 동일.
 */
object ChildTopLevelNav {

    fun NavController.navigateToChildTopLevel(@IdRes destinationId: Int) {
        val options = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setRestoreState(true)
            .setPopUpTo(graph.findStartDestination().id, false, true)
            .build()
        navigate(destinationId, null, options)
    }
}
