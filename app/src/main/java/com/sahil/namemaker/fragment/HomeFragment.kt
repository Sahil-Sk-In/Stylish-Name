package com.sahil.namemaker.fragment

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.sahil.namemaker.MainActivity
import com.sahil.namemaker.R
import com.sahil.namemaker.ui.EditorActivity
import com.sahil.namemaker.ui.MakeActivity
import com.sahil.namemaker.utils.LocalStore
import com.sahil.namemaker.utils.ThemeManager
import java.util.Calendar

class HomeFragment : Fragment() {
    private var rootView: View? = null
    private var ringOuter: View? = null
    private var ringMiddle: View? = null
    private var btnEditor: CardView? = null
    private var btnEditOverlay: ImageView? = null
    private var btnThemeQuick: CardView? = null
    private var btnThemeQuickIcon: ImageView? = null
    private var tvCenterTitle: TextView? = null
    private var tvGreeting: TextView? = null
    private var tvBottom: TextView? = null
    private var btnOpenEditorPill: Button? = null
    private var btnMakeNamePill: Button? = null
    private var ringAnimators: AnimatorSet? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rootView = view.findViewById(R.id.homeRoot)
        ringOuter = view.findViewById(R.id.ringOuter)
        ringMiddle = view.findViewById(R.id.ringMiddle)
        btnEditor = view.findViewById(R.id.btnEditor)
        btnEditOverlay = view.findViewById(R.id.btnEditOverlay)
        btnThemeQuick = view.findViewById(R.id.btnThemeQuick)
        btnThemeQuickIcon = view.findViewById(R.id.btnThemeQuickIcon)
        tvCenterTitle = view.findViewById(R.id.tvCenterTitle)
        tvGreeting = view.findViewById(R.id.tvGreeting)
        tvBottom = view.findViewById(R.id.tvBottom)
        btnOpenEditorPill = view.findViewById(R.id.btnOpenEditorPill)
        btnMakeNamePill = view.findViewById(R.id.btnMakeNamePill)

        view.findViewById<CardView>(R.id.btnProfile).setOnClickListener {
            (activity?.findViewById<DrawerLayout>(R.id.drawerLayout))?.openDrawer(GravityCompat.START)
        }

        btnThemeQuick?.setOnClickListener {
            (activity as? MainActivity)?.showThemePickerDialog()
        }

        val openEditor = View.OnClickListener {
            startActivity(Intent(requireContext(), EditorActivity::class.java))
        }
        btnEditor?.setOnClickListener(openEditor)
        btnEditOverlay?.setOnClickListener(openEditor)
        btnOpenEditorPill?.setOnClickListener(openEditor)

        btnMakeNamePill?.setOnClickListener {
            startActivity(Intent(requireContext(), MakeActivity::class.java))
        }

        refreshUI()
        startRingAnimations()
    }

    override fun onResume() {
        super.onResume()
        refreshUI()
        startRingAnimations()
    }

    override fun onPause() {
        super.onPause()
        stopRingAnimations()
    }

    fun refreshUI() {
        val ctx = context ?: return
        val theme = ThemeManager.getCurrent(ctx)
        val onBg = ThemeManager.contrastOn(theme.bg)
        val onDark = ThemeManager.contrastOn(theme.dark)

        rootView?.setBackgroundColor(theme.bg)
        btnEditor?.setCardBackgroundColor(theme.dark)
        btnThemeQuick?.setCardBackgroundColor(theme.cardBg)
        btnThemeQuickIcon?.setColorFilter(ThemeManager.contrastOn(theme.cardBg))

        tvCenterTitle?.setTextColor(onBg)
        tvGreeting?.setTextColor((onBg and 0x00FFFFFF) or 0xAA000000.toInt())
        tvBottom?.setTextColor(onBg)

        btnOpenEditorPill?.backgroundTintList = ColorStateList.valueOf(theme.dark)
        btnOpenEditorPill?.setTextColor(onDark)
        btnMakeNamePill?.setTextColor(onBg)

        val ringColor = if (theme.isDark) 0x33FFFFFF else 0x33000000
        ringOuter?.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setStroke(3, ringColor)
            setColor(0x00000000)
        }
        ringMiddle?.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setStroke(3, ringColor)
            setColor(0x00000000)
        }

        val name = LocalStore.getUserName(ctx)
        tvCenterTitle?.text = "Hello, $name"
        tvGreeting?.text = timeBasedGreeting()
    }

    private fun timeBasedGreeting(): String {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when (hour) {
            in 5..11 -> "Good Morning, Name Maker Studio"
            in 12..16 -> "Good Afternoon, Name Maker Studio"
            in 17..20 -> "Good Evening, Name Maker Studio"
            else -> "Late Night, Name Maker Studio"
        }
    }

    private fun startRingAnimations() {
        stopRingAnimations()
        val outer = ringOuter ?: return
        val middle = ringMiddle ?: return

        val outerAnim = ObjectAnimator.ofPropertyValuesHolder(
            outer,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 0.96f, 1.04f, 0.96f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.96f, 1.04f, 0.96f),
            PropertyValuesHolder.ofFloat(View.ALPHA, 0.55f, 1.0f, 0.55f)
        ).apply {
            duration = 3200L
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
        }

        val middleAnim = ObjectAnimator.ofPropertyValuesHolder(
            middle,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1.03f, 0.97f, 1.03f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1.03f, 0.97f, 1.03f),
            PropertyValuesHolder.ofFloat(View.ALPHA, 1.0f, 0.6f, 1.0f)
        ).apply {
            duration = 2800L
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
        }

        ringAnimators = AnimatorSet().apply {
            playTogether(outerAnim, middleAnim)
            start()
        }
    }

    private fun stopRingAnimations() {
        ringAnimators?.cancel()
        ringAnimators = null
    }
}
