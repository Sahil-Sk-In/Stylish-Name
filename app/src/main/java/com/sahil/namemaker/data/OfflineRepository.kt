package com.sahil.namemaker.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import kotlin.random.Random

data class CatalogSection(
    val id: String,
    val title: String,
    val items: List<String>
)

data class KeyboardFont(
    val id: String,
    val name: String,
    val lower: Map<String, String>,
    val upper: Map<String, String>
)

data class CrunchDecoration(
    val id: String,
    val text: String,
    val type: String
)

data class CustomCategory(
    val id: String,
    val name: String,
    val items: List<String>
)

data class FeedItem(
    val id: String,
    val author: String,
    val text: String,
    val category: String
)

object OfflineRepository {
    private val gson = Gson()

    private var cachedCatalog: List<CatalogSection>? = null
    private var cachedAlphabet: Map<String, List<String>>? = null
    private var cachedCustomCategories: List<CustomCategory>? = null
    private var cachedKeyboards: List<KeyboardFont>? = null
    private var cachedDecorations: List<CrunchDecoration>? = null
    private var cachedCrunchSymbols: List<String>? = null
    private var cachedPosts: List<FeedItem>? = null
    private var cachedMoments: List<FeedItem>? = null

    suspend fun getCatalog(context: Context): List<CatalogSection> = withContext(Dispatchers.IO) {
        cachedCatalog?.let { return@withContext it }
        val result = mutableListOf<CatalogSection>()
        try {
            context.assets.open("catalog.json").use { stream ->
                val root = gson.fromJson(InputStreamReader(stream), JsonObject::class.java)
                val arr = root.getAsJsonArray("sections")
                for (el in arr) {
                    val obj = el.asJsonObject
                    val id = obj.get("id")?.asString ?: ""
                    val title = obj.get("title")?.asString ?: id
                    val items = mutableListOf<String>()
                    val raw = obj.getAsJsonArray("items")
                    if (raw != null) {
                        for (itemEl in raw) {
                            val s = itemEl.asString
                            if (s.isNotEmpty()) items.add(s)
                        }
                    }
                    result.add(CatalogSection(id, title, items))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cachedCatalog = result
        result
    }

    suspend fun getAlphabet(context: Context): Map<String, List<String>> = withContext(Dispatchers.IO) {
        cachedAlphabet?.let { return@withContext it }
        val map = linkedMapOf<String, List<String>>()
        try {
            context.assets.open("alphabet.json").use { stream ->
                val root = gson.fromJson(InputStreamReader(stream), JsonObject::class.java)
                for (key in root.keySet()) {
                    val arr = root.getAsJsonArray(key) ?: continue
                    val list = mutableListOf<String>()
                    for (el in arr) {
                        val s = el.asString
                        if (s.isNotEmpty()) list.add(s)
                    }
                    map[key] = list
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cachedAlphabet = map
        map
    }

    suspend fun getCustomCategories(context: Context): List<CustomCategory> = withContext(Dispatchers.IO) {
        cachedCustomCategories?.let { return@withContext it }
        val list = mutableListOf<CustomCategory>()
        try {
            context.assets.open("custom_categories.json").use { stream ->
                val root = gson.fromJson(InputStreamReader(stream), JsonObject::class.java)
                for (key in root.keySet()) {
                    val obj = root.getAsJsonObject(key) ?: continue
                    val name = obj.get("name")?.asString ?: key
                    val arr = obj.getAsJsonArray("items")
                    val items = mutableListOf<String>()
                    if (arr != null) {
                        for (el in arr) {
                            val s = el.asString
                            if (s.isNotEmpty()) items.add(s)
                        }
                    }
                    if (items.isNotEmpty()) {
                        list.add(CustomCategory(key, name, items))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cachedCustomCategories = list
        list
    }

    suspend fun loadCrunchData(context: Context) = withContext(Dispatchers.IO) {
        if (cachedKeyboards != null && cachedDecorations != null && cachedCrunchSymbols != null) {
            return@withContext
        }
        val keyboards = mutableListOf<KeyboardFont>()
        val decors = mutableListOf<CrunchDecoration>()
        val symbols = mutableListOf<String>()
        try {
            context.assets.open("crunch_data.json").use { stream ->
                val root = gson.fromJson(InputStreamReader(stream), JsonObject::class.java)
                root.getAsJsonArray("keyboards")?.forEach { el ->
                    val obj = el.asJsonObject
                    val lower = mutableMapOf<String, String>()
                    val upper = mutableMapOf<String, String>()
                    obj.getAsJsonObject("lower")?.let { lObj ->
                        for (k in lObj.keySet()) lower[k] = lObj.get(k).asString
                    }
                    obj.getAsJsonObject("upper")?.let { uObj ->
                        for (k in uObj.keySet()) upper[k] = uObj.get(k).asString
                    }
                    keyboards.add(
                        KeyboardFont(
                            id = obj.get("id")?.asString ?: "",
                            name = obj.get("name")?.asString ?: "Style",
                            lower = lower,
                            upper = upper
                        )
                    )
                }
                root.getAsJsonArray("decorations")?.forEach { el ->
                    val obj = el.asJsonObject
                    decors.add(
                        CrunchDecoration(
                            id = obj.get("id")?.asString ?: "",
                            text = obj.get("text")?.asString ?: "",
                            type = obj.get("type")?.asString ?: "intro"
                        )
                    )
                }
                root.getAsJsonArray("symbols")?.forEach { el ->
                    if (el.isJsonObject) {
                        val t = el.asJsonObject.get("text")?.asString
                        if (!t.isNullOrEmpty()) symbols.add(t)
                    } else if (el.isJsonPrimitive) {
                        val t = el.asString
                        if (t.isNotEmpty()) symbols.add(t)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cachedKeyboards = keyboards
        cachedDecorations = decors
        cachedCrunchSymbols = symbols
    }

    fun getKeyboards(): List<KeyboardFont> = cachedKeyboards ?: emptyList()
    fun getDecorations(): List<CrunchDecoration> = cachedDecorations ?: emptyList()
    fun getCrunchSymbols(): List<String> = cachedCrunchSymbols ?: emptyList()

    fun generateCrunchStyles(
        name: String,
        intro: String,
        outro: String,
        mixMarks: Boolean,
        shuffle: Boolean
    ): List<Pair<String, String>> {
        val baseName = name.ifBlank { "Sahil" }
        val fonts = getKeyboards()
        val symbols = getCrunchSymbols()
        val decors = getDecorations()
        val intros = decors.filter { it.type == "intro" }.map { it.text }
        val outros = decors.filter { it.type == "outro" }.map { it.text }

        val results = mutableListOf<Pair<String, String>>()
        for ((idx, font) in fonts.withIndex()) {
            val sb = StringBuilder()
            val effectiveIntro = if (intro.isNotEmpty()) intro else if (shuffle && intros.isNotEmpty()) intros[Random.nextInt(intros.size)] else ""
            val effectiveOutro = if (outro.isNotEmpty()) outro else if (shuffle && outros.isNotEmpty()) outros[Random.nextInt(outros.size)] else ""

            sb.append(effectiveIntro)
            for ((charIdx, ch) in baseName.withIndex()) {
                val s = ch.toString()
                val mapped = when {
                    ch.isLowerCase() -> font.lower[s] ?: s
                    ch.isUpperCase() -> font.upper[s] ?: font.lower[s.lowercase()] ?: s
                    else -> s
                }
                sb.append(mapped)
                if (mixMarks && symbols.isNotEmpty() && !ch.isWhitespace() && charIdx < baseName.length - 1) {
                    val mark = if (shuffle) symbols[Random.nextInt(symbols.size)] else symbols[(idx + charIdx) % symbols.size]
                    sb.append(mark)
                }
            }
            sb.append(effectiveOutro)
            results.add(font.name to sb.toString())
        }
        return results
    }

    suspend fun getPosts(context: Context): List<FeedItem> = withContext(Dispatchers.IO) {
        cachedPosts?.let { return@withContext it }
        val list = mutableListOf<FeedItem>()
        try {
            context.assets.open("posts.json").use { stream ->
                val root = gson.fromJson(InputStreamReader(stream), JsonObject::class.java)
                for (key in root.keySet()) {
                    val obj = root.getAsJsonObject(key) ?: continue
                    val text = obj.get("text")?.asString ?: obj.get("content")?.asString ?: ""
                    if (text.isNotBlank()) {
                        list.add(
                            FeedItem(
                                id = key,
                                author = obj.get("authorName")?.asString ?: "Style Creator",
                                text = text,
                                category = obj.get("category")?.asString ?: "#Style"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cachedPosts = list
        list
    }

    suspend fun getMoments(context: Context): List<FeedItem> = withContext(Dispatchers.IO) {
        cachedMoments?.let { return@withContext it }
        val list = mutableListOf<FeedItem>()
        try {
            context.assets.open("moments.json").use { stream ->
                val root = gson.fromJson(InputStreamReader(stream), JsonObject::class.java)
                for (key in root.keySet()) {
                    val obj = root.getAsJsonObject(key) ?: continue
                    val text = obj.get("text")?.asString ?: obj.get("styledText")?.asString ?: ""
                    if (text.isNotBlank()) {
                        list.add(
                            FeedItem(
                                id = key,
                                author = obj.get("authorName")?.asString ?: "Editor Moment",
                                text = text,
                                category = "#Moment"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        cachedMoments = list
        list
    }
}
