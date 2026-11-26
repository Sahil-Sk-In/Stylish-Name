package com.sahil.namemaker.ui

import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.sahil.namemaker.R
import com.sahil.namemaker.adapter.KeyboardKeyAdapter
import com.sahil.namemaker.data.CatalogSection
import com.sahil.namemaker.data.OfflineRepository
import com.sahil.namemaker.databinding.ActivityEditorBinding
import com.sahil.namemaker.utils.FontUtils
import com.sahil.namemaker.utils.LocalStore
import com.sahil.namemaker.utils.TextHistory
import com.sahil.namemaker.utils.ThemeManager
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class EditorActivity : AppCompatActivity() {

    private data class KeyboardTab(
        val id: String,
        val label: String,
        val isList: Boolean = false
    )

    private data class GuideStep(
        val title: String,
        val body: String
    )

    companion object {
        private const val PREF_GUIDE = "StudioGuidePrefs"
        private const val KEY_GUIDE_SHOWN = "guide_shown_v1"
    }

    private lateinit var binding: ActivityEditorBinding
    private lateinit var symbolTypeface: Typeface
    private lateinit var keyAdapter: KeyboardKeyAdapter
    private val history = TextHistory()

    private var catalogSections: List<CatalogSection> = emptyList()
    private var alphabet: Map<String, List<String>> = emptyMap()

    private val tabs = mutableListOf<KeyboardTab>()
    private val tabViews = mutableListOf<TextView>()
    private val letterViews = mutableListOf<TextView>()

    private val guideSteps = listOf(
        GuideStep(
            "Step 1: Compose Your Style",
            "Tap any letter, symbol, or template below to insert it into your text."
        ),
        GuideStep(
            "Step 2: Move & Save Symbols",
            "Use Left or Right to move between characters. Hold (long-press) any symbol below to save it into the Saved tab."
        ),
        GuideStep(
            "Step 3: Save Created Names",
            "When your name is ready, tap Save (or hold the text box) to keep it in Saved. Hold any tab to copy all items."
        )
    )
    private var currentGuideStep = 0

    private var activeTabIndex = 0
    private var activeLetter = "A"
    private var fontSize = 17
    private var restoringHistory = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        symbolTypeface = Typeface.DEFAULT
        binding.editor.typeface = symbolTypeface

        fontSize = LocalStore.getEditorFontSize(this)
        binding.editor.textSize = fontSize.toFloat()
        binding.btnFontSize.text = "Size: ${fontSize}▼"

        val preset = intent.getStringExtra("preset_text")
        val initialText = if (!preset.isNullOrEmpty()) preset else LocalStore.getEditorText(this)
        binding.editor.setText(initialText)
        binding.editor.setSelection(initialText.length)
        history.reset(initialText)

        keyAdapter = KeyboardKeyAdapter(
            context = this,
            typeface = symbolTypeface,
            onKeyClick = { value -> onKeyTapped(value) },
            onKeyLongClick = { value -> onKeyLongPressed(value) }
        )
        binding.keyboardRecyclerView.adapter = keyAdapter

        setupEditorWatcher()
        setupToolbarListeners()
        setupGuideOverlay()
        buildLetterChips()
        applyTheme()

        if (savedInstanceState == null) {
            binding.editorRoot.alpha = 0f
            binding.editorRoot.animate().alpha(1f).setDuration(220L).start()
        }

        lifecycleScope.launch {
            val fontJob = async { FontUtils.loadSymbolTypefaceAsync(this@EditorActivity) }
            val catalogJob = async { OfflineRepository.getCatalog(this@EditorActivity) }
            val alphabetJob = async { OfflineRepository.getAlphabet(this@EditorActivity) }

            symbolTypeface = fontJob.await()
            binding.editor.typeface = symbolTypeface
            keyAdapter.setTypeface(symbolTypeface)

            catalogSections = catalogJob.await()
            alphabet = alphabetJob.await()
            buildTabs()
            selectTab(0)
            maybeShowFirstTimeGuide()
        }
    }

    override fun onPause() {
        super.onPause()
        LocalStore.setEditorText(this, binding.editor.text.toString())
    }

    private fun applyTheme() {
        val theme = ThemeManager.getCurrent(this)
        val onDark = ThemeManager.contrastOn(theme.dark)
        val onToolbar = ThemeManager.contrastOn(theme.toolbar)
        val onCorner = ThemeManager.contrastOn(theme.primary)

        binding.editorRoot.setBackgroundColor(theme.bg)
        binding.editorHeader.setBackgroundColor(theme.dark)
        binding.headerTitle.setTextColor(onDark)
        binding.btnFontSize.setTextColor(theme.accent)
        binding.btnBack.setColorFilter(onDark)
        binding.btnDots.setColorFilter(onDark)

        binding.editorToolbar.setBackgroundColor(theme.toolbar)
        binding.categoryTabsScroll.setBackgroundColor(theme.dark)
        binding.alphaLetterTabsWrapper.setBackgroundColor(theme.toolbar)

        val icons = listOf(
            binding.iconUndo, binding.iconRedo, binding.iconCursorLeft, binding.iconCursorRight,
            binding.iconSpace, binding.iconClipboard, binding.iconBackspace, binding.iconClear
        )
        val labels = listOf(
            binding.lblUndo, binding.lblRedo, binding.lblCursorLeft, binding.lblCursorRight,
            binding.lblSpace, binding.lblClipboard, binding.lblBackspace, binding.lblClear
        )
        val iconColor = if (theme.isDark) theme.accent else theme.dark
        icons.forEach { it.setColorFilter(iconColor) }
        labels.forEach { it.setTextColor(iconColor) }

        val toolCards = listOf(
            binding.btnUndo, binding.btnRedo, binding.btnCursorLeft, binding.btnCursorRight,
            binding.btnSpace, binding.btnClipboard, binding.btnBackspace, binding.btnClear
        )
        toolCards.forEach {
            it.background = ThemeManager.roundedCard(theme.cardBg, 0x22000000, 14f)
        }

        val cornerColor = if (theme.isDark) theme.toolbar else theme.primary
        binding.btnCornerSave.background = ThemeManager.roundedCard(cornerColor, 0x22000000, 24f)
        binding.btnCornerCopy.background = ThemeManager.roundedCard(cornerColor, 0x22000000, 24f)
        binding.iconCornerSave.setColorFilter(onCorner)
        binding.lblCornerSave.setTextColor(onCorner)
        binding.iconCornerCopy.setColorFilter(onCorner)
        binding.lblCornerCopy.setTextColor(onCorner)

        binding.coachmarkCard.setCardBackgroundColor(if (theme.isDark) 0xFF1F1A2C.toInt() else theme.dark)

        window.statusBarColor = theme.dark
        window.navigationBarColor = theme.bg

        keyAdapter.setTheme(theme)
        updateTabStyles()
        updateLetterChipStyles()
    }

    private fun setupEditorWatcher() {
        binding.editor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!restoringHistory) {
                    history.push(s?.toString() ?: "")
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Hold editor box to quickly save text
        binding.editor.setOnLongClickListener {
            saveCurrentEditorText()
            false
        }
    }

    private fun setupToolbarListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnFontSize.setOnClickListener {
            val sizes = arrayOf("12", "14", "17", "20", "24", "28", "32")
            AlertDialog.Builder(this)
                .setTitle("Editor Text Size")
                .setItems(sizes) { _, which ->
                    fontSize = sizes[which].toInt()
                    binding.editor.textSize = fontSize.toFloat()
                    binding.btnFontSize.text = "Size: ${fontSize}▼"
                    LocalStore.setEditorFontSize(this, fontSize)
                }
                .show()
        }

        binding.btnDots.setOnClickListener { anchor ->
            val popup = PopupMenu(this, anchor)
            popup.menu.add(0, 1, 0, "Copy Current Tab")
            popup.menu.add(0, 2, 1, "Share Text")
            popup.menu.add(0, 3, 2, "Show Quick Guide")
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> showBulkCopyDialog()
                    2 -> {
                        val txt = binding.editor.text.toString()
                        if (txt.isNotBlank()) LocalStore.shareText(this, txt)
                        else Toast.makeText(this, "Text box is empty", Toast.LENGTH_SHORT).show()
                    }
                    3 -> showGuideStep(0)
                }
                true
            }
            popup.show()
        }

        binding.btnCornerSave.setOnClickListener {
            saveCurrentEditorText()
        }

        binding.btnCornerCopy.setOnClickListener {
            val txt = getSelectedOrAllText()
            if (txt.isNotEmpty()) {
                LocalStore.copyAndTrack(this, txt, "Copied to clipboard!")
            } else {
                copyActiveTabItems(spaced = false)
            }
        }
        binding.btnCornerCopy.setOnLongClickListener {
            showBulkCopyDialog()
            true
        }

        binding.btnUndo.setOnClickListener {
            val prev = history.undo()
            if (prev != null) {
                restoringHistory = true
                binding.editor.setText(prev)
                binding.editor.setSelection(prev.length)
                restoringHistory = false
            }
        }

        binding.btnRedo.setOnClickListener {
            val next = history.redo()
            if (next != null) {
                restoringHistory = true
                binding.editor.setText(next)
                binding.editor.setSelection(next.length)
                restoringHistory = false
            }
        }

        binding.btnCursorLeft.setOnClickListener {
            binding.editor.requestFocus()
            val pos = binding.editor.selectionStart.coerceAtLeast(0)
            if (pos > 0) {
                val prev = Character.offsetByCodePoints(binding.editor.text, pos, -1)
                binding.editor.setSelection(prev)
            }
        }

        binding.btnCursorRight.setOnClickListener {
            binding.editor.requestFocus()
            val pos = binding.editor.selectionEnd.coerceAtLeast(0)
            val len = binding.editor.text.length
            if (pos < len) {
                val next = Character.offsetByCodePoints(binding.editor.text, pos, 1)
                binding.editor.setSelection(next)
            }
        }

        binding.btnSpace.setOnClickListener { insertAtCursor(" ") }

        binding.btnClipboard.setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = cm.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val pasted = clip.getItemAt(0).coerceToText(this).toString()
                insertAtCursor(pasted)
            }
        }

        binding.btnBackspace.setOnClickListener {
            val start = binding.editor.selectionStart.coerceAtLeast(0)
            val end = binding.editor.selectionEnd.coerceAtLeast(0)
            val ed = binding.editor.text
            if (start != end) {
                ed.delete(minOf(start, end), maxOf(start, end))
            } else if (start > 0) {
                val prevIndex = Character.offsetByCodePoints(ed, start, -1)
                ed.delete(prevIndex, start)
            }
        }

        binding.btnClear.setOnClickListener {
            if (binding.editor.text.isNotEmpty()) {
                binding.editor.setText("")
            }
        }
    }

    private fun saveCurrentEditorText() {
        val txt = binding.editor.text.toString().trim()
        if (txt.isEmpty()) {
            Toast.makeText(this, "Type something to save", Toast.LENGTH_SHORT).show()
            return
        }
        LocalStore.saveText(this, txt)
        if (!LocalStore.isFavorite(this, txt)) {
            LocalStore.toggleFavorite(this, txt)
        }
        Toast.makeText(this, "Saved to Saved tab!", Toast.LENGTH_SHORT).show()
        if (tabs.getOrNull(activeTabIndex)?.id == "SAVED") {
            renderActiveTab()
        }
    }

    private fun setupGuideOverlay() {
        binding.btnGuideSkip.setOnClickListener { dismissGuide() }
        binding.btnGuideNext.setOnClickListener {
            if (currentGuideStep + 1 < guideSteps.size) {
                showGuideStep(currentGuideStep + 1)
            } else {
                dismissGuide()
            }
        }
    }

    private fun maybeShowFirstTimeGuide() {
        val prefs = getSharedPreferences(PREF_GUIDE, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(KEY_GUIDE_SHOWN, false)) {
            showGuideStep(0)
        }
    }

    private fun showGuideStep(index: Int) {
        currentGuideStep = index
        val step = guideSteps[index]
        binding.tvGuideTitle.text = step.title
        binding.tvGuideBody.text = step.body
        binding.tvGuideStep.text = "${index + 1} / ${guideSteps.size}"
        binding.btnGuideNext.text = if (index == guideSteps.lastIndex) "Got it" else "Next"

        val card = binding.coachmarkCard
        if (card.visibility != View.VISIBLE) {
            card.alpha = 0f
            card.translationY = 48f
            card.visibility = View.VISIBLE
            card.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(320L)
                .setInterpolator(OvershootInterpolator(1.1f))
                .start()
        } else {
            card.alpha = 0.7f
            card.animate().alpha(1f).setDuration(180L).start()
        }
    }

    private fun dismissGuide() {
        getSharedPreferences(PREF_GUIDE, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_GUIDE_SHOWN, true).apply()
        binding.coachmarkCard.animate()
            .alpha(0f)
            .translationY(40f)
            .setDuration(220L)
            .withEndAction { binding.coachmarkCard.visibility = View.GONE }
            .start()
    }

    private fun buildTabs() {
        tabs.clear()
        tabs.add(KeyboardTab("ALPHABET", "ALPHABET"))
        tabs.add(KeyboardTab("cat_combining", "COMBINING"))
        tabs.add(KeyboardTab("cat_templates", "STYLES", isList = true))
        tabs.add(KeyboardTab("INTRO", "INTRO", isList = true))
        tabs.add(KeyboardTab("OUTRO", "OUTRO", isList = true))
        tabs.add(KeyboardTab("cat_urdu", "URDU", isList = true))
        tabs.add(KeyboardTab("cat_decorations", "DECORATIONS", isList = true))
        tabs.add(KeyboardTab("cat_little_abc", "LITTLE ABC"))
        tabs.add(KeyboardTab("cat_stars", "STARS"))
        tabs.add(KeyboardTab("cat_sets", "SETS"))
        tabs.add(KeyboardTab("RECENT", "RECENT"))
        tabs.add(KeyboardTab("SAVED", "SAVED", isList = true))

        val container = binding.categoryTabsContainer
        container.removeAllViews()
        tabViews.clear()
        val density = resources.displayMetrics.density

        tabs.forEachIndexed { index, tab ->
            val tv = TextView(this).apply {
                text = tab.label
                textSize = 11.5f
                gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD
                setPadding((14 * density).toInt(), (6 * density).toInt(), (14 * density).toInt(), (6 * density).toInt())
                setOnClickListener { selectTab(index) }
                setOnLongClickListener {
                    selectTab(index)
                    copyActiveTabItems(spaced = false)
                    true
                }
            }
            val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, (32 * density).toInt())
            lp.marginEnd = (6 * density).toInt()
            container.addView(tv, lp)
            tabViews.add(tv)
        }
        updateTabStyles()
    }

    private fun buildLetterChips() {
        val container = binding.alphaLetterTabsContainer
        container.removeAllViews()
        letterViews.clear()
        val density = resources.displayMetrics.density

        for (c in 'A'..'Z') {
            val letter = c.toString()
            val tv = LayoutInflater.from(this).inflate(R.layout.item_alpha_tab, container, false) as TextView
            tv.text = letter
            tv.setOnClickListener {
                activeLetter = letter
                updateLetterChipStyles()
                renderActiveTab()
            }
            val lp = LinearLayout.LayoutParams((36 * density).toInt(), (32 * density).toInt())
            lp.marginEnd = (4 * density).toInt()
            container.addView(tv, lp)
            letterViews.add(tv)
        }
        updateLetterChipStyles()
    }

    private fun selectTab(index: Int) {
        if (index !in tabs.indices) return
        activeTabIndex = index
        updateTabStyles()
        renderActiveTab()
    }

    private fun updateTabStyles() {
        val theme = ThemeManager.getCurrent(this)
        tabViews.forEachIndexed { idx, tv ->
            val selected = idx == activeTabIndex
            val fill = if (selected) theme.accent else theme.dark
            val stroke = if (selected) theme.accent else 0x44FFFFFF
            tv.background = ThemeManager.roundedCard(fill, stroke, 20f)
            tv.setTextColor(ThemeManager.contrastOn(fill))
        }
    }

    private fun updateLetterChipStyles() {
        val theme = ThemeManager.getCurrent(this)
        letterViews.forEach { tv ->
            val selected = tv.text.toString() == activeLetter
            val fill = if (selected) theme.dark else theme.cardBg
            tv.background = ThemeManager.roundedCard(fill, 0x22000000, 16f)
            tv.setTextColor(ThemeManager.contrastOn(fill))
        }
    }

    private fun getActiveTabItems(): List<String> {
        val tab = tabs.getOrNull(activeTabIndex) ?: return emptyList()
        return when {
            tab.id == "ALPHABET" -> alphabet[activeLetter] ?: emptyList()
            tab.id == "RECENT" -> LocalStore.getRecentSymbols(this)
            tab.id == "SAVED" -> {
                (LocalStore.getSavedTexts(this) + LocalStore.getFavorites(this)).distinct()
            }
            tab.id == "INTRO" -> {
                catalogSections.find { it.id == "intro" }?.items ?: emptyList()
            }
            tab.id == "OUTRO" -> {
                catalogSections.find { it.id == "outro" }?.items ?: emptyList()
            }
            tab.id.startsWith("cat_") -> {
                val secId = tab.id.removePrefix("cat_")
                catalogSections.find { it.id == secId }?.items ?: emptyList()
            }
            else -> emptyList()
        }
    }

    private fun copyActiveTabItems(spaced: Boolean) {
        val tab = tabs.getOrNull(activeTabIndex) ?: return
        val items = getActiveTabItems()
        if (items.isEmpty()) {
            Toast.makeText(this, "Current tab is empty", Toast.LENGTH_SHORT).show()
            return
        }
        val sep = if (spaced) " " else "\n"
        val sb = StringBuilder()
        var count = 0
        for (item in items) {
            if (sb.length + item.length + sep.length > 350_000) break
            if (sb.isNotEmpty()) sb.append(sep)
            sb.append(item)
            count++
        }
        LocalStore.copyAndTrack(this, sb.toString(), "Copied $count items from ${tab.label}!")
    }

    private fun showBulkCopyDialog() {
        val tab = tabs.getOrNull(activeTabIndex) ?: return
        val items = getActiveTabItems()
        val options = arrayOf(
            "Copy Current Tab - Lines (${items.size} items)",
            "Copy Current Tab - Spaced (${items.size} items)",
            "Insert Tab Items into Editor"
        )
        AlertDialog.Builder(this)
            .setTitle("Copy Tab (${tab.label})")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> copyActiveTabItems(spaced = false)
                    1 -> copyActiveTabItems(spaced = true)
                    2 -> if (items.isNotEmpty()) insertAtCursor(items.take(200).joinToString("\n"))
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun renderActiveTab() {
        val tab = tabs.getOrNull(activeTabIndex) ?: return
        binding.alphaLetterTabsWrapper.visibility = if (tab.id == "ALPHABET") View.VISIBLE else View.GONE

        val items = getActiveTabItems()
        val lm = if (tab.isList) {
            LinearLayoutManager(this)
        } else {
            GridLayoutManager(this, 5)
        }
        binding.keyboardRecyclerView.layoutManager = lm
        keyAdapter.submitKeys(items, tab.isList)
    }

    private fun onKeyTapped(value: String) {
        val tab = tabs.getOrNull(activeTabIndex)
        if (tab?.id == "cat_templates" && value.contains("Name")) {
            val cur = binding.editor.text.toString().trim().ifEmpty { LocalStore.getUserName(this) }
            val result = value.replace("Name", cur)
            binding.editor.setText(result)
            binding.editor.setSelection(result.length)
            return
        }
        insertAtCursor(value)
        if (tab?.id != "SAVED" && tab?.id != "RECENT") {
            LocalStore.addRecentSymbol(this, value)
        }
    }

    private fun onKeyLongPressed(value: String) {
        val tab = tabs.getOrNull(activeTabIndex)
        if (tab?.id == "SAVED") {
            val options = arrayOf("Copy", "Delete from Saved")
            AlertDialog.Builder(this)
                .setTitle("Saved Item")
                .setMessage(value)
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> LocalStore.copyAndTrack(this, value)
                        1 -> {
                            LocalStore.deleteSavedText(this, value)
                            if (LocalStore.isFavorite(this, value)) {
                                LocalStore.toggleFavorite(this, value)
                            }
                            renderActiveTab()
                            Toast.makeText(this, "Removed from Saved", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            val options = arrayOf("Save to Saved Tab", "Copy Symbol")
            AlertDialog.Builder(this)
                .setTitle(value)
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            LocalStore.saveText(this, value)
                            Toast.makeText(this, "Saved to Saved tab!", Toast.LENGTH_SHORT).show()
                        }
                        1 -> LocalStore.copyAndTrack(this, value)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun insertAtCursor(text: String) {
        val ed = binding.editor.text
        val start = binding.editor.selectionStart.coerceAtLeast(0)
        val end = binding.editor.selectionEnd.coerceAtLeast(0)
        val lo = minOf(start, end)
        val hi = maxOf(start, end)
        ed.replace(lo, hi, text)
    }

    private fun getSelectedOrAllText(): String {
        val start = binding.editor.selectionStart.coerceAtLeast(0)
        val end = binding.editor.selectionEnd.coerceAtLeast(0)
        return if (start != end) {
            binding.editor.text.substring(minOf(start, end), maxOf(start, end))
        } else {
            binding.editor.text.toString()
        }
    }
}
