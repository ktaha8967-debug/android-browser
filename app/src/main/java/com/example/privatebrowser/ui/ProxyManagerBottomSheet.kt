package com.example.privatebrowser.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.privatebrowser.R
import com.example.privatebrowser.core.TabManager
import com.example.privatebrowser.databinding.DialogProxyEditBinding
import com.example.privatebrowser.databinding.DialogProxyManagerBinding
import com.example.privatebrowser.model.ProxyProfile
import com.example.privatebrowser.proxy.ProxyManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ProxyManagerBottomSheet(
    private val proxyManager: ProxyManager,
    private val tabManager: TabManager
) : BottomSheetDialogFragment() {

    private var _binding: DialogProxyManagerBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ProxyAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogProxyManagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ProxyAdapter(
            tabManager = tabManager,
            onToggleEnabled = { profile, isEnabled ->
                profile.enabled = isEnabled
                proxyManager.addOrUpdateProfile(profile)
                tabManager.activeTab.value?.id?.let { activeTabId ->
                    proxyManager.updateProxyForActiveTab(activeTabId)
                }
            },
            onEdit = { profile ->
                showEditProxyDialog(profile)
            },
            onDelete = { profile ->
                proxyManager.deleteProfile(profile.id)
                tabManager.activeTab.value?.id?.let { activeTabId ->
                    proxyManager.updateProxyForActiveTab(activeTabId)
                }
            }
        )

        binding.rvProxies.adapter = adapter

        proxyManager.profiles.observe(viewLifecycleOwner) { profiles ->
            adapter.submitList(profiles)
            binding.tvEmptyProxies.visibility = if (profiles.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.btnAddProxy.setOnClickListener {
            showEditProxyDialog(null)
        }
    }

    private fun showEditProxyDialog(existingProfile: ProxyProfile?) {
        val editBinding = DialogProxyEditBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(editBinding.root)
            .create()

        editBinding.tvProxyDialogTitle.text = if (existingProfile != null) "Edit Proxy Profile" else "Add Proxy Profile"

        if (existingProfile != null) {
            editBinding.etProxyName.setText(existingProfile.name)
            editBinding.etProxyHost.setText(existingProfile.host)
            editBinding.etProxyPort.setText(existingProfile.port.toString())
            editBinding.etProxyUsername.setText(existingProfile.username ?: "")
            editBinding.etProxyPassword.setText(existingProfile.password ?: "")
        }

        // Setup Tab Assignment Spinner
        val tabs = tabManager.getAllTabs()
        val spinnerItems = mutableListOf("None (Global / Unassigned)")
        val tabIds = mutableListOf<String?>(null)

        tabs.forEach { tab ->
            spinnerItems.add("Tab #${tab.tabNumber}: ${tab.title.take(18)}")
            tabIds.add(tab.id)
        }

        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, spinnerItems)
        editBinding.spinnerAssignTab.adapter = spinnerAdapter

        // Set initial selected tab
        if (existingProfile?.assignedTabId != null) {
            val selectedIndex = tabIds.indexOf(existingProfile.assignedTabId)
            if (selectedIndex >= 0) {
                editBinding.spinnerAssignTab.setSelection(selectedIndex)
            }
        }

        // Test Proxy
        editBinding.btnTestProxy.setOnClickListener {
            val host = editBinding.etProxyHost.text?.toString()?.trim() ?: ""
            val portStr = editBinding.etProxyPort.text?.toString()?.trim() ?: ""
            val port = portStr.toIntOrNull() ?: 0

            if (host.isBlank() || port <= 0) {
                editBinding.tvProxyTestStatus.text = "Enter valid host and port first."
                return@setOnClickListener
            }

            editBinding.tvProxyTestStatus.text = "Testing connection..."
            val testProfile = ProxyProfile(
                name = "Test",
                host = host,
                port = port
            )
            proxyManager.testProxy(testProfile) { success, msg ->
                editBinding.tvProxyTestStatus.text = msg
            }
        }

        editBinding.btnCancelProxyEdit.setOnClickListener {
            dialog.dismiss()
        }

        editBinding.btnSaveProxy.setOnClickListener {
            val name = editBinding.etProxyName.text?.toString()?.trim() ?: ""
            val host = editBinding.etProxyHost.text?.toString()?.trim() ?: ""
            val portStr = editBinding.etProxyPort.text?.toString()?.trim() ?: ""
            val port = portStr.toIntOrNull() ?: 0
            val username = editBinding.etProxyUsername.text?.toString()?.trim().takeIf { !it.isNullOrBlank() }
            val password = editBinding.etProxyPassword.text?.toString()?.trim().takeIf { !it.isNullOrBlank() }
            val selectedTabIndex = editBinding.spinnerAssignTab.selectedItemPosition
            val assignedTabId = if (selectedTabIndex in tabIds.indices) tabIds[selectedTabIndex] else null

            if (name.isBlank()) {
                Toast.makeText(requireContext(), "Please enter a profile name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (host.isBlank() || port !in 1..65535) {
                Toast.makeText(requireContext(), "Please enter a valid host and port (1-65535)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val profile = existingProfile?.copy(
                name = name,
                host = host,
                port = port,
                username = username,
                password = password,
                assignedTabId = assignedTabId
            ) ?: ProxyProfile(
                name = name,
                host = host,
                port = port,
                username = username,
                password = password,
                assignedTabId = assignedTabId
            )

            proxyManager.addOrUpdateProfile(profile)
            tabManager.activeTab.value?.id?.let { activeTabId ->
                proxyManager.updateProxyForActiveTab(activeTabId)
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "ProxyManagerBottomSheet"
    }
}
