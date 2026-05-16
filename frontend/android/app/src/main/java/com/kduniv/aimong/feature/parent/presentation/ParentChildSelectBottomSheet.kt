package com.kduniv.aimong.feature.parent.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.kduniv.aimong.R
import com.kduniv.aimong.core.network.model.ParentChildItem
import com.kduniv.aimong.feature.parent.domain.ParentAuthPolicy
import com.kduniv.aimong.databinding.BottomSheetParentChildSelectBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ParentChildSelectBottomSheet : BottomSheetDialogFragment() {

    interface Listener {
        fun onAddChildRequested()
    }

    private var _binding: BottomSheetParentChildSelectBinding? = null
    private val binding get() = _binding!!

    private val dashboardViewModel: ParentDashboardViewModel by viewModels({ requireParentFragment() })

    private lateinit var adapter: ParentChildSheetAdapter

    private var onChildLongPress: ((ParentChildItem) -> Unit)? = null

    override fun getTheme(): Int = R.style.AimongBottomSheetDialogTheme

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetParentChildSelectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ParentChildSheetAdapter(
            onSelectChild = { childId ->
                dashboardViewModel.selectChild(childId)
                dismiss()
            },
            onChildLongPress = onChildLongPress
        )
        binding.rvSheetChildren.layoutManager = LinearLayoutManager(requireContext())
        binding.rvSheetChildren.adapter = adapter

        binding.btnCloseSheet.setOnClickListener { dismiss() }
        binding.btnAddChildSheet.setOnClickListener {
            dismiss()
            (parentFragment as? Listener)?.onAddChildRequested()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    dashboardViewModel.children.collect { children ->
                        bindChildren(children, dashboardViewModel.selectedChildId.value)
                    }
                }
                launch {
                    dashboardViewModel.selectedChildId.collect { selectedId ->
                        adapter.selectedChildId = selectedId
                    }
                }
            }
        }
    }

    private fun bindChildren(children: List<ParentChildItem>, selectedId: String?) {
        adapter.selectedChildId = selectedId
        val rows = children.map { item -> item.toSheetRow(requireContext()) }
        adapter.submitList(rows)
        binding.tvSheetEmpty.isVisible = rows.isEmpty()
        binding.rvSheetChildren.isVisible = rows.isNotEmpty()
        val atLimit = children.size >= ParentAuthPolicy.MAX_CHILDREN
        binding.btnAddChildSheet.isEnabled = !atLimit
        binding.btnAddChildSheet.alpha = if (atLimit) 0.45f else 1f
        binding.tvChildLimitHint.isVisible = atLimit
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "parent_child_select_sheet"

        fun newInstance(
            onChildLongPress: ((ParentChildItem) -> Unit)? = null
        ): ParentChildSelectBottomSheet = ParentChildSelectBottomSheet().apply {
            this.onChildLongPress = onChildLongPress
        }
    }
}

private fun ParentChildItem.toSheetRow(context: android.content.Context): ParentChildSheetRow {
    val linked = !lastActiveAt.isNullOrBlank()
    val codeLine = code.trim().takeIf { it.isNotEmpty() }
        ?.let { context.getString(R.string.parent_child_sheet_code_fmt, it) }
    val subtitle = when {
        codeLine != null -> codeLine
        linked -> {
            val date = createdAt?.take(10).orEmpty().ifBlank { "—" }
            context.getString(R.string.parent_child_sheet_registered_fmt, date)
        }
        else -> context.getString(R.string.parent_child_sheet_pending)
    }
    return ParentChildSheetRow(item = this, subtitle = subtitle)
}
