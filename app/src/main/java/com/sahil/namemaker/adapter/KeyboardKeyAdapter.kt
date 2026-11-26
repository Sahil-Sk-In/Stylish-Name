package com.sahil.namemaker.adapter

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sahil.namemaker.R
import com.sahil.namemaker.utils.S2Theme
import com.sahil.namemaker.utils.ThemeManager

class KeyboardKeyAdapter(
    private val context: Context,
    private val typeface: Typeface,
    private val onKeyClick: (String) -> Unit,
    private val onKeyLongClick: ((String) -> Unit)? = null
) : RecyclerView.Adapter<KeyboardKeyAdapter.KeyHolder>() {

    private val items = mutableListOf<String>()
    private var listMode = false
    private var theme: S2Theme = ThemeManager.getCurrent(context)

    fun setTheme(newTheme: S2Theme) {
        theme = newTheme
        notifyDataSetChanged()
    }

    fun submitKeys(keys: List<String>, isListMode: Boolean) {
        items.clear()
        items.addAll(keys)
        listMode = isListMode
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeyHolder {
        val tv = LayoutInflater.from(parent.context).inflate(R.layout.item_keyboard_key, parent, false) as TextView
        return KeyHolder(tv)
    }

    override fun onBindViewHolder(holder: KeyHolder, position: Int) {
        val value = items[position]
        val tv = holder.textView
        tv.typeface = typeface
        tv.text = value

        val fill = theme.cardBg
        val stroke = if (theme.isDark) 0xFF333B52.toInt() else 0x22000000
        tv.background = ThemeManager.roundedCard(fill, stroke, 20f)
        tv.setTextColor(ThemeManager.contrastOn(fill))

        if (listMode) {
            tv.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
            tv.minHeight = (46 * context.resources.displayMetrics.density).toInt()
            tv.maxLines = 4
            tv.gravity = Gravity.CENTER_VERTICAL or Gravity.START
            val padH = (14 * context.resources.displayMetrics.density).toInt()
            val padV = (10 * context.resources.displayMetrics.density).toInt()
            tv.setPadding(padH, padV, padH, padV)
            tv.textSize = 16f
        } else {
            tv.layoutParams.height = (48 * context.resources.displayMetrics.density).toInt()
            tv.maxLines = 1
            tv.gravity = Gravity.CENTER
            tv.setPadding(6, 6, 6, 6)
            tv.textSize = 19f
        }

        tv.setOnClickListener { onKeyClick(value) }
        tv.setOnLongClickListener {
            onKeyLongClick?.invoke(value)
            true
        }
    }

    override fun getItemCount(): Int = items.size

    class KeyHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
}
