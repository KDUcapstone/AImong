package com.kduniv.aimong.feature.quiz.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView
import com.kduniv.aimong.R

class QuizReportBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val REQUEST_KEY_SUBMIT = "quiz_report_submit"
        const val REQUEST_KEY_DISMISS = "quiz_report_dismiss"
        const val RESULT_REASON_CODE = "reasonCode"
        const val RESULT_DETAIL = "detail"

        fun newInstance(): QuizReportBottomSheet = QuizReportBottomSheet()
    }

    private data class Reason(val code: String, val labelRes: Int)

    private val reasons = listOf(
        Reason("SAFETY", R.string.quiz_report_reason_safety),
        Reason("INAPPROPRIATE", R.string.quiz_report_reason_inappropriate),
        Reason("DUPLICATE", R.string.quiz_report_reason_duplicate),
        Reason("WRONG_ANSWER", R.string.quiz_report_reason_wrong_answer),
        Reason("LOW_QUALITY", R.string.quiz_report_reason_low_quality),
        Reason("ETC", R.string.quiz_report_reason_etc),
    )

    private var selectedReason: Reason? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_quiz_report, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val stepReasons = view.findViewById<LinearLayout>(R.id.layout_report_step_reasons)
        val stepDetail = view.findViewById<View>(R.id.layout_report_step_detail)
        val tvSelected = view.findViewById<TextView>(R.id.tv_report_selected_reason)
        val etDetail = view.findViewById<EditText>(R.id.et_report_detail)
        val btnCancel = view.findViewById<View>(R.id.btn_report_cancel)
        val btnCancelDetail = view.findViewById<View>(R.id.btn_report_cancel_detail)
        val btnBack = view.findViewById<View>(R.id.btn_report_back)
        val btnSubmit = view.findViewById<View>(R.id.btn_report_submit)
        val btnSubmitWithout = view.findViewById<View>(R.id.btn_report_submit_without_detail)

        fun showReasons() {
            stepReasons.visibility = View.VISIBLE
            stepDetail.visibility = View.GONE
        }

        fun showDetail(reason: Reason) {
            selectedReason = reason
            tvSelected.text = getString(R.string.quiz_report_detail_title) + "\n" + getString(reason.labelRes)
            etDetail.setText("")
            stepReasons.visibility = View.GONE
            stepDetail.visibility = View.VISIBLE
        }

        stepReasons.removeAllViews()
        reasons.forEach { reason ->
            stepReasons.addView(createReasonRow(reason) { showDetail(reason) })
        }

        btnCancel.setOnClickListener { dismiss() }
        btnCancelDetail.setOnClickListener { dismiss() }
        btnBack.setOnClickListener { showReasons() }

        fun submit(detail: String?) {
            val reason = selectedReason ?: return
            setFragmentResult(
                REQUEST_KEY_SUBMIT,
                bundleOf(
                    RESULT_REASON_CODE to reason.code,
                    RESULT_DETAIL to detail
                )
            )
            dismiss()
        }

        btnSubmit.setOnClickListener {
            submit(etDetail.text?.toString())
        }

        btnSubmitWithout.setOnClickListener {
            submit(null)
        }

        showReasons()
    }

    override fun onDismiss(dialog: android.content.DialogInterface) {
        setFragmentResult(REQUEST_KEY_DISMISS, bundleOf())
        super.onDismiss(dialog)
    }

    private fun createReasonRow(reason: Reason, onClick: () -> Unit): View {
        val density = resources.displayMetrics.density
        return MaterialCardView(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                val m = (10 * density).toInt()
                setMargins(0, 0, 0, m)
            }
            radius = 20 * density
            cardElevation = 0f
            setCardBackgroundColor(ContextCompat.getColor(context, R.color.quiz_card_bg))
            strokeColor = ContextCompat.getColor(context, R.color.quiz_option_default_stroke)
            strokeWidth = (2 * density).toInt()
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }

            addView(TextView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setPadding(
                    (16 * density).toInt(),
                    (14 * density).toInt(),
                    (16 * density).toInt(),
                    (14 * density).toInt()
                )
                text = getString(reason.labelRes)
                setTextColor(ContextCompat.getColor(context, R.color.quiz_text_primary))
                textSize = 13f
            })
        }
    }
}

