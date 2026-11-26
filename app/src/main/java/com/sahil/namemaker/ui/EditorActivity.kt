package com.sahil.namemaker.ui

import android.app.AlertDialog
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
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
import com.sahil.namemaker.data.CustomCategory
import com.sahil.namemaker.data.OfflineRepository
import com.sahil.namemaker.databinding.ActivityEditorBinding
import com.sahil.namemaker.utils.FontUtils
import com.sahil.namemaker.utils.LocalStore
import com.sahil.namemaker.utils.TextHistory
import com.sahil.namemaker.utils.ThemeManager
import kotlinx.coroutines.launch

class EditorActivity : AppCompatActivity() {

    private data class KeyboardTab(
        val id: String,
        val label: String,
        val isList: Boolean = false
    )

    private lateinit var binding: ActivityEditorBinding
    private lateinit var symbolTypeface: Typeface
    private lateinit var keyAdapter: KeyboardKeyAdapter
    private val history = TextHistory()

    private var catalogSections: List<CatalogSection> = emptyList()
    private var alphabet: Map<String, List<String>> = emptyMap()
    private var customCategories: List<CustomCategory> = emptyList()

    private val tabs = mutableListOf<KeyboardTab>()
    private val tabViews = mutableListOf<TextView>()
    private val letterViews = mutableListOf<TextView>()

    private var activeTabIndex = 0
    private var activeLetter = "A"
    private var fontSize = 17
    private var alignIndex = 0
    private var restoringHistory = false
    private var imeVisible = false
    private var searchCursor = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        symbolTypeface = FontUtils.loadSymbolTypeface(this)
        binding.editor.typeface = symbolTypeface

        fontSize = LocalStore.getEditorFontSize(this)
        binding.editor.textSize = fontSize.toFloat()
        binding.btnFontSize.text = "${fontSize}▼"

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
        setupFindReplace()
        setupSearchInput()
        buildLetterChips()
        applyTheme()

        lifecycleScope.launch {
            catalogSections = OfflineRepository.getCatalog(this@EditorActivity)
            alphabet = OfflineRepository.getAlphabet(this@EditorActivity)
            customCategories = OfflineRepository.getCustomCategories(this@EditorActivity)
            OfflineRepository.loadCrunchData(this@EditorActivity)
            buildTabs()
            selectTab(0)
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

        binding.editorRoot.setBackgroundColor(theme.bg)
        binding.editorHeader.setBackgroundColor(theme.dark)
        binding.headerTitle.setTextColor(theme.accent)
        binding.btnFontSize.setTextColor(theme.accent)
        binding.btnBack.setColorFilter(onDark)
        binding.btnDots.setColorFilter(onDark)

        binding.editorToolbar.setBackgroundColor(theme.toolbar)
        binding.findReplacePanel.setBackgroundColor(theme.toolbar)
        binding.categoryTabsScroll.setBackgroundColor(theme.dark)
        binding.alphaLetterTabsWrapper.setBackgroundColor(theme.toolbar)

        val toolbarButtons = listOf(
            binding.btnUndo, binding.btnRedo, binding.btnClipboard, binding.btnKeyboard,
            binding.btnCursorStart, binding.btnCursorUp, binding.btnCursorEnd, binding.btnSearch,
            binding.btnInfo, binding.btnClear, binding.btnSelectAll, binding.btnAlign,
            binding.btnCut, binding.btnCursorLeft, binding.btnCursorDown, binding.btnCursorRight,
            binding.btnNewLine, binding.btnBackspace, binding.btnCopy, binding.btnSpace
        )
        toolbarButtons.forEach { it.setColorFilter(onToolbar) }

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
    }

    private fun setupToolbarListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnFontSize.setOnClickListener {
            val sizes = arrayOf("12", "14", "17", "20", "24", "28", "32")
            AlertDialog.Builder(this)
                .setTitle("Editor Font Size")
                .setItems(sizes) { _, which ->
                    fontSize = sizes[which].toInt()
                    binding.editor.textSize = fontSize.toFloat()
                    binding.btnFontSize.text = "${fontSize}▼"
                    LocalStore.setEditorFontSize(this, fontSize)
                }
                .show()
        }

