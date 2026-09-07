package com.example.privatebrowser.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.privatebrowser.R
import com.example.privatebrowser.databinding.ItemTabBinding
import com.example.privatebrowser.model.BrowserTab
import com.example.privatebrowser.proxy.ProxyManager
import com.example.privatebrowser.utils.UrlUtils

class TabAdapter(
    private val proxyManager: ProxyManager,
    private val onTabSelected: (BrowserTab) -> Unit,
    private val onTabClosed: (BrowserTab) -> Unit
) : ListAdapter<BrowserTab, TabAdapter.TabViewHolder>(TabDiffCallback()) {

    var activeTabId: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
        val binding = ItemTabBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TabViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TabViewHolder(private val binding: ItemTabBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(tab: BrowserTab) {
            binding.tvTabNumber.text = "#${tab.tabNumber}"
            binding.tvTabTitle.text = if (tab.title.isBlank()) "New Tab" else tab.title
            val domain = UrlUtils.extractDomain(tab.url)
            binding.tvTabUrl.text = if (domain.isBlank()) "about:blank" else domain

            // Favicon
            if (tab.favicon != null) {
                binding.ivTabFavicon.setImageBitmap(tab.favicon)
                binding.ivTabFavicon.imageTintList = null
            } else {
                binding.ivTabFavicon.setImageResource(R.drawable.ic_incognito)
                binding.ivTabFavicon.setColorFilter(
                    ContextCompat.getColor(binding.root.context, R.color.accent_blue)
                )
            }

            // Proxy info tag
            val assignedProxy = proxyManager.getProfileForTab(tab.id)
            if (assignedProxy != null && assignedProxy.enabled) {
                binding.tvTabProxyBadge.visibility = View.VISIBLE
                binding.tvTabProxyBadge.text = "Proxy: ${assignedProxy.name}"
            } else {
                binding.tvTabProxyBadge.visibility = View.GONE
            }

            // Selection state
            val isSelected = tab.id == activeTabId
            binding.cardTab.strokeWidth = if (isSelected) 6 else 0

            binding.root.setOnClickListener {
                onTabSelected(tab)
            }

            binding.btnCloseTab.setOnClickListener {
                onTabClosed(tab)
            }
        }
    }

    class TabDiffCallback : DiffUtil.ItemCallback<BrowserTab>() {
        override fun areItemsTheSame(oldItem: BrowserTab, newItem: BrowserTab): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: BrowserTab, newItem: BrowserTab): Boolean {
            return oldItem.title == newItem.title &&
                    oldItem.url == newItem.url &&
                    oldItem.isLoading == newItem.isLoading &&
                    oldItem.assignedProxyId == newItem.assignedProxyId
        }
    }
}
