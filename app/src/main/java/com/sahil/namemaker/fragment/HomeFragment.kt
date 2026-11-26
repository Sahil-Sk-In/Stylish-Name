package com.sahil.namemaker.fragment

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.ActivityOptions
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.sahil.namemaker.R
import com.sahil.namemaker.ui.EditorActivity
import com.sahil.namemaker.utils.LocalStore
import com.sahil.namemaker.utils.ThemeManager
import java.util.Calendar

class HomeFragment : Fragment() {
    private var rootView: View? = null
    private var avatarContainer: View? = null
    private var ringOuter: View? = null
    private var ringMiddle: View? = null
    private var btnProfile: CardView? = null
    private var btnProfileIcon: ImageView? = null
    private var btnEditor: CardView? = null
    private var btnEditOverlay: ImageView? = null
    private var tvHeroAction: TextView? = null
    private var tvCenterTitle: TextView? = null
    private var tvGreeting: TextView? = null
    private var tvStudioLabel: TextView? = null
    private var tvBottom: TextView? = null
    private var ringAnimators: AnimatorSet? = null
    private var entrancePlayed = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rootView = view.findViewById(R.id.homeRoot)
        avatarContainer = view.findViewById(R.id.avatarContainer)
        ringOuter = view.findViewById(R.id.ringOuter)
        ringMiddle = view.findViewById(R.id.ringMiddle)
        btnProfile = view.findViewById(R.id.btnProfile)
        btnProfileIcon = view.findViewById(R.id.btnProfileIcon)
        btnEditor = view.findViewById(R.id.btnEditor)
        btnEditOverlay = view.findViewById(R.id.btnEditOverlay)
        tvHeroAction = view.findViewById(R.id.tvHeroAction)
        tvCenterTitle = view.findViewById(R.id.tvCenterTitle)
        tvGreeting = view.findViewById(R.id.tvGreeting)
        tvStudioLabel = view.findViewById(R.id.tvStudioLabel)
        tvBottom = view.findViewById(R.id.tvBottom)

        btnProfile?.setOnClickListener {
            (activity?.findViewById<DrawerLayout>(R.id.drawerLayout))?.openDrawer(GravityCompat.START)
        }

        btnEditor?.setOnClickListener {
            val ctx = requireContext()
            val opts = ActivityOptions.makeCustomAnimation(ctx, android.R.anim.fade_in, android.R.anim.fade_out)
            startActivity(Intent(ctx, EditorActivity::class.java), opts.toBundle())
        }

        refreshUI()
        if (!entrancePlayed) {
            entrancePlayed = true
            playEntranceAnimation()
        } else {
            startRingAnimations()
        }
    }

    private fun playEntranceAnimation() {
        val decel = DecelerateInterpolator(1.6f)
        val spring = OvershootInterpolator(1.08f)

        btnProfile?.apply {
            alpha = 0f
            scaleX = 0.85f
            scaleY = 0.85f
            animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(300L).setInterpolator(decel).start()
        }

        listOf(tvCenterTitle, tvGreeting).forEachIndexed { idx, v ->
            v?.apply {
                alpha = 0f
                translationY = 24f
                animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(340L)
                    .setStartDelay((idx * 50L))
                    .setInterpolator(decel)
                    .start()
            }
        }

        avatarContainer?.apply {
            alpha = 0f
            scaleX = 0.82f
            scaleY = 0.82f
            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(420L)
                .setStartDelay(60L)
                .setInterpolator(spring)
                .withEndAction { startRingAnimations() }
                .start()
        } ?: startRingAnimations()

        listOf(tvStudioLabel, tvBottom).forEachIndexed { idx, v ->
            v?.apply {
                alpha = 0f
                translationY = 16f
                animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(320L)
                    .setStartDelay(120L + idx * 45L)
                    .setInterpolator(decel)
                    .start()
            }
        }
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
        val onCard = ThemeManager.contrastOn(theme.cardBg)
        val heroFill = if (theme.isDark) theme.accent else theme.dark
        val onHero = ThemeManager.contrastOn(heroFill)

        rootView?.setBackgroundColor(theme.bg)
        btnProfile?.setCardBackgroundColor(theme.cardBg)
        btnProfileIcon?.setColorFilter(onCard)

        btnEditor?.setCardBackgroundColor(heroFill)
        btnEditOverlay?.setColorFilter(onHero)
        tvHeroAction?.setTextColor(onHero)

        tvCenterTitle?.setTextColor(onBg)
        tvGreeting?.setTextColor((onBg and 0x00FFFFFF) or 0xAA000000.toInt())
        tvStudioLabel?.setTextColor(onBg)
        tvBottom?.setTextColor((onBg and 0x00FFFFFF) or 0x88000000.toInt())

        val ringColor = if (theme.isDark) 0x33BB86FC else 0x334A148C
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
            in 5..11 -> "Good Morning • Stylish Names & Bios"
            in 12..16 -> "Good Afternoon • Stylish Names & Bios"
            in 17..20 -> "Good Evening • Stylish Names & Bios"
            else -> "Late Night • Stylish Names & Bios"
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
