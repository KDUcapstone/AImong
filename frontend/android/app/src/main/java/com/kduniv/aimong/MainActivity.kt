package com.kduniv.aimong

import android.content.Intent
import android.net.Uri
import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.Checkable
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.NavOptions
import com.kduniv.aimong.core.navigation.ChildTopLevelNav
import com.kduniv.aimong.core.navigation.ChildTopLevelNav.navigateToChildTopLevel
import com.kduniv.aimong.core.navigation.ChildTopLevelNav.onChildBottomNavTap
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.kduniv.aimong.core.dev.UiMode
import com.kduniv.aimong.BuildConfig
import com.kduniv.aimong.core.network.AuthInterceptor
import com.kduniv.aimong.core.local.SessionManager
import com.kduniv.aimong.core.util.AppVersionUtils
import com.kduniv.aimong.feature.home.domain.repository.AppBootstrapRepository
import com.kduniv.aimong.feature.auth.domain.ChildSessionValidateUseCase
import com.kduniv.aimong.feature.auth.domain.RegisterChildFcmTokenUseCase
import com.kduniv.aimong.feature.auth.domain.RegisterParentFcmTokenUseCase
import com.kduniv.aimong.feature.parent.domain.ParentDashboardRefreshBus
import com.kduniv.aimong.feature.parent.domain.ParentDashboardRefreshTrigger
import com.kduniv.aimong.feature.parent.domain.SyncParentChildrenUseCase
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.kduniv.aimong.databinding.ActivityMainBinding
import com.kduniv.aimong.feature.chat.ChatHintNotifier
import com.kduniv.aimong.feature.home.domain.ChildHomeBootstrapGate
import com.kduniv.aimong.feature.onboarding.child.ChildGachaOnboardingController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        /** 로그인·세션 저장 직후 `MainActivity`를 다시 띄울 때 넣는 플래그(백스택·그래프 초기화). */
        const val EXTRA_IS_RESTART = "IS_RESTART"
        /** FCM 실세계 미션 승인 요청 탭 시 부모 대시보드·퀘스트 갱신 */
        const val EXTRA_OPEN_PARENT_CUSTOM_QUESTS = "open_parent_custom_quests"
        const val EXTRA_QUEST_COMPLETE_CHILD_ID = "quest_complete_child_id"
        const val EXTRA_FCM_NOTIFICATION_TYPE = "fcm_notification_type"
        const val EXTRA_FCM_TARGET = "fcm_target"
        const val FCM_TARGET_PARENT_DASHBOARD = "parent_dashboard"
        const val FCM_TARGET_CHILD_HOME = "child_home"
        const val FCM_TARGET_CHILD_GACHA = "child_gacha"
        private const val REQ_POST_NOTIFICATIONS = 1001
    }

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var registerParentFcmTokenUseCase: RegisterParentFcmTokenUseCase

    @Inject
    lateinit var registerChildFcmTokenUseCase: RegisterChildFcmTokenUseCase

    @Inject
    lateinit var syncParentChildrenUseCase: SyncParentChildrenUseCase

    @Inject
    lateinit var parentDashboardRefreshBus: ParentDashboardRefreshBus

    @Inject
    lateinit var chatHintNotifier: ChatHintNotifier

    @Inject
    lateinit var appBootstrapRepository: AppBootstrapRepository

    @Inject
    lateinit var childSessionValidateUseCase: ChildSessionValidateUseCase

    @Inject
    lateinit var childGachaOnboardingController: ChildGachaOnboardingController

    @Inject
    lateinit var childHomeBootstrapGate: ChildHomeBootstrapGate

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var mainBackPressedCallback: OnBackPressedCallback? = null
    private var exitConfirmDialog: AlertDialog? = null
    private var parentNavDestinationListener: NavController.OnDestinationChangedListener? = null
    private var childNavDestinationListener: NavController.OnDestinationChangedListener? = null
    /** 하단 탭 `selectedItemId` 프로그램 변경 시 `OnItemSelected` 재진입 방지 */
    private var suppressParentBottomNavItemSelected = false
    private var suppressChildBottomNavItemSelected = false

    /** [setupNavigation]에서 설정 — 부모 하단 네비 재동기화용 */
    private var navigationUserRole: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AuthInterceptor.resetLoginRedirectGate()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomNav) { v, insets ->
            val navBars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            v.setPadding(0, 0, 0, navBars.bottom)
            insets
        }

        requestNotificationPermissionIfNeeded()

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        if (navHostFragment != null) {
            navController = navHostFragment.navController
            setupNavigation(savedInstanceState)
        }

        if (!UiMode.useStubNav) {
            lifecycleScope.launch { runBootstrapIfNeeded() }
        }
        handleQuestCompleteNotificationIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleQuestCompleteNotificationIntent(intent)
        if (::navController.isInitialized) {
            lifecycleScope.launch {
                handleFcmNavigationIntent(intent, sessionManager.userRole.first())
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val perm = Manifest.permission.POST_NOTIFICATIONS
        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) return
        AlertDialog.Builder(this)
            .setTitle(R.string.notification_permission_title)
            .setMessage(R.string.notification_permission_message)
            .setPositiveButton(R.string.notification_permission_allow) { _, _ ->
                ActivityCompat.requestPermissions(this, arrayOf(perm), REQ_POST_NOTIFICATIONS)
            }
            .setNegativeButton(R.string.notification_permission_later, null)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQ_POST_NOTIFICATIONS) return
        val granted = grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        if (granted) return
        Snackbar.make(
            binding.root,
            getString(R.string.notification_permission_denied),
            Snackbar.LENGTH_LONG,
        ).setAction(R.string.notification_permission_open_settings) {
            val uri = Uri.fromParts("package", packageName, null)
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri))
        }.show()
    }

    private fun handleQuestCompleteNotificationIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_PARENT_CUSTOM_QUESTS, false) != true) return
        val childId = intent.getStringExtra(EXTRA_QUEST_COMPLETE_CHILD_ID)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
        parentDashboardRefreshBus.notify(
            ParentDashboardRefreshTrigger.CustomQuestsChanged(
                childId = childId,
                showPendingNotice = false,
            ),
        )
        intent.removeExtra(EXTRA_OPEN_PARENT_CUSTOM_QUESTS)
        intent.removeExtra(EXTRA_QUEST_COMPLETE_CHILD_ID)
    }

    private fun handleFcmNavigationIntent(intent: Intent?, userRole: String?) {
        val target = intent?.getStringExtra(EXTRA_FCM_TARGET)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: return
        when (target) {
            FCM_TARGET_PARENT_DASHBOARD -> {
                if (userRole == "PARENT") {
                    runCatching {
                        if (!navController.popBackStack(R.id.parentDashboardFragment, false)) {
                            navController.navigate(R.id.parentDashboardFragment)
                        }
                    }
                }
            }
            FCM_TARGET_CHILD_HOME -> {
                if (userRole == "CHILD") {
                    runCatching { navController.navigateToChildTopLevel(R.id.homeFragment) }
                    binding.bottomNav.post { syncChildBottomNavTabSelection(binding.bottomNav, R.id.homeFragment) }
                }
            }
            FCM_TARGET_CHILD_GACHA -> {
                if (userRole == "CHILD") {
                    runCatching { navController.navigateToChildTopLevel(R.id.gachaFragment) }
                    binding.bottomNav.post { syncChildBottomNavTabSelection(binding.bottomNav, R.id.gachaFragment) }
                }
            }
        }
        intent.removeExtra(EXTRA_FCM_TARGET)
        intent.removeExtra(EXTRA_FCM_NOTIFICATION_TYPE)
    }

    private suspend fun runBootstrapIfNeeded() {
        appBootstrapRepository.getBootstrap().fold(
            onSuccess = { data ->
                val outdated = AppVersionUtils.isBelowMinimum(
                    BuildConfig.VERSION_NAME,
                    data.minimumAppVersion
                )
                if (data.forceUpdateRequired || outdated) {
                    runOnUiThread { showForceUpdateDialog() }
                }
            },
            onFailure = { }
        )
    }

    private fun showForceUpdateDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.force_update_title)
            .setMessage(R.string.force_update_message)
            .setCancelable(false)
            .setPositiveButton(R.string.force_update_go_store) { _, _ ->
                val uri = Uri.parse("https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}")
                startActivity(Intent(Intent.ACTION_VIEW, uri))
                finish()
            }
            .show()
    }

    private fun setupNavigation(savedInstanceState: Bundle?) {
        if (!::navController.isInitialized) return
        lifecycleScope.launch {
            parentNavDestinationListener?.let { navController.removeOnDestinationChangedListener(it) }
            parentNavDestinationListener = null
            childNavDestinationListener?.let { navController.removeOnDestinationChangedListener(it) }
            childNavDestinationListener = null

            val userRole: String? = sessionManager.userRole.first()
            navigationUserRole = userRole

            val targetGraphRes = when (userRole) {
                "CHILD" ->
                    if (UiMode.useStubNav) R.navigation.nav_child_stub
                    else R.navigation.nav_child
                "PARENT" ->
                    if (UiMode.useStubNav) R.navigation.nav_parent_stub
                    else R.navigation.nav_parent
                else ->
                    if (UiMode.useStubNav) R.navigation.nav_main_stub
                    else R.navigation.nav_main
            }

            val targetGraphId = when (userRole) {
                "CHILD" ->
                    if (UiMode.useStubNav) R.id.nav_child_stub
                    else R.id.nav_child
                "PARENT" ->
                    if (UiMode.useStubNav) R.id.nav_parent_stub
                    else R.id.nav_parent
                else ->
                    if (UiMode.useStubNav) R.id.nav_main_stub
                    else R.id.nav_main
            }

            // [수정] navController.graph에 직접 접근 시 그래프가 없으면 IllegalStateException이 발생하므로 안전하게 처리
            val currentGraphId = try {
                navController.graph.id
            } catch (e: Exception) {
                null
            }

            val forceGraphFromSessionRestart =
                savedInstanceState == null && intent.getBooleanExtra(EXTRA_IS_RESTART, false)
            if (forceGraphFromSessionRestart || currentGraphId != targetGraphId) {
                navController.setGraph(targetGraphRes)
                // 역할/그래프 전환 직후 이전 Nav 상태가 남으면 MY 등 복원 시 ClassNotFound가 날 수 있음
                if (forceGraphFromSessionRestart) {
                    runCatching {
                        navController.popBackStack(navController.graph.startDestinationId, false)
                    }
                }
            }
            if (forceGraphFromSessionRestart) {
                intent.removeExtra(EXTRA_IS_RESTART)
            }

            // 목업 모드(useStubNav)일 때는 서버 통신 UseCase를 호출하지 않음
            if (!UiMode.useStubNav) {
                if (userRole == "PARENT") {
                    registerParentFcmTokenUseCase(requireParentSession = true)
                    syncParentChildrenUseCase()
                }

                if (userRole == "CHILD") {
                    registerChildFcmTokenUseCase(requireChildSession = true)
                    childSessionValidateUseCase().fold(
                        onSuccess = { },
                        onFailure = { e ->
                            val code = (e.cause as? HttpException)?.code()
                            if (code != 401) {
                                Snackbar.make(
                                    binding.root,
                                    e.message ?: "세션을 확인하지 못했습니다.",
                                    Snackbar.LENGTH_LONG
                                ).show()
                            }
                        }
                    )
                }
            }

            if (userRole == "CHILD") {
                lifecycleScope.launch {
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        chatHintNotifier.hints.collect { hint ->
                            Snackbar.make(binding.root, hint, Snackbar.LENGTH_LONG).show()
                        }
                    }
                }

                lifecycleScope.launch {
                    repeatOnLifecycle(Lifecycle.State.STARTED) {
                        childHomeBootstrapGate.suppressChildBottomNav.collect {
                            updateChildBottomNavVisibility()
                        }
                    }
                }
                // 선택 탭만 복원되고 Nav 화면은 홈으로 남으면 MY 재탭 시 popBackStack만 호출되어 진입이 막힐 수 있음
                binding.bottomNav.isSaveEnabled = false
                binding.bottomNav.menu.clear()
                menuInflater.inflate(R.menu.bottom_nav_menu, binding.bottomNav.menu)
                // setupWithNavController와 커스텀 탭 리스너가 겹치면 MY 등 일부 탭 전환이 실패할 수 있음
                binding.bottomNav.setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.child_bottom_nav_bg))
                val childNavTint = ContextCompat.getColorStateList(this@MainActivity, R.color.bottom_nav_child_item)
                binding.bottomNav.itemIconTintList = childNavTint
                binding.bottomNav.itemTextColor = childNavTint
                binding.bottomNav.isItemActiveIndicatorEnabled = false

                val childDestListener = NavController.OnDestinationChangedListener { _, destination, _ ->
                    updateChildBottomNavVisibility()
                    if (suppressChildBottomNavItemSelected) return@OnDestinationChangedListener
                    val menuId = ChildTopLevelNav.mapDestinationToTab(destination.id)
                        ?: return@OnDestinationChangedListener
                    syncChildBottomNavTabSelection(binding.bottomNav, menuId)
                }
                childNavDestinationListener = childDestListener
                navController.addOnDestinationChangedListener(childDestListener)
                updateChildBottomNavVisibility()
                ChildTopLevelNav.mapDestinationToTab(navController.currentDestination?.id)?.let { initial ->
                    syncChildBottomNavTabSelection(binding.bottomNav, initial)
                }

                binding.bottomNav.setOnItemSelectedListener { item ->
                    if (suppressChildBottomNavItemSelected) return@setOnItemSelectedListener true
                    if (childGachaOnboardingController.isNavLockedToGacha &&
                        item.itemId != R.id.gachaFragment
                    ) {
                        Snackbar.make(
                            binding.root,
                            R.string.child_onboarding_gacha_only,
                            Snackbar.LENGTH_SHORT,
                        ).show()
                        return@setOnItemSelectedListener false
                    }
                    val currentId = navController.currentDestination?.id
                    // MY 하위(알림 설정 등)에서 탭만 맞추는 경우 pop/navigate 하지 않음 — 잠깐 열렸다 닫힘 방지
                    if (currentId == item.itemId ||
                        ChildTopLevelNav.mapDestinationToTab(currentId) == item.itemId
                    ) {
                        return@setOnItemSelectedListener true
                    }
                    runCatching { navController.navigateToChildTopLevel(item.itemId) }
                    binding.bottomNav.post {
                        ChildTopLevelNav.mapDestinationToTab(navController.currentDestination?.id)?.let { tabId ->
                            syncChildBottomNavTabSelection(binding.bottomNav, tabId)
                        }
                    }
                    true
                }

                binding.bottomNav.setOnItemReselectedListener { item ->
                    if (suppressChildBottomNavItemSelected) return@setOnItemReselectedListener
                    if (childGachaOnboardingController.isNavLockedToGacha &&
                        item.itemId != R.id.gachaFragment
                    ) {
                        return@setOnItemReselectedListener
                    }
                    navController.onChildBottomNavTap(item.itemId)
                }
            } else if (userRole == "PARENT") {
                binding.bottomNav.visibility = View.GONE
                binding.bottomNav.setOnItemSelectedListener(null)
                binding.bottomNav.setOnItemReselectedListener(null)
            } else {
                binding.bottomNav.visibility = View.GONE
            }

            installMainBackNavigation(userRole)
            handleFcmNavigationIntent(intent, userRole)
        }
    }

    private fun installMainBackNavigation(userRole: String?) {
        val topLevelDestinations = when (userRole) {
            "CHILD" -> setOf(
                R.id.homeFragment,
                R.id.chatFragment,
                R.id.gachaFragment,
                R.id.myProfileFragment,
            )
            "PARENT" -> setOf(R.id.parentDashboardFragment)
            else -> setOf(R.id.roleSelectFragment)
        }
        val appBarConfig = AppBarConfiguration(topLevelDestinations)
        mainBackPressedCallback?.remove()
        mainBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (NavigationUI.navigateUp(navController, appBarConfig)) return
                showExitConfirmDialog()
            }
        }.also { onBackPressedDispatcher.addCallback(this, it) }
    }

    private fun showExitConfirmDialog() {
        exitConfirmDialog?.dismiss()
        exitConfirmDialog = AlertDialog.Builder(this)
            .setTitle(R.string.app_exit_confirm_title)
            .setMessage(R.string.app_exit_confirm_message)
            .setPositiveButton(R.string.app_exit_confirm_positive) { _, _ -> finish() }
            .setNegativeButton(R.string.app_exit_confirm_negative) { d, _ -> d.dismiss() }
            .setOnDismissListener { exitConfirmDialog = null }
            .show()
    }

    override fun onDestroy() {
        if (::navController.isInitialized) {
            parentNavDestinationListener?.let { navController.removeOnDestinationChangedListener(it) }
            childNavDestinationListener?.let { navController.removeOnDestinationChangedListener(it) }
        }
        parentNavDestinationListener = null
        childNavDestinationListener = null
        exitConfirmDialog?.dismiss()
        exitConfirmDialog = null
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        if (navigationUserRole == "CHILD" && ::navController.isInitialized) {
            ChildTopLevelNav.mapDestinationToTab(navController.currentDestination?.id)?.let { tabId ->
                syncChildBottomNavTabSelection(binding.bottomNav, tabId)
            }
        }
    }

    /** 자녀 첫 펫 온보딩 — 수집 탭으로 이동하고 뽑기 코치마크 단계로 전환 */
    fun navigateChildToGachaForOnboarding() {
        if (navigationUserRole != "CHILD" || !::navController.isInitialized) return
        childGachaOnboardingController.onGachaPullCoachmark()
        suppressChildBottomNavItemSelected = true
        runCatching { navController.navigateToChildTopLevel(R.id.gachaFragment) }
        binding.bottomNav.post {
            syncChildBottomNavTabSelection(binding.bottomNav, R.id.gachaFragment)
            suppressChildBottomNavItemSelected = false
        }
    }

    fun navigateChildToHomeAfterOnboarding() {
        if (navigationUserRole != "CHILD" || !::navController.isInitialized) return
        suppressChildBottomNavItemSelected = true
        runCatching { navController.navigateToChildTopLevel(R.id.homeFragment) }
        binding.bottomNav.post {
            syncChildBottomNavTabSelection(binding.bottomNav, R.id.homeFragment)
            suppressChildBottomNavItemSelected = false
        }
    }

    private fun updateChildBottomNavVisibility() {
        if (navigationUserRole != "CHILD" || !::navController.isInitialized) return
        val destId = navController.currentDestination?.id
        val hideForScreen = ChildTopLevelNav.shouldHideBottomNav(destId)
        val hideForHomeBootstrap = childHomeBootstrapGate.suppressChildBottomNav.value
        binding.bottomNav.visibility =
            if (hideForScreen || hideForHomeBootstrap) View.GONE else View.VISIBLE
    }

    /** MY 등 화면과 하단 탭 선택 상태가 어긋날 때 Fragment에서 호출 */
    fun syncChildBottomNavForCurrentDestination() {
        if (navigationUserRole != "CHILD" || !::navController.isInitialized) return
        ChildTopLevelNav.mapDestinationToTab(navController.currentDestination?.id)?.let { tabId ->
            syncChildBottomNavTabSelection(binding.bottomNav, tabId)
        }
    }

    /** 자녀 하단 탭: 선택 탭만 초록·확대, 나머지는 회색·기본 크기로 맞춘다. */
    private fun syncChildBottomNavTabSelection(bottomNav: BottomNavigationView, selectedMenuItemId: Int) {
        bottomNav.post {
            if (!bottomNav.isAttachedToWindow) return@post
            suppressChildBottomNavItemSelected = true
            try {
                bottomNav.selectedItemId = selectedMenuItemId
                for (i in 0 until bottomNav.menu.size()) {
                    val mi = bottomNav.menu.getItem(i)
                    val selected = mi.itemId == selectedMenuItemId
                    mi.isChecked = selected
                    val tab = bottomNav.findViewById<View>(mi.itemId) ?: continue
                    if (tab is Checkable) {
                        tab.isChecked = selected
                    }
                    tab.isSelected = selected
                    tab.animate().cancel()
                    val target = if (selected) 1.12f else 1f
                    if ((tab.scaleX - target) * (tab.scaleX - target) > 0.0004f) {
                        tab.animate().scaleX(target).scaleY(target).setDuration(120).start()
                    } else {
                        tab.scaleX = target
                        tab.scaleY = target
                    }
                    applyChildBottomNavItemContentColors(tab, selected)
                }
                bottomNav.invalidate()
            } finally {
                // selectedItemId 변경 리스너는 finally 직후에 올 수 있어 한 프레임 뒤에 해제
                bottomNav.post { suppressChildBottomNavItemSelected = false }
            }
        }
    }

    private fun applyChildBottomNavItemContentColors(itemRoot: View, selected: Boolean) {
        val green = ContextCompat.getColor(itemRoot.context, R.color.child_nav_item_selected)
        val muted = ContextCompat.getColor(itemRoot.context, R.color.child_nav_item_unselected)
        val color = if (selected) green else muted
        val iconTint = ColorStateList.valueOf(color)
        fun walk(v: View) {
            when (v) {
                is ImageView -> v.imageTintList = iconTint
                is TextView -> v.setTextColor(color)
                is ViewGroup -> {
                    for (i in 0 until v.childCount) walk(v.getChildAt(i))
                }
            }
        }
        walk(itemRoot)
    }

    /** 부모 하단 탭: 아이콘·라벨에 선택 여부에 따른 색을 직접 적용한다. */
    private fun applyParentBottomNavItemContentColors(itemRoot: View, selected: Boolean) {
        val blue = ContextCompat.getColor(itemRoot.context, R.color.parent_mock_blue)
        val muted = ContextCompat.getColor(itemRoot.context, R.color.parent_mock_text_muted)
        val color = if (selected) blue else muted
        val iconTint = ColorStateList.valueOf(color)
        fun walk(v: View) {
            when (v) {
                is ImageView -> v.imageTintList = iconTint
                is TextView -> v.setTextColor(color)
                is ViewGroup -> {
                    for (i in 0 until v.childCount) walk(v.getChildAt(i))
                }
            }
        }
        walk(itemRoot)
    }

    /** 부모 하단 탭: `selectedItemId`·체크·스케일·색을 목적지와 맞춘다. */
    private fun syncParentBottomNavTabSelection(bottomNav: BottomNavigationView, selectedMenuItemId: Int) {
        bottomNav.post {
            if (!bottomNav.isAttachedToWindow) return@post
            suppressParentBottomNavItemSelected = true
            try {
                bottomNav.selectedItemId = selectedMenuItemId
                for (i in 0 until bottomNav.menu.size()) {
                    val mi = bottomNav.menu.getItem(i)
                    val selected = mi.itemId == selectedMenuItemId
                    mi.isChecked = selected
                    val tab = bottomNav.findViewById<View>(mi.itemId) ?: continue
                    if (tab is Checkable) {
                        tab.isChecked = selected
                    }
                    tab.isSelected = selected
                    tab.animate().cancel()
                    val target = if (selected) 1.15f else 1f
                    val delta = tab.scaleX - target
                    if (delta * delta > 0.0004f) {
                        tab.animate().scaleX(target).scaleY(target).setDuration(120).start()
                    } else {
                        tab.scaleX = target
                        tab.scaleY = target
                    }
                    applyParentBottomNavItemContentColors(tab, selected)
                }
                bottomNav.invalidate()
            } finally {
                suppressParentBottomNavItemSelected = false
            }
        }
    }
}
