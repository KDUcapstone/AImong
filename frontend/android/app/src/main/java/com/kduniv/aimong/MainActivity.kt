package com.kduniv.aimong

import android.content.Intent
import android.net.Uri
import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Checkable
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
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
import com.kduniv.aimong.feature.parent.domain.SyncParentChildrenUseCase
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.snackbar.Snackbar
import com.kduniv.aimong.databinding.ActivityMainBinding
import com.kduniv.aimong.feature.chat.ChatHintNotifier
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
    lateinit var chatHintNotifier: ChatHintNotifier

    @Inject
    lateinit var appBootstrapRepository: AppBootstrapRepository

    @Inject
    lateinit var childSessionValidateUseCase: ChildSessionValidateUseCase

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private var mainBackPressedCallback: OnBackPressedCallback? = null
    private var exitConfirmDialog: AlertDialog? = null
    private var parentNavDestinationListener: NavController.OnDestinationChangedListener? = null
    private var childNavDestinationListener: NavController.OnDestinationChangedListener? = null
    /** [syncParentBottomNavTabSelection]에서 `selectedItemId` 변경 시 `OnItemSelected` 재진입 방지 */
    private var suppressParentBottomNavItemSelected = false
    private var suppressChildBottomNavItemSelected = false

    /** [setupNavigation]에서 설정 — 부모 하단 네비 재동기화용 */
    private var navigationUserRole: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val perm = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(perm), 1001)
            }
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        if (navHostFragment != null) {
            navController = navHostFragment.navController
            setupNavigation(savedInstanceState)
        }

        if (!UiMode.useStubNav) {
            lifecycleScope.launch { runBootstrapIfNeeded() }
        }
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

                binding.bottomNav.visibility = View.VISIBLE
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
                    binding.bottomNav.visibility =
                        if (ChildTopLevelNav.shouldHideBottomNav(destination.id)) View.GONE else View.VISIBLE
                    if (suppressChildBottomNavItemSelected) return@OnDestinationChangedListener
                    val menuId = ChildTopLevelNav.mapDestinationToTab(destination.id)
                        ?: return@OnDestinationChangedListener
                    if (binding.bottomNav.selectedItemId != menuId) {
                        suppressChildBottomNavItemSelected = true
                        try {
                            binding.bottomNav.selectedItemId = menuId
                        } finally {
                            suppressChildBottomNavItemSelected = false
                        }
                    }
                }
                childNavDestinationListener = childDestListener
                navController.addOnDestinationChangedListener(childDestListener)
                navController.currentDestination?.id?.let { currentId ->
                    binding.bottomNav.visibility =
                        if (ChildTopLevelNav.shouldHideBottomNav(currentId)) View.GONE else View.VISIBLE
                }
                ChildTopLevelNav.mapDestinationToTab(navController.currentDestination?.id)?.let { initial ->
                    suppressChildBottomNavItemSelected = true
                    try {
                        binding.bottomNav.selectedItemId = initial
                    } finally {
                        suppressChildBottomNavItemSelected = false
                    }
                }

                binding.bottomNav.setOnItemSelectedListener { item ->
                    if (suppressChildBottomNavItemSelected) return@setOnItemSelectedListener true
                    // 네비 전환은 비동기 — post에서 currentDestination으로 맞추면 이전 탭(예: 수집)으로 되돌아감
                    runCatching { navController.onChildBottomNavTap(item.itemId) }
                    true
                }

                binding.bottomNav.setOnItemReselectedListener { item ->
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
