package com.sahil.namemaker.data.model

import com.google.gson.annotations.SerializedName

data class StylishPost(
    val id: String = "",
    val text: String = "",
    val category: String = "#Random",
    val keywords: String = "",
    val createdAt: String = "",
    val authorName: String = "Sahil"
)

data class Decoration(
    val id: String = "",
    val text: String = "",
    val type: String = "intro" // "intro", "outro", etc.
)

data class KeyboardFont(
    val id: String = "",
    val name: String = "",
    val lower: Map<String, String> = emptyMap(),
    val upper: Map<String, String> = emptyMap()
)

data class SymbolCategory(
    val id: String = "",
    val name: String = "",
    val items: List<String> = emptyList(),
    val preview: String = "",
    val kind: String = ""
)
