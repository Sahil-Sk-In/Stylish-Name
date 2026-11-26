package com.sahil.namemaker.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sahil.namemaker.R
import com.sahil.namemaker.adapter.StyleDisplayItem
import com.sahil.namemaker.adapter.StyleRowAdapter
import com.sahil.namemaker.data.OfflineRepository
import com.sahil.namemaker.utils.FontUtils
import com.sahil.namemaker.utils.LocalStore
import com.sahil.namemaker.utils.ThemeManager
import kotlinx.coroutines.launch

class CrunchFragment : Fragment() {
    private var rootView: View? = null
    private var headerView: View? = null
    private var tvTitle: TextView? = null
    private var etName: EditText? = null
    private var btnGenerate: Button? = null
    private var subTabsRow: LinearLayout? = null
    private var optionsPanel: View? = null
    private var etIntro: EditText? = null
    private var etOutro: EditText? = null
    private var cbMix: CheckBox? = null
    private var cbShuffle: CheckBox? = null
    private var recycler: RecyclerView? = null

    private lateinit var adapter: StyleRowAdapter
    private val tabLabels = listOf("Generated", "Intros", "Outros", "Symbols", "Options")
    private val tabViews = mutableListOf<TextView>()
    private var activeTab = 0
    private var currentIntro = ""
    private var currentOutro = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_crunch, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rootView = view.findViewById(R.id.crunchRoot)
        headerView = view.findViewById(R.id.crunchHeader)
        tvTitle = view.findViewById(R.id.tvCrunchTitle)
        etName = view.findViewById(R.id.etCrunchName)
        btnGenerate = view.findViewById(R.id.btnGenerate)
        subTabsRow = view.findViewById(R.id.crunchSubTabs)
        optionsPanel = view.findViewById(R.id.crunchOptionsPanel)
        etIntro = view.findViewById(R.id.etIntro)
        etOutro = view.findViewById(R.id.etOutro)
        cbMix = view.findViewById(R.id.cbMixSymbols)
        cbShuffle = view.findViewById(R.id.cbShuffle)
        recycler = view.findViewById(R.id.rvCrunch)

        etName?.setText(LocalStore.getUserName(requireContext()))

        val tf = FontUtils.loadSymbolTypeface(requireContext())
        adapter = StyleRowAdapter(requireContext(), tf)
        recycler?.layoutManager = LinearLayoutManager(requireContext())
        recycler?.adapter = adapter

        buildSubTabs()

        btnGenerate?.setOnClickListener {
            activeTab = 0
            updateSubTabStyles()
            optionsPanel?.visibility = View.GONE
            renderTab()
        }

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (activeTab == 0) renderTab()
            }
            override fun afterTextChanged(s: Editable?) {}
        }
        etName?.addTextChangedListener(watcher)
        etIntro?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentIntro = s?.toString() ?: ""
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        etOutro?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentOutro = s?.toString() ?: ""
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        cbMix?.setOnCheckedChangeListener { _, _ -> if (activeTab == 0) renderTab() }
        cbShuffle?.setOnCheckedChangeListener { _, _ -> if (activeTab == 0) renderTab() }

        lifecycleScope.launch {
            OfflineRepository.loadCrunchData(requireContext())
            renderTab()
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
        btnGenerate?.setBackgroundColor(theme.accent)
        btnGenerate?.setTextColor(ThemeManager.contrastOn(theme.accent))
        if (::adapter.isInitialized) adapter.setTheme(theme)
        updateSubTabStyles()
    }

    private fun buildSubTabs() {
        val row = subTabsRow ?: return
        val ctx = context ?: return
        row.removeAllViews()
        tabViews.clear()
        val density = resources.displayMetrics.density

        tabLabels.forEachIndexed { index, label ->
            val tv = TextView(ctx).apply {
                text = label
                textSize = 12f
                setPadding((14 * density).toInt(), (6 * density).toInt(), (14 * density).toInt(), (6 * density).toInt())
                setOnClickListener {
                    activeTab = index
                    updateSubTabStyles()
                    optionsPanel?.visibility = if (index == 4) View.VISIBLE else View.GONE
                    renderTab()
                }
            }
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            lp.marginEnd = (8 * density).toInt()
            row.addView(tv, lp)
            tabViews.add(tv)
        }
        updateSubTabStyles()
    }

    private fun updateSubTabStyles() {
        val ctx = context ?: return
        val theme = ThemeManager.getCurrent(ctx)
        tabViews.forEachIndexed { index, tv ->
            val selected = index == activeTab
            val fill = if (selected) theme.accent else theme.cardBg
            tv.background = ThemeManager.roundedCard(fill, 0x22FFFFFF, 30f)
            tv.setTextColor(ThemeManager.contrastOn(fill))
        }
    }

    private fun renderTab() {
        if (!::adapter.isInitialized) return
        val name = etName?.text?.toString() ?: "Sahil"
        val mix = cbMix?.isChecked ?: true
        val shuffle = cbShuffle?.isChecked ?: false

        when (activeTab) {
            0 -> {
                val styles = OfflineRepository.generateCrunchStyles(name, currentIntro, currentOutro, mix, shuffle)
                adapter.submitList(styles.map { StyleDisplayItem(it.second, it.first) })
            }
            1 -> {
                val intros = OfflineRepository.getDecorations().filter { it.type == "intro" }
                adapter.submitList(intros.map { StyleDisplayItem(it.text, "Intro (tap to copy or long-press)") })
                if (intros.isNotEmpty()) {
                    Toast.makeText(requireContext(), "Tip: Enter Intro in Options tab to auto-apply", Toast.LENGTH_SHORT).show()
                }
            }
            2 -> {
                val outros = OfflineRepository.getDecorations().filter { it.type == "outro" }
                adapter.submitList(outros.map { StyleDisplayItem(it.text, "Outro") })
            }
            3 -> {
                val syms = OfflineRepository.getCrunchSymbols()
                adapter.submitList(syms.map { StyleDisplayItem(it, "Combining Symbol") })
            }
            4 -> {
                val styles = OfflineRepository.generateCrunchStyles(name, currentIntro, currentOutro, mix, shuffle)
                adapter.submitList(styles.map { StyleDisplayItem(it.second, it.first) })
            }
        }
    }
}
