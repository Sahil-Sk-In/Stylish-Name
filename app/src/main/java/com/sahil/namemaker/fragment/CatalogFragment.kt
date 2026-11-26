package com.sahil.namemaker.fragment

import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sahil.namemaker.R
import com.sahil.namemaker.adapter.StyleDisplayItem
import com.sahil.namemaker.adapter.StyleRowAdapter
import com.sahil.namemaker.data.CatalogSection
import com.sahil.namemaker.data.OfflineRepository
import com.sahil.namemaker.utils.FontUtils
import com.sahil.namemaker.utils.LocalStore
import com.sahil.namemaker.utils.ThemeManager
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class CatalogFragment : Fragment() {
    private var rootView: View? = null
    private var headerView: View? = null
    private var tvTitle: TextView? = null
    private var tvCount: TextView? = null
    private var btnCopyAll: Button? = null
    private var etSearch: EditText? = null
    private var chipsRow: LinearLayout? = null
    private var recycler: RecyclerView? = null

    private lateinit var adapter: StyleRowAdapter
    private var sections: List<CatalogSection> = emptyList()
    private val chipViews = mutableListOf<TextView>()
    private var activeChipIndex = 0
    private var searchQuery = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_catalog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rootView = view.findViewById(R.id.catalogRoot)
        headerView = view.findViewById(R.id.catalogHeader)
        tvTitle = view.findViewById(R.id.tvCatalogTitle)
        tvCount = view.findViewById(R.id.tvCatalogCount)
        btnCopyAll = view.findViewById(R.id.btnCopyAll)
        etSearch = view.findViewById(R.id.etCatalogSearch)
        chipsRow = view.findViewById(R.id.catalogChipsRow)
        recycler = view.findViewById(R.id.rvCatalog)

        adapter = StyleRowAdapter(requireContext(), Typeface.DEFAULT) {
            updateSavedChipText()
            if (activeChipIndex == sections.size + 1) {
                renderItems()
            }
        }
        recycler?.layoutManager = LinearLayoutManager(requireContext())
        recycler?.adapter = adapter

        etSearch?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim() ?: ""
                renderItems()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnCopyAll?.setOnClickListener {
            val all = adapter.getAllValues()
            if (all.isNotEmpty()) {
                val sb = StringBuilder()
                var count = 0
                for (item in all) {
                    if (sb.length + item.length + 1 > 350_000) break
                    if (sb.isNotEmpty()) sb.append("\n")
                    sb.append(item)
                    count++
                }
                LocalStore.copyAndTrack(requireContext(), sb.toString(), "Copied $count items")
            }
        }

        lifecycleScope.launch {
            val ctx = requireContext()
            val tfJob = async { FontUtils.loadSymbolTypefaceAsync(ctx) }
            val catalogJob = async { OfflineRepository.getCatalog(ctx) }
            adapter.setTypeface(tfJob.await())
            sections = catalogJob.await()
                .filter { it.id == "names" || it.id == "bios" }
            buildChips()
            renderItems()
            applyTheme()
        }
    }

    override fun onResume() {
        super.onResume()
        updateSavedChipText()
        if (activeChipIndex == sections.size + 1 && sections.isNotEmpty()) {
            renderItems()
        }
        applyTheme()
    }

    fun selectSavedTab() {
        if (sections.isEmpty()) return
        activeChipIndex = sections.size + 1
        updateChipStyles()
        renderItems()
    }

    fun applyTheme() {
        val ctx = context ?: return
        val theme = ThemeManager.getCurrent(ctx)
        rootView?.setBackgroundColor(theme.bg)
        headerView?.setBackgroundColor(theme.dark)
        tvTitle?.setTextColor(theme.accent)
        tvCount?.setTextColor(ThemeManager.contrastOn(theme.dark))
        btnCopyAll?.setTextColor(theme.accent)
        if (::adapter.isInitialized) adapter.setTheme(theme)
        updateChipStyles()
    }

    private fun buildChips() {
        val row = chipsRow ?: return
        val ctx = context ?: return
        row.removeAllViews()
        chipViews.clear()

        val labels = mutableListOf("All")
        sections.forEach { labels.add("${it.title} (${it.items.size})") }
        labels.add("Saved ★ (${getSavedItems().size})")

        val density = resources.displayMetrics.density
        labels.forEachIndexed { index, label ->
            val chip = TextView(ctx).apply {
                text = label
                textSize = 12f
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                gravity = android.view.Gravity.CENTER
                setPadding((14 * density).toInt(), (7 * density).toInt(), (14 * density).toInt(), (7 * density).toInt())
                setOnClickListener {
                    activeChipIndex = index
                    updateChipStyles()
                    renderItems()
                }
            }
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.marginEnd = (8 * density).toInt()
            row.addView(chip, lp)
            chipViews.add(chip)
        }
        updateChipStyles()
    }

    private fun getSavedItems(): List<String> {
        val ctx = context ?: return emptyList()
        return (LocalStore.getFavorites(ctx) + LocalStore.getSavedTexts(ctx)).distinct()
    }

    private fun updateSavedChipText() {
        val savedChip = chipViews.lastOrNull() ?: return
        if (chipViews.size >= 2) {
            savedChip.text = "Saved ★ (${getSavedItems().size})"
        }
    }

    private fun updateChipStyles() {
        val ctx = context ?: return
        val theme = ThemeManager.getCurrent(ctx)
        chipViews.forEachIndexed { idx, tv ->
            val selected = idx == activeChipIndex
            val fill = if (selected) theme.dark else theme.cardBg
            val stroke = if (selected) theme.dark else 0x22000000
            tv.background = ThemeManager.roundedCard(fill, stroke, 40f)
            tv.setTextColor(ThemeManager.contrastOn(fill))
        }
    }

    private fun renderItems() {
        if (!::adapter.isInitialized) return
        val savedTabIndex = sections.size + 1
        val source = when {
            activeChipIndex == 0 -> sections.flatMap { it.items }
            activeChipIndex == savedTabIndex -> getSavedItems()
            else -> sections.getOrNull(activeChipIndex - 1)?.items ?: emptyList()
        }

        val filtered = if (searchQuery.isEmpty()) {
            if (activeChipIndex == 0) source.take(2000) else source
        } else {
            source.filter { it.contains(searchQuery, ignoreCase = true) }.take(2000)
        }

        adapter.submitList(filtered.map { StyleDisplayItem(it) })
        tvCount?.text = if (filtered.size < source.size) {
            "${filtered.size} / ${source.size}"
        } else {
            "${filtered.size} items"
        }
    }
}
