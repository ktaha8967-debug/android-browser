package com.example.privatebrowser.ui

import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.privatebrowser.R
import com.example.privatebrowser.core.AutoClaimManager
import com.example.privatebrowser.core.TabManager
import com.example.privatebrowser.databinding.DialogMultiInputBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip

class MultiInputDialog(
    private val tabManager: TabManager
) : BottomSheetDialogFragment() {

    private var _binding: DialogMultiInputBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogMultiInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val allTabs = tabManager.getAllTabs()
        val activeTab = tabManager.activeTab.value

        // Populate chips for selected tabs mode
        binding.chipGroupTabs.removeAllViews()
        val chipMap = mutableMapOf<String, Chip>()

        for (tab in allTabs) {
            val chip = Chip(requireContext()).apply {
                text = "Tab #${tab.tabNumber}: ${tab.title.take(14)}"
                isCheckable = true
                isChecked = true
            }
            binding.chipGroupTabs.addView(chip)
            chipMap[tab.id] = chip
        }

        binding.rgInputScope.setOnCheckedChangeListener { _, checkedId ->
            binding.chipGroupTabs.visibility = if (checkedId == R.id.rbScopeSelectedTabs) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

        // Auto paste from clipboard if clipboard contains a code
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clipData = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
        if (!clipData.isNullOrBlank() && !clipData.startsWith("http://") && !clipData.startsWith("https://")) {
            binding.etMultiInputText.setText(clipData)
        }

        binding.btnPasteClipboard.setOnClickListener {
            val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
            if (!clip.isNullOrBlank()) {
                binding.etMultiInputText.setText(clip)
                Toast.makeText(requireContext(), "Pasted: $clip", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Clipboard is empty", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnReloadAllTabs.setOnClickListener {
            AutoClaimManager.reloadAllTabs(allTabs)
            Toast.makeText(requireContext(), "Reloading all ${allTabs.size} tabs...", Toast.LENGTH_SHORT).show()
        }

        binding.btnCancelMultiInput.setOnClickListener {
            dismiss()
        }

        binding.btnSendToTabs.setOnClickListener {
            val codeToInject = binding.etMultiInputText.text?.toString()?.trim() ?: ""
            if (codeToInject.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter or paste a gift code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val targetTabs = when (binding.rgInputScope.checkedRadioButtonId) {
                R.id.rbScopeCurrentTab -> {
                    activeTab?.let { listOf(it) } ?: emptyList()
                }
                R.id.rbScopeSelectedTabs -> {
                    allTabs.filter { tab -> chipMap[tab.id]?.isChecked == true }
                }
                else -> {
                    allTabs // All tabs
                }
            }

            if (targetTabs.isEmpty()) {
                Toast.makeText(requireContext(), "No tabs selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val autoClickRedeem = binding.cbAutoClickRedeem.isChecked
            binding.btnSendToTabs.isEnabled = false

            AutoClaimManager.claimAcrossTabs(targetTabs, codeToInject, autoClickRedeem) { result ->
                if (!isAdded) return@claimAcrossTabs
                binding.btnSendToTabs.isEnabled = true

                if (result.successCount > 0) {
                    Toast.makeText(
                        requireContext(),
                        "⚡ Code claimed on ${result.successCount} of ${result.totalTabs} accounts!",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    val firstErr = result.details.firstOrNull() ?: "Input field not found"
                    Toast.makeText(
                        requireContext(),
                        "Could not claim: $firstErr",
                        Toast.LENGTH_LONG
                    ).show()
                }
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "MultiInputDialog"
    }
}
