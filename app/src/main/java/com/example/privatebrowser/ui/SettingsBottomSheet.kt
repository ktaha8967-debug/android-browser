package com.example.privatebrowser.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.example.privatebrowser.R
import com.example.privatebrowser.core.PrivateWebViewFactory
import com.example.privatebrowser.databinding.DialogSettingsBinding
import com.example.privatebrowser.model.AccentColor
import com.example.privatebrowser.model.SearchEngine
import com.example.privatebrowser.model.SearchEngineType
import com.example.privatebrowser.model.ThemeMode
import com.example.privatebrowser.model.ToolbarPosition
import com.example.privatebrowser.settings.SettingsManager
import com.example.privatebrowser.utils.ThemeHelper
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SettingsBottomSheet(
    private val settingsManager: SettingsManager,
    private val onSettingsChanged: () -> Unit,
    private val onOpenProxyManager: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Setup Theme
        when (settingsManager.getThemeMode()) {
            ThemeMode.SYSTEM -> binding.rbThemeSystem.isChecked = true
            ThemeMode.LIGHT -> binding.rbThemeLight.isChecked = true
            ThemeMode.DARK -> binding.rbThemeDark.isChecked = true
        }

        binding.rgTheme.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.rbThemeLight -> ThemeMode.LIGHT
                R.id.rbThemeDark -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
            settingsManager.setThemeMode(mode)
            ThemeHelper.applyTheme(mode)
            onSettingsChanged()
        }

        // 2. Setup Accent Colors
        binding.colorBlue.setOnClickListener { selectAccentColor(AccentColor.BLUE) }
        binding.colorEmerald.setOnClickListener { selectAccentColor(AccentColor.EMERALD) }
        binding.colorPurple.setOnClickListener { selectAccentColor(AccentColor.PURPLE) }
        binding.colorAmber.setOnClickListener { selectAccentColor(AccentColor.AMBER) }
        binding.colorRose.setOnClickListener { selectAccentColor(AccentColor.ROSE) }
        binding.colorCyan.setOnClickListener { selectAccentColor(AccentColor.CYAN) }

        // 3. Setup Search Engine
        val searchEngineTypes = SearchEngineType.values().map { it.displayName }
        val searchAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, searchEngineTypes)
        binding.spinnerSearchEngine.adapter = searchAdapter

        val currentEngine = settingsManager.getSearchEngine()
        val currentEngineIndex = SearchEngineType.values().indexOf(currentEngine.type)
        if (currentEngineIndex >= 0) {
            binding.spinnerSearchEngine.setSelection(currentEngineIndex)
        }
        if (currentEngine.type == SearchEngineType.CUSTOM) {
            binding.tilCustomSearchUrl.visibility = View.VISIBLE
            binding.etCustomSearchUrl.setText(currentEngine.customUrl)
        }

        binding.spinnerSearchEngine.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedType = SearchEngineType.values()[position]
                if (selectedType == SearchEngineType.CUSTOM) {
                    binding.tilCustomSearchUrl.visibility = View.VISIBLE
                } else {
                    binding.tilCustomSearchUrl.visibility = View.GONE
                    settingsManager.setSearchEngine(SearchEngine(selectedType, ""))
                    onSettingsChanged()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.etCustomSearchUrl.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val url = binding.etCustomSearchUrl.text?.toString() ?: ""
                settingsManager.setSearchEngine(SearchEngine(SearchEngineType.CUSTOM, url))
                onSettingsChanged()
            }
        }

        // 4. Toolbar Configuration
        binding.switchToolbarVisible.isChecked = settingsManager.isBottomToolbarVisible()
        binding.switchToolbarVisible.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setBottomToolbarVisible(isChecked)
            onSettingsChanged()
        }

        binding.switchCompactMode.isChecked = settingsManager.isCompactMode()
        binding.switchCompactMode.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setCompactMode(isChecked)
            onSettingsChanged()
        }

        // 5. Proxy shortcut
        binding.btnOpenProxyManager.setOnClickListener {
            dismiss()
            onOpenProxyManager()
        }

        // 6. Clear session data action
        binding.btnClearAllData.setOnClickListener {
            PrivateWebViewFactory.clearGlobalIncognitoStorage(requireContext())
            Toast.makeText(requireContext(), "All session cache, cookies and storage wiped clean", Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectAccentColor(color: AccentColor) {
        settingsManager.setAccentColor(color)
        Toast.makeText(requireContext(), "Accent color set to ${color.hexName}", Toast.LENGTH_SHORT).show()
        onSettingsChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "SettingsBottomSheet"
    }
}
