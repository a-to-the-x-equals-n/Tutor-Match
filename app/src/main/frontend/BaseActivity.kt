package com.tm.frontend

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tm.R

abstract class BaseActivity : AppCompatActivity() {

    lateinit var mainLayout: ViewGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContentView is called in derived classes, so do not access views here
    }

    override fun onStart() {
        super.onStart()
        mainLayout = initializeMainLayout() // Initialize here
        setupBottomNavigation()
        initializeAnimatedBackground()
    }

    private fun setupBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView?.setOnNavigationItemSelectedListener { item ->
            val userRole = getUserRole()
            when (item.itemId) {
                R.id.navigation_courses -> navigateTo(CoursesActivity::class.java)
                R.id.navigation_profile -> navigateTo(ProfileActivity::class.java)
                R.id.navigation_sessions -> navigateTo(SessionsActivity::class.java)
                R.id.navigation_calendar -> {
                    if (userRole == "student") {
                        showAccessDeniedDialog()
                        return@setOnNavigationItemSelectedListener false
                    } else {
                        navigateTo(WeekViewActivity::class.java)
                    }
                }
                else -> false
            }
        }
    }

    private fun showAccessDeniedDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Access Denied")
        builder.setMessage("Students are not allowed to access the schedule.")
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun navigateTo(activityClass: Class<*>, extraKey: String? = null, extraValue: Boolean? = null): Boolean {
        val intent = Intent(this, activityClass)
        if (extraKey != null && extraValue != null) {
            intent.putExtra(extraKey, extraValue)
        }
        startActivity(intent)
        return true
    }

    private fun initializeAnimatedBackground() {
        val evaluator = ArgbEvaluator()
        val colors = arrayOf(
            Color.parseColor("#0000FF"),  // Blue
            Color.parseColor("#8A2BE2"),  // Violet
            Color.parseColor("#FF1493"),  // Pink
            Color.parseColor("#00FFFF"),  // Cyan
            Color.parseColor("#9400D3")   // Dark Violet
        ).toIntArray()

        val gradientDrawable = GradientDrawable(GradientDrawable.Orientation.BL_TR, colors)
        gradientDrawable.gradientType = GradientDrawable.LINEAR_GRADIENT
        mainLayout.background = gradientDrawable

        val valueAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 5000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener { animation ->
                val fraction = animation.animatedFraction
                val newColors = colors.mapIndexed { index, color ->
                    val nextIndex = (index + 1) % colors.size
                    evaluator.evaluate(fraction, color, colors[nextIndex]) as Int
                }.toIntArray()
                gradientDrawable.colors = newColors
            }
        }
        valueAnimator.start()
    }

    private fun getUserRole(): String {
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        return sharedPreferences.getString("userRole", "student") ?: "student"  // Default to "student"
    }


    protected abstract fun initializeMainLayout(): ViewGroup

    abstract fun setActiveBottomNavigationItem()
}
