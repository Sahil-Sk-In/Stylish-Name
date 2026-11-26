package com.sahil.namemaker.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sahil.namemaker.R
import com.sahil.namemaker.adapter.StyleDisplayItem
import com.sahil.namemaker.adapter.StyleRowAdapter
import com.sahil.namemaker.utils.FontUtils
import com.sahil.namemaker.utils.LocalStore
import com.sahil.namemaker.utils.ThemeManager

class SavedFragment : Fragment() {
    private var rootView: View? = null
    private var headerView: View? = null
    private var tvTitle: TextView? = null
    private var tvEmpty: TextView? = null
    private var tabFavs: TextView? = null
    private var tabNotes: TextView? = null
    private var tabRecent: TextView? = null
    private var recycler: RecyclerView? = null

    private lateinit var adapter: StyleRowAdapter
    private var activeTab = 0 // 0=Favs, 1=SavedTexts, 2=Recent

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_saved, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rootView = view.findViewById(R.id.savedRoot)
        headerView = view.findViewById(R.id.savedHeader)
        tvTitle = view.findViewById(R.id.tvSavedTitle)
        tvEmpty = view.findViewById(R.id.tvSavedEmpty)
        tabFavs = view.findViewById(R.id.tabFavorites)
        tabNotes = view.findViewById(R.id.tabSavedNotes)
        tabRecent = view.findViewById(R.id.tabRecent)
        recycler = view.findViewById(R.id.rvSaved)

        val tf = FontUtils.loadSymbolTypeface(requireContext())
        adapter = StyleRowAdapter(requireContext(), tf) { renderList() }
        recycler?.layoutManager = LinearLayoutManager(requireContext())
        recycler?.adapter = adapter

        tabFavs?.setOnClickListener { activeTab = 0; applyTheme(); renderList() }
        tabNotes?.setOnClickListener { activeTab = 1; applyTheme(); renderList() }
        tabRecent?.setOnClickListener { activeTab = 2; applyTheme(); renderList() }

        applyTheme()
        renderList()
    }

    override fun onResume() {
        super.onResume()
        applyTheme()
        renderList()
    }

    fun applyTheme() {
        val ctx = context ?: return
        val theme = ThemeManager.getCurrent(ctx)
        rootView?.setBackgroundColor(theme.bg)
        headerView?.setBackgroundColor(theme.dark)
        tvTitle?.setTextColor(theme.accent)

        val tabs = listOf(tabFavs, tabNotes, tabRecent)
        tabs.forEachIndexed { idx, tv ->
            val fill = if (idx == activeTab) theme.accent else theme.cardBg
            tv?.background = ThemeManager.roundedCard(fill, 0x22FFFFFF, 24f)
            tv?.setTextColor(ThemeManager.contrastOn(fill))
        }
        if (::adapter.isInitialized) adapter.setTheme(theme)
    }

    private fun renderList() {
        val ctx = context ?: return
        if (!::adapter.isInitialized) return
        val items = when (activeTab) {
            0 -> LocalStore.getFavorites(ctx)
            1 -> LocalStore.getSavedTexts(ctx)
            else -> LocalStore.getRecent(ctx)
        }
        tvEmpty?.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        adapter.submitList(items.map { StyleDisplayItem(it) })
    }
}
