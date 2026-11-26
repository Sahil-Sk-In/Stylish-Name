package com.sahil.namemaker.engine

import com.sahil.namemaker.data.model.KeyboardFont

object FontTransformer {

    fun transform(text: String, font: KeyboardFont, intro: String = "", outro: String = ""): String {
        if (text.isEmpty()) return ""
        val sb = StringBuilder()
        sb.append(intro)

        for (ch in text) {
            val s = ch.toString()
            val mapped = when {
                ch.isLowerCase() -> font.lower[s] ?: s
                ch.isUpperCase() -> font.upper[s] ?: font.lower[s.lowercase()] ?: s
                else -> s
            }
            sb.append(mapped)
        }

        sb.append(outro)
        return sb.toString()
    }

    fun generateAllStyles(
        text: String,
        fonts: List<KeyboardFont>,
        intro: String = "",
        outro: String = ""
    ): List<Pair<String, String>> {
        val query = if (text.isBlank()) "Stylish Name" else text
        val results = mutableListOf<Pair<String, String>>()

        // 1. JSON-defined keyboards (K 1 .. K 25)
        for (font in fonts) {
            val transformed = transform(query, font, intro, outro)
            results.add(Pair(font.name, transformed))
        }

        // 2. Built-in Special VIP Nickname Styles
        val vipDecorations = listOf(
            "★彡 VIP Style 彡★" to ("★彡[" + query + "]彡★"),
            "亗 FreeFire Pro 亗" to ("亗『" + query + "』亗"),
            "꧁༒☬ Legend ☬༒꧂" to ("꧁༒☬" + query + "☬༒꧂"),
            "༺ Aesthetic ༻" to ("༺" + query + "༻"),
            "⚡ Thunder ⚡" to ("⚡" + query + "⚡"),
            "✿ Cute Flower ✿" to ("✿ " + query + " ✿"),
            "𝄟 Royalty 𝄟" to ("𝄟 " + query + " 𝄟"),
            "༒ Bad Boy ༒" to ("༒" + query + "༒")
        )
        for ((name, decor) in vipDecorations) {
            results.add(Pair(name, intro + decor + outro))
        }

        return results
    }
}
