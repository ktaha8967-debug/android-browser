package com.example.privatebrowser.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.privatebrowser.core.TabManager
import com.example.privatebrowser.databinding.ItemProxyBinding
import com.example.privatebrowser.model.ProxyProfile

class ProxyAdapter(
    private val tabManager: TabManager,
    private val onToggleEnabled: (ProxyProfile, Boolean) -> Unit,
    private val onEdit: (ProxyProfile) -> Unit,
    private val onDelete: (ProxyProfile) -> Unit
) : ListAdapter<ProxyProfile, ProxyAdapter.ProxyViewHolder>(ProxyDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProxyViewHolder {
        val binding = ItemProxyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProxyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProxyViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProxyViewHolder(private val binding: ItemProxyBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(profile: ProxyProfile) {
            binding.tvProxyProfileName.text = profile.name
            binding.tvProxyAddress.text = profile.getFormattedAddress()

            val assignedTab = profile.assignedTabId?.let { tabManager.getTabById(it) }
            binding.tvProxyAssignment.text = when {
                assignedTab != null -> "Assigned to Tab #${assignedTab.tabNumber} (${assignedTab.title})"
                profile.assignedTabId != null -> "Assigned to Closed Tab"
                else -> "Global (All unassigned tabs)"
            }

            binding.switchProxyEnabled.isChecked = profile.enabled
            binding.switchProxyEnabled.setOnCheckedChangeListener { _, isChecked ->
                onToggleEnabled(profile, isChecked)
            }

            binding.btnEditProxy.setOnClickListener {
                onEdit(profile)
            }

            binding.btnDeleteProxy.setOnClickListener {
                onDelete(profile)
            }
        }
    }

    class ProxyDiffCallback : DiffUtil.ItemCallback<ProxyProfile>() {
        override fun areItemsTheSame(oldItem: ProxyProfile, newItem: ProxyProfile): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ProxyProfile, newItem: ProxyProfile): Boolean {
            return oldItem == newItem
        }
    }
}
