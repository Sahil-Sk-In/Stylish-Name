package com.sahil.namemaker

import android.app.AlertDialog
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import com.sahil.namemaker.data.OfflineRepository
import com.sahil.namemaker.databinding.ActivityMainBinding
import com.sahil.namemaker.fragment.CatalogFragment
import com.sahil.namemaker.fragment.HomeFragment
import com.sahil.namemaker.utils.FontUtils
import com.sahil.namemaker.utils.LocalStore
import com.sahil.namemaker.utils.ThemeManager
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG_HOME = "tab_home"
        private const val TAG_CATALOG = "tab_catalog"
    }

    private lateinit var binding: ActivityMainBinding
    private var currentTag = TAG_HOME

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_NameMaker)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.tabHost, HomeFragment(), TAG_HOME)
                .add(R.id.tabHost, CatalogFragment(), TAG_CATALOG)
                .commitNow()
        }

        setupBottomNav()
        setupDrawer()
        switchTab(TAG_HOME)
        applyCurrentTheme()

        if (savedInstanceState == null) {
            playOpeningTransition()
        }

        // Pre-warm fonts and assets in background after first frame so Studio & Catalog open instantly
        window.decorView.post {
            lifecycleScope.launch {
                val appCtx = applicationContext
                val fontJob = async { FontUtils.loadSymbolTypefaceAsync(appCtx) }
                val catalogJob = async { OfflineRepository.getCatalog(appCtx) }
                val alphabetJob = async { OfflineRepository.getAlphabet(appCtx) }
                fontJob.await()
                catalogJob.await()
                alphabetJob.await()
            }
        }
    }

    private fun playOpeningTransition() {
        val interp = DecelerateInterpolator(1.6f)
        binding.mainContentRoot.alpha = 0f
        binding.mainContentRoot.animate()
            .alpha(1f)
            .setDuration(280L)
            .setInterpolator(interp)
            .start()

        binding.bottomNav.alpha = 0f
        binding.bottomNav.translationY = 40f
        binding.bottomNav.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(360L)
            .setStartDelay(80L)
            .setInterpolator(interp)
            .start()
    }

    private fun setupBottomNav() {
        binding.tabHome.setOnClickListener { switchTab(TAG_HOME) }
        binding.tabCatalog.setOnClickListener { switchTab(TAG_CATALOG) }
    }

    private fun setupDrawer() {
        updateNavHeader()
        binding.navigationView.setNavigationItemSelectedListener { item ->
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            when (item.itemId) {
                R.id.nav_change_name -> showChangeNameDialog()
                R.id.nav_theme -> showThemePickerDialog()
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
        listOf(TAG_HOME, TAG_CATALOG).forEach { t ->
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
            Triple(TAG_CATALOG, binding.iconCatalog, binding.labelCatalog)
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
        val tintList = android.content.res.ColorStateList.valueOf(theme.accent)
        binding.navigationView.itemIconTintList = tintList
        binding.navigationView.getHeaderView(0)?.let { hdr ->
            hdr.findViewById<android.view.View>(R.id.navHeaderRoot)?.setBackgroundColor(theme.dark)
            hdr.findViewById<TextView>(R.id.tvNavBio)?.setTextColor(theme.accent)
        }

        window.statusBarColor = theme.dark
        window.navigationBarColor = theme.bg

        updateBottomNavStyles()

        (supportFragmentManager.findFragmentByTag(TAG_HOME) as? HomeFragment)?.refreshUI()
        (supportFragmentManager.findFragmentByTag(TAG_CATALOG) as? CatalogFragment)?.applyTheme()
    }
}
