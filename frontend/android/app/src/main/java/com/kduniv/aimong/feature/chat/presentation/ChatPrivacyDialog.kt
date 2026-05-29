package com.kduniv.aimong.feature.chat.presentation

import android.app.Dialog
import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.kduniv.aimong.R
import com.kduniv.aimong.core.privacy.PrivacyRadar
import com.kduniv.aimong.core.privacy.PrivacyType
import com.kduniv.aimong.core.ui.CelebrationDialogWindow

object ChatPrivacyDialog {

    fun show(
        host: Fragment,
        prompt: ChatPrivacyPrompt,
        privacyRadar: PrivacyRadar,
        onMaskAndSend: () -> Unit,
        onCancel: () -> Unit,
    ): Dialog? {
        if (!host.isAdded) return null
        val ctx = host.requireContext()
        val view = LayoutInflater.from(ctx).inflate(R.layout.dialog_chat_privacy, null, false)
        val dialog = AlertDialog.Builder(ctx, R.style.TransparentDialog)
            .setView(view)
            .setCancelable(true)
            .create()

        view.findViewById<TextView>(R.id.tv_chat_privacy_preview).text =
            buildHighlightedPreview(ctx, prompt.originalText, prompt.highlightRanges)
        view.findViewById<TextView>(R.id.tv_chat_privacy_detected_types).text =
            formatDetectedTypes(ctx, privacyRadar, prompt.originalText, prompt.highlightRanges)

        view.findViewById<MaterialButton>(R.id.btn_chat_privacy_mask).setOnClickListener {
            dialog.dismiss()
            onMaskAndSend()
        }
        view.findViewById<MaterialButton>(R.id.btn_chat_privacy_cancel).setOnClickListener {
            dialog.dismiss()
            onCancel()
        }
        dialog.setOnCancelListener { onCancel() }

        dialog.show()
        CelebrationDialogWindow.apply(dialog, ctx)
        return dialog
    }

    private fun buildHighlightedPreview(
        context: Context,
        fullText: String,
        ranges: List<IntRange>,
    ): CharSequence {
        if (fullText.isEmpty()) return ""
        val builder = SpannableStringBuilder(fullText)
        val bg = ContextCompat.getColor(context, R.color.chat_privacy_highlight)
        val fg = ContextCompat.getColor(context, R.color.chat_privacy_sensitive_text)
        val n = fullText.length
        for (range in ranges) {
            val start = range.first.coerceIn(0, n)
            val end = (range.last + 1).coerceIn(start, n)
            if (start >= end) continue
            builder.setSpan(BackgroundColorSpan(bg), start, end, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.setSpan(ForegroundColorSpan(fg), start, end, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.setSpan(StyleSpan(Typeface.BOLD), start, end, SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return builder
    }

    private fun formatDetectedTypes(
        context: Context,
        privacyRadar: PrivacyRadar,
        text: String,
        ranges: List<IntRange>,
    ): String {
        val types = privacyRadar.privacyTypesInRanges(text, ranges)
        if (types.isEmpty()) {
            return context.getString(
                R.string.chat_privacy_detected_types_fmt,
                context.getString(R.string.chat_privacy_type_etc),
            )
        }
        val labels = types.map { typeLabel(context, it) }
        return context.getString(
            R.string.chat_privacy_detected_types_fmt,
            labels.joinToString(separator = " · "),
        )
    }

    private fun typeLabel(context: Context, type: PrivacyType): String = when (type) {
        PrivacyType.PHONE -> context.getString(R.string.chat_privacy_type_phone)
        PrivacyType.EMAIL -> context.getString(R.string.chat_privacy_type_email)
        PrivacyType.SCHOOL -> context.getString(R.string.chat_privacy_type_school)
        PrivacyType.AGE -> context.getString(R.string.chat_privacy_type_age)
        PrivacyType.GRADE -> context.getString(R.string.chat_privacy_type_grade)
        PrivacyType.ADDRESS -> context.getString(R.string.chat_privacy_type_address)
        PrivacyType.DATE -> context.getString(R.string.chat_privacy_type_date)
        PrivacyType.URL -> context.getString(R.string.chat_privacy_type_url)
        PrivacyType.NAME -> context.getString(R.string.chat_privacy_type_name)
        PrivacyType.ETC -> context.getString(R.string.chat_privacy_type_etc)
    }
}
