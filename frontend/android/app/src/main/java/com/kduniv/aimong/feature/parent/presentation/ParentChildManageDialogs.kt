package com.kduniv.aimong.feature.parent.presentation

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.kduniv.aimong.R
import com.kduniv.aimong.core.network.model.ParentChildItem

object ParentChildManageDialogs {

    fun showManageMenu(
        fragment: Fragment,
        child: ParentChildItem,
        onEditNickname: (ParentChildItem) -> Unit,
        onRegenerateCode: (String) -> Unit,
        onDelete: (ParentChildItem) -> Unit,
    ) {
        val ctx = fragment.requireContext()
        val dialogView = LayoutInflater.from(ctx)
            .inflate(R.layout.dialog_parent_child_manage, null, false)
        dialogView.findViewById<TextView>(R.id.tv_manage_child_name).text = child.nickname

        val dialog = openCardDialog(fragment, dialogView)

        fun dismissThen(action: () -> Unit) {
            dialog.dismiss()
            action()
        }

        bindActionRow(
            row = dialogView.findViewById(R.id.row_edit_nickname),
            iconBgRes = R.drawable.bg_stat_icon_blue,
            iconRes = R.drawable.ic_parent_settings,
            iconTint = ContextCompat.getColor(ctx, R.color.parent_mock_blue),
            label = ctx.getString(R.string.parent_child_manage_edit_nickname),
        ) { dismissThen { onEditNickname(child) } }

        bindActionRow(
            row = dialogView.findViewById(R.id.row_regenerate_code),
            iconBgRes = R.drawable.bg_stat_icon_green,
            iconRes = R.drawable.ic_copy_outline,
            iconTint = ContextCompat.getColor(ctx, R.color.parent_mock_green),
            label = ctx.getString(R.string.parent_child_manage_regenerate_code),
        ) { dismissThen { onRegenerateCode(child.childId) } }

        bindActionRow(
            row = dialogView.findViewById(R.id.row_delete_child),
            iconBgRes = R.drawable.bg_stat_icon_red,
            iconRes = R.drawable.ic_exit_logout,
            iconTint = ContextCompat.getColor(ctx, R.color.parent_mock_logout),
            label = ctx.getString(R.string.parent_child_manage_delete),
            labelColor = ContextCompat.getColor(ctx, R.color.parent_mock_logout),
        ) { dismissThen { onDelete(child) } }

        dialogView.findViewById<MaterialButton>(R.id.btn_manage_close).setOnClickListener {
            dialog.dismiss()
        }
    }

    fun showEditNickname(
        fragment: Fragment,
        child: ParentChildItem,
        anchor: View,
        onSave: (String) -> Unit,
    ) {
        val ctx = fragment.requireContext()
        val dialogView = LayoutInflater.from(ctx)
            .inflate(R.layout.dialog_parent_child_nickname, null, false)
        val input = dialogView.findViewById<TextInputEditText>(R.id.input_child_nickname).apply {
            setText(child.nickname)
            setSelection(text?.length ?: 0)
        }
        val dialog = openCardDialog(fragment, dialogView)

        dialogView.findViewById<MaterialButton>(R.id.btn_nickname_cancel).setOnClickListener {
            dialog.dismiss()
        }
        dialogView.findViewById<MaterialButton>(R.id.btn_nickname_save).setOnClickListener {
            val name = input.text?.toString()?.trim().orEmpty()
            when {
                name.isBlank() ->
                    Snackbar.make(anchor, R.string.auth_error_nickname_empty, Snackbar.LENGTH_SHORT).show()
                name.length > 20 ->
                    Snackbar.make(anchor, R.string.auth_error_nickname_length, Snackbar.LENGTH_SHORT).show()
                else -> {
                    dialog.dismiss()
                    onSave(name)
                }
            }
        }
    }

