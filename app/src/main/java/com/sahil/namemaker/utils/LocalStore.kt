package com.sahil.namemaker.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import org.json.JSONArray

object LocalStore {
    private const val PREF_USER = "UserPrefs"
    private const val PREF_STORE = "NameMakerStore"
    private const val KEY_USER_NAME = "userName"
    private const val KEY_FAVORITES = "favorites_json"
    private const val KEY_RECENT = "recent_json"
    private const val KEY_RECENT_SYMBOLS = "recent_symbols_json"
    private const val KEY_SAVED_TEXTS = "saved_texts_json"
    private const val KEY_EDITOR_TEXT = "editor_text"
    private const val KEY_EDITOR_SIZE = "editor_font_size"
    private const val MAX_RECENT = 200
    private const val MAX_RECENT_SYMBOLS = 60

    fun getUserName(context: Context): String {
        val name = context.getSharedPreferences(PREF_USER, Context.MODE_PRIVATE)
            .getString(KEY_USER_NAME, "Sahil")
        return if (name.isNullOrBlank()) "Sahil" else name.trim()
    }

    fun setUserName(context: Context, name: String) {
        val safe = if (name.isBlank()) "Sahil" else name.trim()
        context.getSharedPreferences(PREF_USER, Context.MODE_PRIVATE)
            .edit().putString(KEY_USER_NAME, safe).apply()
    }

    fun getFavorites(context: Context): MutableList<String> = readList(context, KEY_FAVORITES)

    fun isFavorite(context: Context, value: String): Boolean = getFavorites(context).contains(value)

    fun toggleFavorite(context: Context, value: String): Boolean {
        val list = getFavorites(context)
        val added = if (list.contains(value)) {
            list.remove(value)
            false
        } else {
            list.add(0, value)
            true
        }
        writeList(context, KEY_FAVORITES, list)
        return added
    }

    fun getRecent(context: Context): MutableList<String> = readList(context, KEY_RECENT)

    fun addRecent(context: Context, value: String) {
        if (value.isBlank()) return
        val list = getRecent(context)
        list.remove(value)
        list.add(0, value)
        if (list.size > MAX_RECENT) {
            list.subList(MAX_RECENT, list.size).clear()
        }
        writeList(context, KEY_RECENT, list)
    }

    fun getRecentSymbols(context: Context): MutableList<String> = readList(context, KEY_RECENT_SYMBOLS)

    fun addRecentSymbol(context: Context, symbol: String) {
        if (symbol.isEmpty()) return
        val list = getRecentSymbols(context)
        list.remove(symbol)
        list.add(0, symbol)
        if (list.size > MAX_RECENT_SYMBOLS) {
            list.subList(MAX_RECENT_SYMBOLS, list.size).clear()
        }
        writeList(context, KEY_RECENT_SYMBOLS, list)
    }

    fun getSavedTexts(context: Context): MutableList<String> = readList(context, KEY_SAVED_TEXTS)

    fun saveText(context: Context, text: String) {
        if (text.isBlank()) return
        val list = getSavedTexts(context)
        list.remove(text)
        list.add(0, text)
        writeList(context, KEY_SAVED_TEXTS, list)
    }

    fun deleteSavedText(context: Context, text: String) {
        val list = getSavedTexts(context)
        list.remove(text)
        writeList(context, KEY_SAVED_TEXTS, list)
    }

    fun getEditorText(context: Context): String {
        return context.getSharedPreferences(PREF_STORE, Context.MODE_PRIVATE)
            .getString(KEY_EDITOR_TEXT, "") ?: ""
    }

    fun setEditorText(context: Context, text: String) {
        context.getSharedPreferences(PREF_STORE, Context.MODE_PRIVATE)
            .edit().putString(KEY_EDITOR_TEXT, text).apply()
    }

    fun getEditorFontSize(context: Context): Int {
        return context.getSharedPreferences(PREF_STORE, Context.MODE_PRIVATE)
            .getInt(KEY_EDITOR_SIZE, 17)
    }

    fun setEditorFontSize(context: Context, size: Int) {
        context.getSharedPreferences(PREF_STORE, Context.MODE_PRIVATE)
            .edit().putInt(KEY_EDITOR_SIZE, size).apply()
    }

    fun copyAndTrack(context: Context, text: String, toastMessage: String = "Copied to clipboard") {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("NameMaker", text))
        addRecent(context, text)
        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
    }

    fun shareText(context: Context, text: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            context.startActivity(Intent.createChooser(intent, "Share Style"))
        } catch (_: Exception) {
        }
    }

    private fun readList(context: Context, key: String): MutableList<String> {
        val raw = context.getSharedPreferences(PREF_STORE, Context.MODE_PRIVATE).getString(key, null)
            ?: return mutableListOf()
        val result = mutableListOf<String>()
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val item = arr.optString(i, "")
                if (item.isNotEmpty()) result.add(item)
            }
        } catch (_: Exception) {
        }
        return result
    }

    private fun writeList(context: Context, key: String, items: List<String>) {
        val arr = JSONArray()
        items.forEach { arr.put(it) }
        context.getSharedPreferences(PREF_STORE, Context.MODE_PRIVATE)
            .edit().putString(key, arr.toString()).apply()
    }
}
