package com.tm.frontend

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import com.tm.R

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Setup the central button to navigate to SecondActivity
        val centralButton: Button = findViewById(R.id.centralButton)
        centralButton.setOnClickListener {
            startActivity(Intent(this, SecondActivity::class.java))
        }

        // Setup the top-left button to navigate to ProfileActivity
        val topLeftButton: Button = findViewById(R.id.topLeftButton)
        topLeftButton.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // Setup the bot-left button to navigate to CoursesActivity
        val bottomLeftButton: Button = findViewById(R.id.bottomLeftButton)
        bottomLeftButton.setOnClickListener {
            startActivity(Intent(this, CoursesActivity::class.java))
        }

        // Setup the bot-right button to navigate to SessionsActivity
        val bottomRightButton: Button = findViewById(R.id.bottomRightButton)
        bottomRightButton.setOnClickListener {
            startActivity(Intent(this, SessionsActivity::class.java))

        }

        // Setup the bot-right button to navigate to SessionsActivity
        val topRightButton: Button = findViewById(R.id.topRightButton)
        topRightButton.setOnClickListener {
            startActivity(Intent(this, CalendarActivity::class.java))
        }
    }
}