    fun showRegenerateConfirm(
        fragment: Fragment,
        onConfirm: () -> Unit,
    ) {
        val ctx = fragment.requireContext()
        val dialogView = LayoutInflater.from(ctx)
            .inflate(R.layout.dialog_parent_child_confirm, null, false)
        styleConfirmDialog(
            dialogView = dialogView,
            iconBgRes = R.drawable.bg_stat_icon_blue,
            iconTint = ContextCompat.getColor(ctx, R.color.parent_mock_blue),
            title = ctx.getString(R.string.parent_child_regenerate_code_confirm_title),
            message = ctx.getString(R.string.parent_child_regenerate_code_confirm),
            confirmText = ctx.getString(R.string.parent_child_regenerate_code_confirm_action),
            confirmTint = ContextCompat.getColor(ctx, R.color.parent_mock_blue),
        )
        val dialog = openCardDialog(fragment, dialogView)
        dialogView.findViewById<MaterialButton>(R.id.btn_confirm_cancel).setOnClickListener {
            dialog.dismiss()
        }
        dialogView.findViewById<MaterialButton>(R.id.btn_confirm_action).setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }
    }

    fun showDeleteConfirm(
        fragment: Fragment,
        child: ParentChildItem,
        onConfirm: () -> Unit,
    ) {
        val ctx = fragment.requireContext()
        val dialogView = LayoutInflater.from(ctx)
            .inflate(R.layout.dialog_parent_child_confirm, null, false)
        styleConfirmDialog(
            dialogView = dialogView,
            iconBgRes = R.drawable.bg_stat_icon_red,
            iconTint = ContextCompat.getColor(ctx, R.color.parent_mock_logout),
            title = ctx.getString(R.string.parent_child_delete_confirm_title),
            message = ctx.getString(R.string.parent_child_delete_confirm, child.nickname),
            confirmText = ctx.getString(R.string.parent_child_manage_delete),
            confirmTint = ContextCompat.getColor(ctx, R.color.parent_mock_logout),
        )
        val dialog = openCardDialog(fragment, dialogView)
        dialogView.findViewById<MaterialButton>(R.id.btn_confirm_cancel).setOnClickListener {
            dialog.dismiss()
        }
        dialogView.findViewById<MaterialButton>(R.id.btn_confirm_action).setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }
    }

    fun showCodeRegenerated(
        fragment: Fragment,
        newCode: String,
        anchor: View,
    ) {
        val ctx = fragment.requireContext()
        val dialogView = LayoutInflater.from(ctx)
            .inflate(R.layout.dialog_parent_child_code_success, null, false)
        dialogView.findViewById<TextView>(R.id.tv_new_child_code).text = newCode
        val dialog = openCardDialog(fragment, dialogView)

        dialogView.findViewById<ImageButton>(R.id.btn_copy_new_code).setOnClickListener {
            copyCode(ctx, anchor, newCode)
        }
        dialogView.findViewById<MaterialButton>(R.id.btn_code_success_confirm).setOnClickListener {
            dialog.dismiss()
        }
    }

    private fun openCardDialog(fragment: Fragment, contentView: View): Dialog {
        val dialog = Dialog(fragment.requireContext()).apply {
            setContentView(contentView)
            setCancelable(true)
        }
        ParentFormDialogWindow.apply(dialog, fragment.requireContext())
        dialog.show()
        return dialog
    }

    private fun bindActionRow(
        row: View,
        iconBgRes: Int,
        iconRes: Int,
        iconTint: Int,
        label: String,
        labelColor: Int = ContextCompat.getColor(row.context, R.color.parent_mock_text_primary),
        onClick: () -> Unit,
    ) {
        row.findViewById<FrameLayout>(R.id.frame_action_icon).setBackgroundResource(iconBgRes)
        row.findViewById<ImageView>(R.id.iv_action_icon).apply {
            setImageResource(iconRes)
            setColorFilter(iconTint)
        }
        row.findViewById<TextView>(R.id.tv_action_label).apply {
            text = label
            setTextColor(labelColor)
        }
        row.setOnClickListener { onClick() }
    }

    private fun styleConfirmDialog(
        dialogView: View,
        iconBgRes: Int,
        iconTint: Int,
        title: String,
        message: String,
        confirmText: String,
        confirmTint: Int,
    ) {
        dialogView.findViewById<FrameLayout>(R.id.frame_confirm_icon).setBackgroundResource(iconBgRes)
        dialogView.findViewById<ImageView>(R.id.iv_confirm_icon).setColorFilter(iconTint)
        dialogView.findViewById<TextView>(R.id.tv_confirm_title).text = title
        dialogView.findViewById<TextView>(R.id.tv_confirm_message).text = message
        dialogView.findViewById<MaterialButton>(R.id.btn_confirm_action).apply {
            text = confirmText
            backgroundTintList = android.content.res.ColorStateList.valueOf(confirmTint)
        }
    }

    private fun copyCode(context: Context, anchor: View, code: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("child_code", code))
        Snackbar.make(anchor, R.string.child_register_code_copied, Snackbar.LENGTH_SHORT).show()
    }
}
