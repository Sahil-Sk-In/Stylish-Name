package com.sahil.namemaker.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.sahil.namemaker.data.model.Decoration
import com.sahil.namemaker.data.model.KeyboardFont
import com.sahil.namemaker.data.model.StylishPost
import com.sahil.namemaker.data.model.SymbolCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class AppRepository(private val context: Context) {
    private val gson = Gson()

    // In-memory cache for 0ms ultra-fast subsequent queries
    private var cachedPosts: List<StylishPost>? = null
    private var cachedDecorations: List<Decoration>? = null
    private var cachedKeyboards: List<KeyboardFont>? = null
    private var cachedCategories: List<SymbolCategory>? = null
    private var cachedAlphaSymbols: Map<String, List<String>>? = null

    suspend fun getPosts(): List<StylishPost> = withContext(Dispatchers.IO) {
        cachedPosts?.let { return@withContext it }

        val list = mutableListOf<StylishPost>()
        try {
            context.assets.open("posts.json").use { stream ->
                val jsonObject = gson.fromJson(InputStreamReader(stream), JsonObject::class.java)
                for (key in jsonObject.keySet()) {
                    val item = jsonObject.getAsJsonObject(key) ?: continue
                    val text = item.get("text")?.asString ?: item.get("content")?.asString ?: ""
                    if (text.isNotBlank()) {
                        list.add(
                            StylishPost(
                                id = key,
                                text = text,
                                category = item.get("category")?.asString ?: "#Random",
                                keywords = item.get("keywords")?.asString ?: "",
                                createdAt = item.get("createdAt")?.asString ?: "",
                                authorName = "Sahil"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Also load stylish_names.txt as curated posts if available
        try {
            context.assets.open("stylish_names.txt").use { stream ->
                val reader = BufferedReader(InputStreamReader(stream))
                var line: String?
                var count = 0
                while (reader.readLine().also { line = it } != null) {
                    val trimmed = line?.trim() ?: ""
                    if (trimmed.length > 3 && !trimmed.startsWith("//") && count < 60) {
                        list.add(
                            StylishPost(
                                id = "curated_$count",
                                text = trimmed,
                                category = "#VIP_Name",
                                keywords = "vip stylish name freefire pubg",
                                createdAt = "",
                                authorName = "VIP Studio"
                            )
                        )
                        count++
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        cachedPosts = list
        list
    }

    suspend fun getCrunchData(): Pair<List<Decoration>, List<KeyboardFont>> = withContext(Dispatchers.IO) {
        if (cachedDecorations != null && cachedKeyboards != null) {
            return@withContext Pair(cachedDecorations!!, cachedKeyboards!!)
        }

        val decors = mutableListOf<Decoration>()
        val keyboards = mutableListOf<KeyboardFont>()

        try {
            context.assets.open("crunch_data.json").use { stream ->
                val root = gson.fromJson(InputStreamReader(stream), JsonObject::class.java)

                // 1. Decorations (Intros, Outros, etc.)
                if (root.has("decorations")) {
                    val decArray = root.getAsJsonArray("decorations")
                    for (el in decArray) {
                        val obj = el.asJsonObject
                        decors.add(
                            Decoration(
                                id = obj.get("id")?.asString ?: "",
                                text = obj.get("text")?.asString ?: "",
                                type = obj.get("type")?.asString ?: "intro"
                            )
                        )
                    }
                }

                // 2. Keyboard Fonts (K 1 .. K 25)
                if (root.has("keyboards")) {
                    val kbArray = root.getAsJsonArray("keyboards")
                    for (el in kbArray) {
                        val obj = el.asJsonObject
                        val name = obj.get("name")?.asString ?: "Font"
                        val id = obj.get("id")?.asString ?: ""

                        val lowerMap = mutableMapOf<String, String>()
                        val upperMap = mutableMapOf<String, String>()

                        if (obj.has("lower") && obj.get("lower").isJsonObject) {
                            val lObj = obj.getAsJsonObject("lower")
                            for (lk in lObj.keySet()) {
                                lowerMap[lk] = lObj.get(lk).asString
                            }
                        }
                        if (obj.has("upper") && obj.get("upper").isJsonObject) {
                            val uObj = obj.getAsJsonObject("upper")
                            for (uk in uObj.keySet()) {
                                upperMap[uk] = uObj.get(uk).asString
                            }
                        }

                        keyboards.add(
                            KeyboardFont(
                                id = id,
                                name = name,
                                lower = lowerMap,
                                upper = upperMap
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        cachedDecorations = decors
        cachedKeyboards = keyboards
        Pair(decors, keyboards)
    }

    suspend fun getCustomCategories(): List<SymbolCategory> = withContext(Dispatchers.IO) {
        cachedCategories?.let { return@withContext it }

        val list = mutableListOf<SymbolCategory>()
        try {
            context.assets.open("custom_categories.json").use { stream ->
                val root = gson.fromJson(InputStreamReader(stream), JsonObject::class.java)
                for (key in root.keySet()) {
                    val obj = root.getAsJsonObject(key) ?: continue
                    val name = obj.get("name")?.asString ?: key
                    val preview = obj.get("preview")?.asString ?: ""
                    val kind = obj.get("kind")?.asString ?: ""

                    val items = mutableListOf<String>()
                    if (obj.has("items") && obj.get("items").isJsonArray) {
                        for (itEl in obj.getAsJsonArray("items")) {
                            items.add(itEl.asString)
                        }
                    }

                    list.add(
                        SymbolCategory(
                            id = key,
                            name = name,
                            items = items,
                            preview = preview,
                            kind = kind
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Also add S2 Special Marks from symbols.json as a category
        try {
            val alphaMap = getAlphaSymbols()
            if (alphaMap.isNotEmpty()) {
                val allAlphaSymbols = alphaMap.values.flatten().distinct()
                list.add(
                    0,
                    SymbolCategory(
                        id = "s2_special_marks",
                        name = "★ S2 Marks",
                        items = allAlphaSymbols,
                        preview = "★",
                        kind = "marks"
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        cachedCategories = list
        list
    }

    suspend fun getAlphaSymbols(): Map<String, List<String>> = withContext(Dispatchers.IO) {
        cachedAlphaSymbols?.let { return@withContext it }

        val map = mutableMapOf<String, List<String>>()
        try {
            context.assets.open("symbols.json").use { stream ->
                val root = gson.fromJson(InputStreamReader(stream), JsonObject::class.java)
                for (key in root.keySet()) {
                    val arr = root.getAsJsonArray(key) ?: continue
                    val items = mutableListOf<String>()
                    for (el in arr) {
                        items.add(el.asString)
                    }
                    map[key] = items
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        cachedAlphaSymbols = map
        map
    }
}
