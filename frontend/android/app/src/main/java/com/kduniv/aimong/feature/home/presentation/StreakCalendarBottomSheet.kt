package com.kduniv.aimong.feature.home.presentation

import android.app.Dialog
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import com.google.android.material.R as MaterialR
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.appcompat.widget.AppCompatButton
import com.google.android.material.snackbar.Snackbar
import com.kduniv.aimong.R
import com.kduniv.aimong.feature.home.data.StreakCalendarMapper
import com.kduniv.aimong.core.dev.UiMode
import com.kduniv.aimong.feature.home.domain.ChildHomeRefreshBus
import com.kduniv.aimong.feature.home.domain.HomeRefreshTrigger
import com.kduniv.aimong.feature.home.domain.model.StreakCalendarResult
import com.kduniv.aimong.feature.home.domain.repository.HomeRepository
import com.kduniv.aimong.feature.streak.data.StreakRepository
import com.kduniv.aimong.feature.streak.data.model.StreakStatusData
import com.kduniv.aimong.feature.wallet.domain.repository.WalletRepository
import com.kduniv.aimong.feature.gacha.PetArtAssets
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class StreakCalendarBottomSheet : BottomSheetDialogFragment() {

    @Inject
    lateinit var homeRepository: HomeRepository

    @Inject
    lateinit var streakRepository: StreakRepository

    @Inject
    lateinit var walletRepository: WalletRepository

    @Inject
    lateinit var homeRefreshBus: ChildHomeRefreshBus

    private var viewingYearMonth: String? = null
    private var streakStatus: StreakStatusData? = null
    private var shieldCount: Int = 0
    private var streakShieldCost: Int = WalletBalanceDefaults.STREAK_SHIELD_COST
    private var gearBalance: Int = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(MaterialR.id.design_bottom_sheet)
            bottomSheet?.apply {
                background = ContextCompat.getDrawable(context, R.drawable.bg_energy_bottom_sheet)
                layoutParams = layoutParams?.apply {
                    height = ViewGroup.LayoutParams.MATCH_PARENT
                }
                BottomSheetBehavior.from(this).apply {
                    isFitToContents = true
                }
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_streak_calendar, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        applySheetInsets(view)
        val fallbackStreak = requireArguments().getInt(ARG_FALLBACK_STREAK, 0)

        view.findViewById<TextView>(R.id.btn_prev_month).setOnClickListener {
            shiftMonth(-1)
            loadAndRender(fallbackStreak)
        }
        view.findViewById<TextView>(R.id.btn_next_month).setOnClickListener {
            shiftMonth(1)
            loadAndRender(fallbackStreak)
        }

        loadAndRender(fallbackStreak)

        requireView().findViewById<AppCompatButton>(R.id.btn_buy_shield).setOnClickListener { btn ->
            viewLifecycleOwner.lifecycleScope.launch {
                btn.isEnabled = false
                streakRepository.purchaseShield(1).fold(
                    onSuccess = { data ->
                        shieldCount = data.shieldCount
                        data.resolvedGearBalance()?.let { gearBalance = it }
                        refreshStreakSnapshot()
                        btn.isEnabled = gearBalance >= streakShieldCost
                        Snackbar.make(requireView(), getString(R.string.streak_shield_purchase_success), Snackbar.LENGTH_SHORT).show()
                        if (!UiMode.useStubNav) {
                            homeRefreshBus.notify(HomeRefreshTrigger.Full)
                        } else {
                            parentFragmentManager.setFragmentResult(
                                REQUEST_KEY,
                                bundleOf(EXTRA_REFRESH_HOME to true)
                            )
                        }
                    },
                    onFailure = { e ->
                        Snackbar.make(
                            requireView(),
                            e.message ?: getString(R.string.gear_shield_purchase_failed),
                            Snackbar.LENGTH_LONG
                        ).show()
                        btn.isEnabled = gearBalance >= streakShieldCost
                    }
                )
            }
        }
    }

    private fun applySheetInsets(root: View) {
        val scroll = root.findViewById<NestedScrollView>(R.id.scroll_streak_sheet)
        val content = scroll.getChildAt(0) ?: return
        val baseBottom = content.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { _, insets ->
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            content.updatePadding(bottom = baseBottom + nav.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(scroll)
    }

    private suspend fun refreshStreakSnapshot() {
        streakRepository.getStreak().getOrNull()?.let { status ->
            streakStatus = status
            shieldCount = status.shieldCount
        }
        walletRepository.getWallet().getOrNull()?.let {
            gearBalance = it.gear
            streakShieldCost = it.streakShieldCost
        }
        bindStatusSummary()
        bindShieldSection()
    }

    private fun bindStatusSummary() {
        val tv = view?.findViewById<TextView>(R.id.tv_streak_status_summary) ?: return
        val status = streakStatus
        if (status == null) {
            tv.visibility = View.GONE
            return
        }
        tv.visibility = View.VISIBLE
        tv.text = getString(
            R.string.home_streak_status_summary_fmt,
            status.continuousDays,
            status.todaySetCount,
            status.shieldCount
        )
    }

    private fun bindShieldSection() {
        val root = view ?: return
        root.findViewById<TextView>(R.id.tv_shield_count).text =
            getString(R.string.streak_shield_count_fmt, shieldCount)
        val btn = root.findViewById<AppCompatButton>(R.id.btn_buy_shield)
        btn.text = getString(R.string.streak_buy_shield_btn, streakShieldCost)
        val canBuy = gearBalance >= streakShieldCost
        btn.isEnabled = canBuy
        btn.alpha = if (canBuy) 1f else 0.45f
    }

    private fun shiftMonth(delta: Int) {
        val kst = ZoneId.of("Asia/Seoul")
        val cur = viewingYearMonth ?: StreakCalendarMapper.defaultYearMonthKst()
        val ym = YearMonth.parse(cur).plusMonths(delta.toLong())
        val nowYm = YearMonth.now(kst)
        if (ym.isAfter(nowYm)) return
        viewingYearMonth = ym.format(DateTimeFormatter.ofPattern("yyyy-MM", Locale.US))
    }

    private fun loadAndRender(fallbackStreak: Int) {
        val root = requireView()
        val pb = root.findViewById<ProgressBar>(R.id.pb_streak_loading)
        pb.visibility = View.VISIBLE
        viewLifecycleOwner.lifecycleScope.launch {
            val streakDeferred = async { streakRepository.getStreak().getOrNull() }
            val calendarDeferred = async {
                homeRepository.getStreakCalendar(viewingYearMonth).fold(
                    onSuccess = { it },
                    onFailure = { e ->
                        Snackbar.make(
                            root,
                            e.message ?: getString(R.string.home_streak_load_failed),
                            Snackbar.LENGTH_LONG
                        ).show()
                        null
                    }
                )
            }
            streakDeferred.await()?.let { status ->
                streakStatus = status
                shieldCount = status.shieldCount
            }
            walletRepository.getWallet().getOrNull()?.let {
                gearBalance = it.gear
                streakShieldCost = it.streakShieldCost
            }
            bindStatusSummary()
            bindShieldSection()

            val result = calendarDeferred.await()
            pb.visibility = View.GONE
            if (result != null) {
                viewingYearMonth = result.yearMonth
                bind(result, fallbackStreak)
            } else {
                bindEmpty(fallbackStreak, calendarLoadFailed = true)
            }
        }
    }

    private fun resolveContinuousDays(calendarContinuous: Int, fallbackStreak: Int): Int {
        val fromStatus = streakStatus?.continuousDays ?: 0
        return when {
            fromStatus > 0 -> fromStatus
            calendarContinuous > 0 -> calendarContinuous
            else -> fallbackStreak
        }
    }

    /** v2.0: GET /streak 의 todaySetCount가 정본 (KST 경계·복습 제외는 BE 처리) */
    private fun isTodaySetCompleted(
        calendarToday: LocalDate?,
        calendarCompleted: Set<LocalDate>
    ): Boolean {
        streakStatus?.let { if (it.todaySetCount > 0) return true }
        val today = calendarToday ?: return false
        return calendarCompleted.contains(today)
    }

    private fun bindHeaderAndMessage(
        root: View,
        streakVal: Int,
        isTodayCompleted: Boolean
    ) {
        root.findViewById<TextView>(R.id.tv_streak_big_number).text = streakVal.toString()

        val tvMessage = root.findViewById<TextView>(R.id.tv_streak_message)
        val ivIcon = root.findViewById<ImageView>(R.id.iv_message_icon)
        val petType = requireArguments().getString(ARG_PET_TYPE).orEmpty()
        val petStage = requireArguments().getString(ARG_PET_STAGE).orEmpty().ifBlank { "EGG" }
        val petGrade = requireArguments().getString(ARG_PET_GRADE).orEmpty().ifBlank { "NORMAL" }
        PetArtAssets.bindEquipped(
            image = root.findViewById(R.id.iv_streak_pet_sprite),
            emojiFallback = root.findViewById(R.id.tv_streak_pet_emoji),
            petType = petType,
            stage = petStage,
            grade = petGrade,
            lottie = root.findViewById(R.id.lav_streak_pet),
        )

        tvMessage.maxLines = 2
        val accent = ContextCompat.getColor(requireContext(), R.color.child_streak_accent)
        val warn = ContextCompat.getColor(requireContext(), R.color.quiz_red)
        if (isTodayCompleted) {
            tvMessage.text = getString(R.string.home_streak_praise_short, streakVal)
            ivIcon.setImageResource(R.drawable.ic_flame)
            ivIcon.setColorFilter(accent)
        } else if (streakVal > 0) {
            tvMessage.text = getString(R.string.home_streak_encourage_continue)
            ivIcon.setImageResource(R.drawable.ic_star_filled)
            ivIcon.setColorFilter(warn)
        } else {
            tvMessage.text = getString(R.string.home_streak_encourage_start)
            ivIcon.setImageResource(R.drawable.ic_star_filled)
            ivIcon.setColorFilter(warn)
        }
    }

    private fun bindEmpty(fallbackStreak: Int, calendarLoadFailed: Boolean = false) {
        val root = requireView()
        val streakVal = resolveContinuousDays(0, fallbackStreak)
        val kst = ZoneId.of("Asia/Seoul")
        val today = LocalDate.now(kst)
        bindHeaderAndMessage(root, streakVal, isTodaySetCompleted(today, emptySet()))

        if (calendarLoadFailed) {
            val calendarNote = getString(R.string.home_streak_calendar_no_completion_data)
            root.findViewById<TextView>(R.id.tv_streak_message).apply {
                maxLines = 5
                text = "${text}\n$calendarNote"
            }
        }

        val ymStr = viewingYearMonth ?: StreakCalendarMapper.defaultYearMonthKst()
        root.findViewById<TextView>(R.id.tv_month_label).text = formatYearMonthLabel(ymStr)
        buildWeekdayRow(root)
        buildCalendarCells(root, YearMonth.parse(ymStr), emptySet(), today)
        updateNavButtons(root)
    }

    private fun bind(result: StreakCalendarResult, fallbackStreak: Int) {
        val root = requireView()
        val ym = YearMonth.parse(result.yearMonth)
        val completed = result.completedDates.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()
        val today = result.today?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

        val streakVal = resolveContinuousDays(result.continuousDays, fallbackStreak)
        bindHeaderAndMessage(root, streakVal, isTodaySetCompleted(today, completed))

        root.findViewById<TextView>(R.id.tv_month_label).text = formatYearMonthLabel(result.yearMonth)
        buildWeekdayRow(root)
        buildCalendarCells(root, ym, completed, today)
        updateNavButtons(root)
    }

    private fun updateNavButtons(root: View) {
        val kst = ZoneId.of("Asia/Seoul")
        val nowYm = YearMonth.now(kst)
        val cur = YearMonth.parse(viewingYearMonth ?: StreakCalendarMapper.defaultYearMonthKst())
        root.findViewById<TextView>(R.id.btn_next_month).alpha = if (cur >= nowYm) 0.35f else 1f
        root.findViewById<TextView>(R.id.btn_next_month).isEnabled = cur < nowYm
    }

    private fun formatYearMonthLabel(ym: String): String {
        val parts = ym.split("-")
        if (parts.size != 2) return ym
        val y = parts[0].toIntOrNull() ?: return ym
        val m = parts[1].toIntOrNull() ?: return ym
        return getString(R.string.home_streak_month_label_fmt, y, m)
    }

    private fun buildWeekdayRow(root: View) {
        val row = root.findViewById<LinearLayout>(R.id.layout_weekday_row)
        row.removeAllViews()
        val labels = listOf("일", "월", "화", "수", "목", "금", "토")
        labels.forEach { label ->
            val tv = TextView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(36), 1f)
                gravity = Gravity.CENTER
                text = label
                textSize = 12f
                setTextColor(ContextCompat.getColor(requireContext(), R.color.child_quest_sheet_text_secondary))
            }
            row.addView(tv)
        }
    }

    private fun buildCalendarCells(root: View, ym: YearMonth, completed: Set<LocalDate>, today: LocalDate?) {
        val grid = root.findViewById<LinearLayout>(R.id.layout_calendar_grid)
        grid.removeAllViews()

        val first = ym.atDay(1)
        val offset = first.dayOfWeek.value % 7
        val daysInMonth = ym.lengthOfMonth()
        val totalCells = ((offset + daysInMonth) + 6) / 7 * 7

        val textPrimary = ContextCompat.getColor(requireContext(), R.color.child_quest_sheet_text_primary)
        val textOnAccent = ContextCompat.getColor(requireContext(), R.color.white)

        var day = 1
        for (rowStart in 0 until totalCells step 7) {
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            for (col in 0 until 7) {
                val idx = rowStart + col
                val cell = TextView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(0, dp(40), 1f)
                    gravity = Gravity.CENTER
                    textSize = 13f
                    setTextColor(textPrimary)
                }
                when {
                    idx < offset || day > daysInMonth -> {
                        cell.text = ""
                        cell.background = null
                        cell.setTypeface(null, Typeface.NORMAL)
                    }
                    else -> {
                        val date = ym.atDay(day)
                        cell.text = day.toString()
                        when {
                            completed.contains(date) -> {
                                cell.setBackgroundResource(R.drawable.bg_streak_day_completed_duo)
                                cell.setTextColor(textOnAccent)
                                cell.setTypeface(null, Typeface.BOLD)
                            }
                            today != null && date == today -> {
                                cell.setBackgroundResource(R.drawable.bg_streak_day_today)
                                cell.setTextColor(
                                    ContextCompat.getColor(requireContext(), R.color.child_streak_accent_dark)
                                )
                                cell.setTypeface(null, Typeface.BOLD)
                            }
                            else -> {
                                cell.background = null
                                cell.setTextColor(textPrimary)
                                cell.setTypeface(null, Typeface.NORMAL)
                            }
                        }
                        day++
                    }
                }
                row.addView(cell)
            }
            grid.addView(row)
        }
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    companion object {
        const val REQUEST_KEY = "aimong_streak_calendar"
        const val EXTRA_REFRESH_HOME = "refresh_home"

        private const val ARG_FALLBACK_STREAK = "fallbackStreak"
        private const val ARG_PET_TYPE = "petType"
        private const val ARG_PET_STAGE = "petStage"
        private const val ARG_PET_GRADE = "petGrade"

        fun newInstance(
            fallbackStreakDaysFromHome: Int,
            petType: String = "",
            petStage: String = "EGG",
            petGrade: String = "NORMAL",
        ): StreakCalendarBottomSheet {
            return StreakCalendarBottomSheet().apply {
                arguments = bundleOf(
                    ARG_FALLBACK_STREAK to fallbackStreakDaysFromHome,
                    ARG_PET_TYPE to petType,
                    ARG_PET_STAGE to petStage,
                    ARG_PET_GRADE to petGrade,
                )
            }
        }
    }
}