        binding.btnDots.setOnClickListener { anchor ->
            val popup = PopupMenu(this, anchor)
            popup.menu.add(0, 1, 0, "Save to Saved Texts")
            popup.menu.add(0, 2, 1, "Share Text")
            popup.menu.add(0, 3, 2, "Open Make Name")
            popup.menu.add(0, 4, 3, "Switch Theme")
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> {
                        val txt = binding.editor.text.toString()
                        if (txt.isNotBlank()) {
                            LocalStore.saveText(this, txt)
                            Toast.makeText(this, "Saved to Saved Texts", Toast.LENGTH_SHORT).show()
                        }
                    }
                    2 -> {
                        val txt = binding.editor.text.toString()
                        if (txt.isNotBlank()) LocalStore.shareText(this, txt)
                    }
                    3 -> {
                        startActivity(Intent(this, MakeActivity::class.java))
                    }
                    4 -> showThemePicker()
                }
                true
            }
            popup.show()
        }

        val copyAction = View.OnClickListener {
            val txt = getSelectedOrAllText()
            if (txt.isNotEmpty()) {
                LocalStore.copyAndTrack(this, txt)
            } else {
                copyActiveTabItems(spaced = false)
            }
        }
        val copyLongAction = View.OnLongClickListener {
            showBulkCopyDialog()
            true
        }
        binding.btnCornerCopy.setOnClickListener(copyAction)
        binding.btnCopy.setOnClickListener(copyAction)
        binding.btnCornerCopy.setOnLongClickListener(copyLongAction)
        binding.btnCopy.setOnLongClickListener(copyLongAction)

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

        binding.btnClipboard.setOnClickListener {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = cm.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val pasted = clip.getItemAt(0).coerceToText(this).toString()
                insertAtCursor(pasted)
            }
        }

        binding.btnKeyboard.setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            if (imeVisible) {
                imm.hideSoftInputFromWindow(binding.editor.windowToken, 0)
                imeVisible = false
            } else {
                binding.editor.requestFocus()
                imm.showSoftInput(binding.editor, InputMethodManager.SHOW_IMPLICIT)
                imeVisible = true
            }
        }

        binding.btnCursorStart.setOnClickListener { binding.editor.setSelection(0) }
        binding.btnCursorEnd.setOnClickListener { binding.editor.setSelection(binding.editor.text.length) }
        binding.btnCursorLeft.setOnClickListener {
            val pos = binding.editor.selectionStart
            if (pos > 0) binding.editor.setSelection(pos - 1)
        }
        binding.btnCursorRight.setOnClickListener {
            val pos = binding.editor.selectionEnd
            if (pos < binding.editor.text.length) binding.editor.setSelection(pos + 1)
        }
        binding.btnCursorUp.setOnClickListener {
            binding.editor.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP))
        }
        binding.btnCursorDown.setOnClickListener {
            binding.editor.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN))
        }

        binding.btnNewLine.setOnClickListener { insertAtCursor("\n") }
        binding.btnSpace.setOnClickListener { insertAtCursor(" ") }
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

        binding.btnCut.setOnClickListener {
            val start = binding.editor.selectionStart.coerceAtLeast(0)
            val end = binding.editor.selectionEnd.coerceAtLeast(0)
            if (start != end) {
                val lo = minOf(start, end)
                val hi = maxOf(start, end)
                val selected = binding.editor.text.substring(lo, hi)
                LocalStore.copyAndTrack(this, selected, "Cut to clipboard")
                binding.editor.text.delete(lo, hi)
            } else if (binding.editor.text.isNotEmpty()) {
                LocalStore.copyAndTrack(this, binding.editor.text.toString(), "Cut all to clipboard")
                binding.editor.setText("")
            }
        }

        binding.btnSelectAll.setOnClickListener { binding.editor.selectAll() }

        binding.btnAlign.setOnClickListener {
            alignIndex = (alignIndex + 1) % 3
            binding.editor.gravity = when (alignIndex) {
                1 -> Gravity.CENTER_HORIZONTAL or Gravity.TOP
                2 -> Gravity.END or Gravity.TOP
                else -> Gravity.START or Gravity.TOP
            }
        }

        binding.btnSearch.setOnClickListener {
            binding.findReplacePanel.visibility =
                if (binding.findReplacePanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        binding.btnInfo.setOnClickListener {
            val txt = binding.editor.text.toString()
            val lines = if (txt.isEmpty()) 0 else txt.lines().size
            val words = if (txt.isBlank()) 0 else txt.trim().split(Regex("\\s+")).size
            val codePoints = txt.codePointCount(0, txt.length)
            AlertDialog.Builder(this)
                .setTitle("Text Statistics")
                .setMessage("Characters: ${txt.length}\nUnicode Codepoints: $codePoints\nWords: $words\nLines: $lines")
                .setPositiveButton("OK", null)
                .show()
        }

        binding.btnClear.setOnClickListener {
            if (binding.editor.text.isNotEmpty()) {
                binding.editor.setText("")
            }
        }
    }

    private fun setupFindReplace() {
        binding.btnFindNext.setOnClickListener { findOccurrence(forward = true) }
        binding.btnFindPrev.setOnClickListener { findOccurrence(forward = false) }
        binding.btnReplace.setOnClickListener {
            val q = binding.findInput.text.toString()
            val rep = binding.replaceInput.text.toString()
            if (q.isEmpty()) return@setOnClickListener
            val start = binding.editor.selectionStart.coerceAtLeast(0)
            val end = binding.editor.selectionEnd.coerceAtLeast(0)
            val selected = if (start != end) binding.editor.text.substring(minOf(start, end), maxOf(start, end)) else ""
            if (selected == q) {
                binding.editor.text.replace(minOf(start, end), maxOf(start, end), rep)
            }
            findOccurrence(forward = true)
        }
        binding.btnReplaceAll.setOnClickListener {
            val q = binding.findInput.text.toString()
            val rep = binding.replaceInput.text.toString()
            if (q.isEmpty()) return@setOnClickListener
            val updated = binding.editor.text.toString().replace(q, rep)
            binding.editor.setText(updated)
            Toast.makeText(this, "Replaced all occurrences", Toast.LENGTH_SHORT).show()
        }
    }

    private fun findOccurrence(forward: Boolean) {
        val q = binding.findInput.text.toString()
        val text = binding.editor.text.toString()
        if (q.isEmpty() || text.isEmpty()) return
        val idx = if (forward) {
            val from = (binding.editor.selectionEnd).coerceIn(0, text.length)
            val found = text.indexOf(q, from)
            if (found >= 0) found else text.indexOf(q, 0)
        } else {
            val from = (binding.editor.selectionStart - 1).coerceAtLeast(0)
            val found = text.lastIndexOf(q, from)
            if (found >= 0) found else text.lastIndexOf(q)
        }
        if (idx >= 0) {
            binding.editor.requestFocus()
            binding.editor.setSelection(idx, idx + q.length)
            searchCursor = idx
        } else {
            Toast.makeText(this, "Not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupSearchInput() {
        binding.symbolSearchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (tabs.getOrNull(activeTabIndex)?.id == "ALL") {
                    renderActiveTab()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun buildTabs() {
        tabs.clear()
        tabs.add(KeyboardTab("ALPHABET", "ALPHABET"))
        tabs.add(KeyboardTab("RECENT", "RECENT"))
        tabs.add(KeyboardTab("SAVED", "SAVED", isList = true))
        tabs.add(KeyboardTab("ALL", "ALL"))
        tabs.add(KeyboardTab("INTRO", "INTRO", isList = true))
        tabs.add(KeyboardTab("OUTRO", "OUTRO", isList = true))

        catalogSections.forEach { sec ->
            val listMode = sec.id in setOf("names", "bios", "templates", "frames", "dividers", "intro", "outro", "urdu", "decorations")
            tabs.add(KeyboardTab("cat_${sec.id}", sec.title.uppercase(), isList = listMode))
        }
        customCategories.forEach { cat ->
            tabs.add(KeyboardTab("custom_${cat.id}", cat.name.uppercase()))
        }

        val container = binding.categoryTabsContainer
        container.removeAllViews()
        tabViews.clear()
        val density = resources.displayMetrics.density

        tabs.forEachIndexed { index, tab ->
            val tv = TextView(this).apply {
                text = tab.label
                textSize = 11f
                setPadding((12 * density).toInt(), (6 * density).toInt(), (12 * density).toInt(), (6 * density).toInt())
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
            tab.id == "SAVED" -> LocalStore.getSavedTexts(this)
            tab.id == "INTRO" -> OfflineRepository.getDecorations().filter { it.type == "intro" }.map { it.text }
            tab.id == "OUTRO" -> OfflineRepository.getDecorations().filter { it.type == "outro" }.map { it.text }
            tab.id == "ALL" -> {
                val q = binding.symbolSearchInput.text.toString().trim()
                val all = catalogSections.flatMap { it.items }
                if (q.isEmpty()) all.take(400) else all.filter { it.contains(q, ignoreCase = true) }.take(400)
            }
            tab.id.startsWith("cat_") -> {
                val secId = tab.id.removePrefix("cat_")
                catalogSections.find { it.id == secId }?.items ?: emptyList()
            }
            tab.id.startsWith("custom_") -> {
                val catId = tab.id.removePrefix("custom_")
                customCategories.find { it.id == catId }?.items ?: emptyList()
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
            .setTitle("Bulk Copy (${tab.label})")
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
        binding.symbolSearchBox.visibility = if (tab.id == "ALL") View.VISIBLE else View.GONE

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
            AlertDialog.Builder(this)
                .setTitle("Delete Saved Text?")
                .setMessage(value)
                .setPositiveButton("Delete") { _, _ ->
                    LocalStore.deleteSavedText(this, value)
                    renderActiveTab()
                }
                .setNegativeButton("Cancel", null)
                .show()
        } else {
            LocalStore.copyAndTrack(this, value)
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

    private fun showThemePicker() {
        AlertDialog.Builder(this)
            .setTitle("Choose Theme")
            .setSingleChoiceItems(ThemeManager.getThemeNames(), ThemeManager.getCurrentIndex(this)) { dialog, which ->
                ThemeManager.setIndex(this, which)
                applyTheme()
                dialog.dismiss()
            }
            .show()
    }
}
