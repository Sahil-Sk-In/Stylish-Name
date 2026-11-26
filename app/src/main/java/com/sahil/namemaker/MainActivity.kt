package com.sahil.namemaker

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import com.sahil.namemaker.databinding.ActivityMainBinding
import com.sahil.namemaker.fragment.CatalogFragment
import com.sahil.namemaker.fragment.CrunchFragment
import com.sahil.namemaker.fragment.FeedFragment
import com.sahil.namemaker.fragment.HomeFragment
import com.sahil.namemaker.fragment.SavedFragment
import com.sahil.namemaker.ui.EditorActivity
import com.sahil.namemaker.ui.MakeActivity
import com.sahil.namemaker.utils.LocalStore
import com.sahil.namemaker.utils.ThemeManager

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG_HOME = "tab_home"
        private const val TAG_CRUNCH = "tab_crunch"
        private const val TAG_CATALOG = "tab_catalog"
        private const val TAG_FEED = "tab_feed"
        private const val TAG_SAVED = "tab_saved"
    }

    private lateinit var binding: ActivityMainBinding
    private var currentTag = TAG_HOME

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.tabHost, HomeFragment(), TAG_HOME)
                .add(R.id.tabHost, CrunchFragment(), TAG_CRUNCH)
                .add(R.id.tabHost, CatalogFragment(), TAG_CATALOG)
                .add(R.id.tabHost, FeedFragment(), TAG_FEED)
                .add(R.id.tabHost, SavedFragment(), TAG_SAVED)
                .commitNow()
        }

        setupBottomNav()
        setupDrawer()
        switchTab(TAG_HOME)
        applyCurrentTheme()
    }

    private fun setupBottomNav() {
        binding.tabHome.setOnClickListener { switchTab(TAG_HOME) }
        binding.tabCrunch.setOnClickListener { switchTab(TAG_CRUNCH) }
        binding.tabCatalog.setOnClickListener { switchTab(TAG_CATALOG) }
        binding.tabFeed.setOnClickListener { switchTab(TAG_FEED) }
        binding.tabSaved.setOnClickListener { switchTab(TAG_SAVED) }
    }

    private fun setupDrawer() {
        updateNavHeader()
        binding.navigationView.setNavigationItemSelectedListener { item ->
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            when (item.itemId) {
                R.id.nav_editor -> startActivity(Intent(this, EditorActivity::class.java))
                R.id.nav_make -> startActivity(Intent(this, MakeActivity::class.java))
                R.id.nav_crunch -> switchTab(TAG_CRUNCH)
                R.id.nav_catalog -> switchTab(TAG_CATALOG)
                R.id.nav_change_name -> showChangeNameDialog()
                R.id.nav_theme -> showThemePickerDialog()
                R.id.nav_favorites -> switchTab(TAG_SAVED)
            }
            true
        }
    }

    private fun updateNavHeader() {
        val header = binding.navigationView.getHeaderView(0) ?: return
        header.findViewById<TextView>(R.id.tvNavName)?.text = LocalStore.getUserName(this)
    }

    fun switchTab(tag: String) {
        currentTag = tag
        val tx = supportFragmentManager.beginTransaction()
        listOf(TAG_HOME, TAG_CRUNCH, TAG_CATALOG, TAG_FEED, TAG_SAVED).forEach { t ->
            val frag = supportFragmentManager.findFragmentByTag(t)
            if (frag != null) {
                if (t == tag) tx.show(frag) else tx.hide(frag)
            }
        }
        tx.commitAllowingStateLoss()
        updateBottomNavStyles()
    }

    private fun updateBottomNavStyles() {
        val theme = ThemeManager.getCurrent(this)
        val activeColor = theme.accent
        val inactiveColor = if (theme.isDark) 0xFF94A3B8.toInt() else 0xFFCBD5E1.toInt()

        val items = listOf(
            Triple(TAG_HOME, binding.iconHome, binding.labelHome),
            Triple(TAG_CRUNCH, binding.iconCrunch, binding.labelCrunch),
            Triple(TAG_CATALOG, binding.iconCatalog, binding.labelCatalog),
            Triple(TAG_FEED, binding.iconFeed, binding.labelFeed),
            Triple(TAG_SAVED, binding.iconSaved, binding.labelSaved)
        )

        items.forEach { (tag, icon, label) ->
            val selected = tag == currentTag
            val c = if (selected) activeColor else inactiveColor
            icon.setColorFilter(c)
            label.setTextColor(c)
        }
    }

    fun showChangeNameDialog() {
        val input = EditText(this).apply {
            setText(LocalStore.getUserName(this@MainActivity))
            hint = "Display Name"
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(this)
            .setTitle("Change Display Name")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                LocalStore.setUserName(this, input.text.toString())
                updateNavHeader()
                (supportFragmentManager.findFragmentByTag(TAG_HOME) as? HomeFragment)?.refreshUI()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun showThemePickerDialog() {
        AlertDialog.Builder(this)
            .setTitle("Select App Theme")
            .setSingleChoiceItems(ThemeManager.getThemeNames(), ThemeManager.getCurrentIndex(this)) { dialog, which ->
                ThemeManager.setIndex(this, which)
                applyCurrentTheme()
                dialog.dismiss()
            }
            .show()
    }

    fun applyCurrentTheme() {
        val theme = ThemeManager.getCurrent(this)
        binding.drawerLayout.setBackgroundColor(theme.bg)
        binding.mainContentRoot.setBackgroundColor(theme.bg)
        binding.bottomNav.background = ThemeManager.roundedCard(theme.dark, 0x33FFFFFF, 44f)
        binding.navigationView.setBackgroundColor(theme.dark)

        window.statusBarColor = theme.dark
        window.navigationBarColor = theme.bg

        updateBottomNavStyles()

        (supportFragmentManager.findFragmentByTag(TAG_HOME) as? HomeFragment)?.refreshUI()
        (supportFragmentManager.findFragmentByTag(TAG_CRUNCH) as? CrunchFragment)?.applyTheme()
        (supportFragmentManager.findFragmentByTag(TAG_CATALOG) as? CatalogFragment)?.applyTheme()
        (supportFragmentManager.findFragmentByTag(TAG_FEED) as? FeedFragment)?.applyTheme()
        (supportFragmentManager.findFragmentByTag(TAG_SAVED) as? SavedFragment)?.applyTheme()
    }
}
