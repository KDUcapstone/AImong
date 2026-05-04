package com.kduniv.aimong.feature.home.presentation

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.kduniv.aimong.R
import com.kduniv.aimong.feature.home.data.StreakCalendarMapper
import com.kduniv.aimong.feature.home.domain.model.StreakCalendarResult
import com.kduniv.aimong.feature.home.domain.repository.HomeRepository
import dagger.hilt.android.AndroidEntryPoint
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

    private var viewingYearMonth: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_streak_calendar, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
            val result = homeRepository.getStreakCalendar(viewingYearMonth).fold(
                onSuccess = { it },
                onFailure = { e ->
                    Snackbar.make(root, e.message ?: getString(R.string.home_streak_load_failed), Snackbar.LENGTH_LONG).show()
                    null
                }
            )
            pb.visibility = View.GONE
            if (result != null) {
                viewingYearMonth = result.yearMonth
                bind(result, fallbackStreak)
            } else {
                bindEmpty(fallbackStreak)
            }
        }
    }

    private fun bindEmpty(fallbackStreak: Int) {
        val root = requireView()
        root.findViewById<TextView>(R.id.tv_month_label).text =
            formatYearMonthLabel(viewingYearMonth ?: StreakCalendarMapper.defaultYearMonthKst())
        root.findViewById<TextView>(R.id.tv_streak_summary).text =
            getString(R.string.home_streak_summary_fmt, fallbackStreak, 0)
        root.findViewById<TextView>(R.id.tv_today_hint).visibility = View.GONE
        buildWeekdayRow(root)
        buildCalendarCells(root, YearMonth.parse(viewingYearMonth ?: StreakCalendarMapper.defaultYearMonthKst()), emptySet(), null)
        updateNavButtons(root)
    }

    private fun bind(result: StreakCalendarResult, fallbackStreak: Int) {
        val root = requireView()
        val ym = YearMonth.parse(result.yearMonth)
        val completed = result.completedDates.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()
        val today = result.today?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

        root.findViewById<TextView>(R.id.tv_month_label).text = formatYearMonthLabel(result.yearMonth)
        val streakVal = if (result.continuousDays > 0) result.continuousDays else fallbackStreak
        root.findViewById<TextView>(R.id.tv_streak_summary).text = getString(
            R.string.home_streak_month_summary_fmt,
            streakVal,
            completed.size,
            result.yearMonth
        )
        val hint = root.findViewById<TextView>(R.id.tv_today_hint)
        if (today != null) {
            hint.visibility = View.VISIBLE
            hint.text = getString(R.string.home_streak_today_kst_fmt, today)
        } else {
            hint.visibility = View.GONE
        }

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
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_sub_grey))
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
                    setTextColor(ContextCompat.getColor(requireContext(), R.color.text_white))
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
                            today != null && date == today -> {
                                cell.setBackgroundResource(R.drawable.bg_streak_day_today)
                                cell.setTypeface(null, Typeface.BOLD)
                            }
                            completed.contains(date) -> {
                                cell.setBackgroundResource(R.drawable.bg_streak_day_completed)
                                cell.setTypeface(null, Typeface.NORMAL)
                            }
                            else -> {
                                cell.background = null
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
        private const val ARG_FALLBACK_STREAK = "fallbackStreak"

        fun newInstance(fallbackStreakDaysFromHome: Int): StreakCalendarBottomSheet {
            return StreakCalendarBottomSheet().apply {
                arguments = Bundle().apply {
                    putInt(ARG_FALLBACK_STREAK, fallbackStreakDaysFromHome)
                }
            }
        }
    }
}
