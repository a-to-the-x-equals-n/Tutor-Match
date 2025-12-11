package com.tm.frontend

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.tm.R


//Figure out how to stop android built splash
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logoAnimation = AnimationUtils.loadAnimation(this, R.anim.zoom_in)
        val textAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)

        findViewById<ImageView>(R.id.logoImageView).startAnimation(logoAnimation)
        findViewById<TextView>(R.id.appNameTextView).startAnimation(textAnimation)

        // Navigate to MainActivity after animations complete
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }, 2500) // Wait
    }
}
