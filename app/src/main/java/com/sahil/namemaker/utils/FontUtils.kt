package com.sahil.namemaker.utils

import android.content.Context
import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FontUtils {
    @Volatile
    private var cachedSymbolTypeface: Typeface? = null
    private val lock = Any()

    suspend fun loadSymbolTypefaceAsync(context: Context): Typeface = withContext(Dispatchers.IO) {
        loadSymbolTypeface(context.applicationContext)
    }

    fun loadSymbolTypeface(context: Context): Typeface {
        cachedSymbolTypeface?.let { return it }
        synchronized(lock) {
            cachedSymbolTypeface?.let { return it }
            val appCtx = context.applicationContext
            val fontFiles = listOf(
                "fonts/NotoSans-Regular.ttf",
                "fonts/NotoSansMath-Regular.ttf",
                "fonts/Noto_Sans_Math.ttf",
                "fonts/NotoSansSymbols-Regular.ttf",
                "fonts/NotoSansSymbols2-Regular.ttf",
                "fonts/Noto_Sans_Cherokee.ttf",
                "fonts/Noto_Sans_Canadian_Aboriginal.ttf",
                "fonts/Noto_Sans_Tai_Viet.ttf",
                "fonts/Noto_Sans_Miao.ttf",
                "fonts/Noto_Sans_Vai.ttf",
                "fonts/Noto_Sans_Bamum.ttf",
                "fonts/Noto_Sans_Tifinagh.ttf",
                "fonts/Noto_Sans_Balinese.ttf",
                "fonts/Noto_Sans_Runic.ttf",
                "fonts/Noto_Sans_Ethiopic.ttf"
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    var builder: Typeface.CustomFallbackBuilder? = null
                    for (path in fontFiles) {
                        try {
                            val font = Font.Builder(appCtx.assets, path).build()
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
                Typeface.createFromAsset(appCtx.assets, "fonts/NotoSansSymbols2-Regular.ttf")
            } catch (_: Exception) {
                Typeface.DEFAULT
            }
            cachedSymbolTypeface = fallback
            return fallback
        }
    }
}
