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
        S2Theme("Salaam Pink", 0xFFE898A8.toInt(), 0xFF1F1A2C.toInt(), 0xFFC9A9A6.toInt(), 0xFFFAF0F0.toInt(), 0xFF1F1A2C.toInt(), 0xFFFFFFFF.toInt(), 0xFFE898A8.toInt(), 0xFFFFFFFF.toInt(), false),
        S2Theme("Day Light", 0xFFE3ECFF.toInt(), 0xFF1E3A8A.toInt(), 0xFFBFD4FF.toInt(), 0xFFF8FAFF.toInt(), 0xFF1E3A8A.toInt(), 0xFFFFFFFF.toInt(), 0xFF3B82F6.toInt(), 0xFFFFFFFF.toInt(), false),
        S2Theme("Night Dark", 0xFF1E1E2E.toInt(), 0xFF11111B.toInt(), 0xFF313244.toInt(), 0xFF181825.toInt(), 0xFFCDD6F4.toInt(), 0xFFCDD6F4.toInt(), 0xFFCBA6F7.toInt(), 0xFF313244.toInt(), true),
        S2Theme("Forest Green", 0xFFD4EDDA.toInt(), 0xFF1B5E20.toInt(), 0xFFA5D6A7.toInt(), 0xFFE8F5E9.toInt(), 0xFF1B5E20.toInt(), 0xFFFFFFFF.toInt(), 0xFF4CAF50.toInt(), 0xFFFFFFFF.toInt(), false),
        S2Theme("Ocean Blue", 0xFFB2EBF2.toInt(), 0xFF006064.toInt(), 0xFF80DEEA.toInt(), 0xFFE0F7FA.toInt(), 0xFF006064.toInt(), 0xFFFFFFFF.toInt(), 0xFF00BCD4.toInt(), 0xFFFFFFFF.toInt(), false),
        S2Theme("Sunset Orange", 0xFFFFE0B2.toInt(), 0xFFBF360C.toInt(), 0xFFFFCC80.toInt(), 0xFFFFF3E0.toInt(), 0xFFBF360C.toInt(), 0xFFFFFFFF.toInt(), 0xFFFF9800.toInt(), 0xFFFFFFFF.toInt(), false),
        S2Theme("Royal Purple", 0xFFE1BEE7.toInt(), 0xFF4A148C.toInt(), 0xFFCE93D8.toInt(), 0xFFF3E5F5.toInt(), 0xFF4A148C.toInt(), 0xFFFFFFFF.toInt(), 0xFF9C27B0.toInt(), 0xFFFFFFFF.toInt(), false),
        S2Theme("Midnight Blue", 0xFF0D1B2A.toInt(), 0xFF0A1628.toInt(), 0xFF1B2838.toInt(), 0xFF0D1B2A.toInt(), 0xFFE0E6ED.toInt(), 0xFFE0E6ED.toInt(), 0xFF48CAE4.toInt(), 0xFF1B2838.toInt(), true),
        S2Theme("Cherry Red", 0xFFFFCDD2.toInt(), 0xFFB71C1C.toInt(), 0xFFEF9A9A.toInt(), 0xFFFFEBEE.toInt(), 0xFFB71C1C.toInt(), 0xFFFFFFFF.toInt(), 0xFFF44336.toInt(), 0xFFFFFFFF.toInt(), false),
        S2Theme("AMOLED Black", 0xFF000000.toInt(), 0xFF000000.toInt(), 0xFF1A1A1A.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFFE0E0E0.toInt(), 0xFFBB86FC.toInt(), 0xFF1A1A1A.toInt(), true),
        S2Theme("Crimson Black", 0xFF0A0A0A.toInt(), 0xFF000000.toInt(), 0xFF141414.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFFE0E0E0.toInt(), 0xFFE94B5B.toInt(), 0xFF141414.toInt(), true),
        S2Theme("Teal Black", 0xFF0A0A0A.toInt(), 0xFF000000.toInt(), 0xFF101010.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt(), 0xFFD0D0D0.toInt(), 0xFF1FB6B6.toInt(), 0xFF101010.toInt(), true)
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
