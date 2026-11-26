package com.sahil.namemaker.utils

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable

data class S2Theme(
    val name: String,
    val primary: Int,
    val dark: Int,
    val toolbar: Int,
    val bg: Int,
    val textDark: Int,
    val textLight: Int,
    val accent: Int,
    val cardBg: Int,
    val isDark: Boolean
)

object ThemeManager {
    private const val PREFS = "ThemePrefs"
    private const val KEY_INDEX = "theme_index"

    val THEMES: List<S2Theme> = listOf(
        S2Theme("Royal Purple", 0xFFE1BEE7.toInt(), 0xFF4A148C.toInt(), 0xFFCE93D8.toInt(), 0xFFF3E5F5.toInt(), 0xFF4A148C.toInt(), 0xFFFFFFFF.toInt(), 0xFF9C27B0.toInt(), 0xFFFFFFFF.toInt(), false),
        S2Theme("AMOLED Black", 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF1A1A1A.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFFE0E0E0.toInt(), 0xFFBB86FC.toInt(), 0xFF1A1A1A.toInt(), true)
    )

    fun getCurrentIndex(context: Context): Int {
        val idx = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(KEY_INDEX, 0)
        return if (idx in THEMES.indices) idx else 0
    }

    fun getCurrent(context: Context): S2Theme = THEMES[getCurrentIndex(context)]

    fun setIndex(context: Context, index: Int) {
        val safe = if (index in THEMES.indices) index else 0
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_INDEX, safe).apply()
    }

    fun getThemeNames(): Array<String> = THEMES.map { it.name }.toTypedArray()

    fun contrastOn(color: Int): Int {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val lum = 0.2126 * r + 0.7152 * g + 0.0722 * b
        return if (lum < 140.0) Color.WHITE else 0xFF1A1A2E.toInt()
    }

    fun roundedCard(fill: Int, stroke: Int, radiusPx: Float): GradientDrawable {
        return GradientDrawable().apply {
            setColor(fill)
            cornerRadius = radiusPx
            setStroke(2, stroke)
        }
    }
}
