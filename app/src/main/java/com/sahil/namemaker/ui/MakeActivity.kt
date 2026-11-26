package com.sahil.namemaker.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sahil.namemaker.R
import com.sahil.namemaker.data.OfflineRepository
import com.sahil.namemaker.databinding.ActivityMakeBinding
import com.sahil.namemaker.utils.FontUtils
import com.sahil.namemaker.utils.LocalStore
import com.sahil.namemaker.utils.ThemeManager
import kotlinx.coroutines.launch

class MakeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMakeBinding
    private var templates: List<String> = emptyList()
    private var selectedTemplate = "★彡 Name 彡★"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMakeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val tf = FontUtils.loadSymbolTypeface(this)
        binding.tvPreview.typeface = tf
        binding.etName.setText(LocalStore.getUserName(this))

        val theme = ThemeManager.getCurrent(this)
        binding.makeRoot.setBackgroundColor(theme.bg)
        binding.makeHeader.setBackgroundColor(theme.dark)
        binding.makeHeaderTitle.setTextColor(theme.accent)
        binding.btnBack.setColorFilter(ThemeManager.contrastOn(theme.dark))
        window.statusBarColor = theme.dark

        binding.btnBack.setOnClickListener { finish() }

        binding.btnCopyResult.setOnClickListener {
            LocalStore.copyAndTrack(this, binding.tvPreview.text.toString())
        }

        binding.btnSendToEditor.setOnClickListener {
            val intent = Intent(this, EditorActivity::class.java)
            intent.putExtra("preset_text", binding.tvPreview.text.toString())
            startActivity(intent)
        }

        binding.etName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                refreshPreview()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.rvTemplates.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            val sections = OfflineRepository.getCatalog(this@MakeActivity)
            val tpl = sections.find { it.id == "templates" }?.items ?: emptyList()
            val frames = sections.find { it.id == "frames" }?.items?.map { "$it Name $it" } ?: emptyList()
            templates = (tpl + frames).distinct()
            if (templates.isNotEmpty()) selectedTemplate = templates.first()
            refreshPreview()

            binding.rvTemplates.adapter = object : RecyclerView.Adapter<TemplateHolder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemplateHolder {
                    val v = LayoutInflater.from(parent.context).inflate(R.layout.item_catalog_row, parent, false)
                    return TemplateHolder(v)
                }

                override fun onBindViewHolder(holder: TemplateHolder, position: Int) {
                    val tplStr = templates[position]
                    val name = binding.etName.text.toString().trim().ifEmpty { "Sahil" }
                    val rendered = if (tplStr.contains("Name")) tplStr.replace("Name", name) else "$tplStr $name"
                    holder.tv.typeface = tf
                    holder.tv.text = rendered
                    holder.tv.setTextColor(ThemeManager.contrastOn(theme.cardBg))
                    holder.cardRoot.background = ThemeManager.roundedCard(theme.cardBg, 0x22000000, 24f)
                    holder.btnCopy.visibility = View.GONE
                    holder.btnFav.visibility = View.GONE
                    holder.itemView.setOnClickListener {
                        selectedTemplate = tplStr
                        refreshPreview()
                    }
                }

                override fun getItemCount(): Int = templates.size
            }
        }
    }

    private fun refreshPreview() {
        val name = binding.etName.text.toString().trim().ifEmpty { "Sahil" }
        val result = if (selectedTemplate.contains("Name")) {
            selectedTemplate.replace("Name", name)
        } else {
            "$selectedTemplate $name"
        }
        binding.tvPreview.text = result
    }

    class TemplateHolder(v: View) : RecyclerView.ViewHolder(v) {
        val cardRoot: View = v.findViewById(R.id.cardRoot)
        val tv: TextView = v.findViewById(R.id.tvItemValue)
        val btnFav: View = v.findViewById(R.id.btnFav)
        val btnCopy: View = v.findViewById(R.id.btnCopy)
    }
}
