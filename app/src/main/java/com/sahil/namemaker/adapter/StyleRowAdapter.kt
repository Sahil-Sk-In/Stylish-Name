package com.sahil.namemaker.adapter

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.sahil.namemaker.R
import com.sahil.namemaker.ui.EditorActivity
import com.sahil.namemaker.utils.LocalStore
import com.sahil.namemaker.utils.S2Theme
import com.sahil.namemaker.utils.ThemeManager

data class StyleDisplayItem(
    val value: String,
    val label: String? = null
)

class StyleRowAdapter(
    private val context: Context,
    private val typeface: Typeface,
    private val onFavoriteChanged: (() -> Unit)? = null
) : RecyclerView.Adapter<StyleRowAdapter.Holder>() {

    private val items = mutableListOf<StyleDisplayItem>()
    private var theme: S2Theme = ThemeManager.getCurrent(context)

    fun setTheme(newTheme: S2Theme) {
        theme = newTheme
        notifyDataSetChanged()
    }

    fun submitList(newItems: List<StyleDisplayItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun getAllValues(): List<String> = items.map { it.value }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_catalog_row, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        val cardFill = theme.cardBg
        val stroke = if (theme.isDark) 0xFF2D3348.toInt() else 0x22000000
        holder.cardRoot.background = ThemeManager.roundedCard(cardFill, stroke, 28f)

        val textColor = ThemeManager.contrastOn(cardFill)
        holder.tvValue.typeface = typeface
        holder.tvValue.setTextColor(textColor)
        holder.tvValue.text = if (item.label != null) "${item.label}\n${item.value}" else item.value

        val isFav = LocalStore.isFavorite(context, item.value)
        holder.btnFav.setColorFilter(if (isFav) 0xFFF59E0B.toInt() else (textColor and 0x55FFFFFF))
        holder.btnCopy.setColorFilter(textColor)

        holder.itemView.setOnClickListener {
            LocalStore.copyAndTrack(context, item.value)
        }
        holder.btnCopy.setOnClickListener {
            LocalStore.copyAndTrack(context, item.value)
        }
        holder.btnFav.setOnClickListener {
            val nowFav = LocalStore.toggleFavorite(context, item.value)
            holder.btnFav.setColorFilter(if (nowFav) 0xFFF59E0B.toInt() else (textColor and 0x55FFFFFF))
            Toast.makeText(context, if (nowFav) "Added to Favorites" else "Removed from Favorites", Toast.LENGTH_SHORT).show()
            onFavoriteChanged?.invoke()
        }
        holder.itemView.setOnLongClickListener {
            showPreviewDialog(item.value)
            true
        }
    }

    private fun showPreviewDialog(value: String) {
        val preview = TextView(context).apply {
            text = value
            textSize = 26f
            typeface = this@StyleRowAdapter.typeface
            gravity = Gravity.CENTER
            setPadding(48, 56, 48, 56)
            setTextIsSelectable(true)
            setTextColor(Color.WHITE)
            setBackgroundColor(0xFF1A1A2E.toInt())
        }
        val isFav = LocalStore.isFavorite(context, value)
        AlertDialog.Builder(context)
            .setView(preview)
            .setPositiveButton("Copy") { _, _ -> LocalStore.copyAndTrack(context, value) }
            .setNeutralButton("Open in Studio") { _, _ ->
                val intent = Intent(context, EditorActivity::class.java)
                intent.putExtra("preset_text", value)
                context.startActivity(intent)
            }
            .setNegativeButton(if (isFav) "Share" else "★ Favorite") { _, _ ->
                if (isFav) {
                    LocalStore.shareText(context, value)
                } else {
                    LocalStore.toggleFavorite(context, value)
                    notifyDataSetChanged()
                    onFavoriteChanged?.invoke()
                    Toast.makeText(context, "Added to Favorites", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    override fun getItemCount(): Int = items.size

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val cardRoot: LinearLayout = view.findViewById(R.id.cardRoot)
        val tvValue: TextView = view.findViewById(R.id.tvItemValue)
        val btnFav: ImageButton = view.findViewById(R.id.btnFav)
        val btnCopy: ImageButton = view.findViewById(R.id.btnCopy)
    }
}
