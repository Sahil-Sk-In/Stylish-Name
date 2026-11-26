package com.sahil.namemaker.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sahil.namemaker.R
import com.sahil.namemaker.adapter.StyleDisplayItem
import com.sahil.namemaker.adapter.StyleRowAdapter
import com.sahil.namemaker.data.FeedItem
import com.sahil.namemaker.data.OfflineRepository
import com.sahil.namemaker.utils.FontUtils
import com.sahil.namemaker.utils.ThemeManager
import kotlinx.coroutines.launch

class FeedFragment : Fragment() {
    private var rootView: View? = null
    private var headerView: View? = null
    private var tvTitle: TextView? = null
    private var etSearch: EditText? = null
    private var tabPosts: TextView? = null
    private var tabMoments: TextView? = null
    private var recycler: RecyclerView? = null

    private lateinit var adapter: StyleRowAdapter
    private var posts: List<FeedItem> = emptyList()
    private var moments: List<FeedItem> = emptyList()
    private var showMoments = false
    private var query = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_feed, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rootView = view.findViewById(R.id.feedRoot)
        headerView = view.findViewById(R.id.feedHeader)
        tvTitle = view.findViewById(R.id.tvFeedTitle)
        etSearch = view.findViewById(R.id.etFeedSearch)
        tabPosts = view.findViewById(R.id.tabPosts)
        tabMoments = view.findViewById(R.id.tabMoments)
        recycler = view.findViewById(R.id.rvFeed)

        val tf = FontUtils.loadSymbolTypeface(requireContext())
        adapter = StyleRowAdapter(requireContext(), tf)
        recycler?.layoutManager = LinearLayoutManager(requireContext())
        recycler?.adapter = adapter

        tabPosts?.setOnClickListener {
            showMoments = false
            applyTheme()
            renderList()
        }
        tabMoments?.setOnClickListener {
            showMoments = true
            applyTheme()
            renderList()
        }

        etSearch?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                query = s?.toString()?.trim() ?: ""
                renderList()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        lifecycleScope.launch {
            posts = OfflineRepository.getPosts(requireContext())
            moments = OfflineRepository.getMoments(requireContext())
            tabPosts?.text = "Posts (${posts.size})"
            tabMoments?.text = "Moments (${moments.size})"
            renderList()
            applyTheme()
        }
    }

    override fun onResume() {
        super.onResume()
        applyTheme()
    }

    fun applyTheme() {
        val ctx = context ?: return
        val theme = ThemeManager.getCurrent(ctx)
        rootView?.setBackgroundColor(theme.bg)
        headerView?.setBackgroundColor(theme.dark)
        tvTitle?.setTextColor(theme.accent)

        val postsFill = if (!showMoments) theme.accent else theme.cardBg
        val momentsFill = if (showMoments) theme.accent else theme.cardBg
        tabPosts?.background = ThemeManager.roundedCard(postsFill, 0x22FFFFFF, 24f)
        tabPosts?.setTextColor(ThemeManager.contrastOn(postsFill))
        tabMoments?.background = ThemeManager.roundedCard(momentsFill, 0x22FFFFFF, 24f)
        tabMoments?.setTextColor(ThemeManager.contrastOn(momentsFill))

        if (::adapter.isInitialized) adapter.setTheme(theme)
    }

    private fun renderList() {
        if (!::adapter.isInitialized) return
        val src = if (showMoments) moments else posts
        val filtered = if (query.isEmpty()) src else src.filter {
            it.text.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true) ||
                it.author.contains(query, ignoreCase = true)
        }
        adapter.submitList(filtered.map { StyleDisplayItem(it.text, "${it.author} · ${it.category}") })
    }
}
