package com.sahil.namemaker.utils

import android.content.Context
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.os.Build

object FontUtils {
    private var cachedSymbolTypeface: Typeface? = null

    fun loadSymbolTypeface(context: Context): Typeface {
        cachedSymbolTypeface?.let { return it }
        val fontFiles = listOf(
            "fonts/NotoSans-Regular.ttf",
            "fonts/NotoSansMath-Regular.ttf",
            "fonts/NotoSansSymbols-Regular.ttf",
            "fonts/NotoSansSymbols2-Regular.ttf",
            "fonts/Noto_Sans_Balinese.ttf",
            "fonts/Noto_Sans_Runic.ttf",
            "fonts/Noto_Sans_Ethiopic.ttf"
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                var builder: Typeface.CustomFallbackBuilder? = null
                for (path in fontFiles) {
                    try {
                        val font = Font.Builder(context.assets, path).build()
                        val family = FontFamily.Builder(font).build()
                        if (builder == null) {
                            builder = Typeface.CustomFallbackBuilder(family)
                        } else {
                            builder.addCustomFallback(family)
                        }
                    } catch (_: Exception) {
                    }
                }
                if (builder != null) {
                    val tf = builder.setSystemFallback("sans-serif").build()
                    cachedSymbolTypeface = tf
                    return tf
                }
            } catch (_: Exception) {
            }
        }

        val fallback = try {
            Typeface.createFromAsset(context.assets, "fonts/NotoSansSymbols2-Regular.ttf")
        } catch (_: Exception) {
            Typeface.DEFAULT
        }
        cachedSymbolTypeface = fallback
        return fallback
    }
}
