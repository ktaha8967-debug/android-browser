package com.example.privatebrowser.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.privatebrowser.BrowserViewModel
import com.example.privatebrowser.core.TabManager
import com.example.privatebrowser.databinding.DialogTabSwitcherBinding
import com.example.privatebrowser.proxy.ProxyManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class TabSwitcherBottomSheet(
    private val viewModel: BrowserViewModel,
    private val tabManager: TabManager,
    private val proxyManager: ProxyManager
) : BottomSheetDialogFragment() {

    private var _binding: DialogTabSwitcherBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TabAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTabSwitcherBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = TabAdapter(
            proxyManager = proxyManager,
            onTabSelected = { tab ->
                tabManager.selectTab(tab.id)
                dismiss()
            },
            onTabClosed = { tab ->
                tabManager.closeTab(tab.id)
            }
        )

        binding.rvTabs.adapter = adapter

        tabManager.tabs.observe(viewLifecycleOwner) { tabList ->
            adapter.submitList(tabList)
            binding.tvTabSwitcherHeader.text = "Tabs (${tabList.size})"
        }

        tabManager.activeTab.observe(viewLifecycleOwner) { active ->
            adapter.activeTabId = active?.id
            adapter.notifyDataSetChanged()
        }

        binding.btnNewTabInSwitcher.setOnClickListener {
            tabManager.createNewTab()
            dismiss()
        }

        binding.btnCloseAllTabs.setOnClickListener {
            tabManager.closeAllTabs()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "TabSwitcherBottomSheet"
    }
}
