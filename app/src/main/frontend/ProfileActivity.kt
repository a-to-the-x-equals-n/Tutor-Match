package com.tm.frontend

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tm.R
import org.json.JSONArray
import java.io.File
import java.util.UUID

class ProfileActivity : BaseActivity() {
    private lateinit var nameTextView: TextView
    private lateinit var roleTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        nameTextView = findViewById(R.id.nameTextView)
        roleTextView = findViewById(R.id.roleTextView)

        val currentUserUUID = getCurrentUserUUID()
        loadUserProfile(currentUserUUID)

        val logoutButton: Button = findViewById(R.id.logoutButton)
        logoutButton.setOnClickListener {
            handleLogout()
        }
    }

    /**
     * Retrieves the current user's UUID from shared preferences.
     * @return The UUID of the current user as a String.
     */
    private fun getCurrentUserUUID(): String {
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        return sharedPreferences.getString("userUUID", "") ?: ""
    }

    /**
     * Loads the user profile data from a local JSON file and updates the UI.
     * @param uuid The UUID of the user whose profile is to be loaded.
     */
    private fun loadUserProfile(uuid: String) {
        val fileName = "account_db.json"
        val file = File(filesDir, fileName)
        if (!file.exists()) {
            Toast.makeText(this, "Account file not found.", Toast.LENGTH_LONG).show()
            return
        }

        val jsonStr = file.readText(Charsets.UTF_8)
        val jsonArray = JSONArray(jsonStr)

        for (i in 0 until jsonArray.length()) {
            val userJson = jsonArray.getJSONObject(i)
            if (UUID.fromString(userJson.getString("ID")).toString() == uuid) {
                val name = userJson.getString("name")
                val role = if (userJson.getBoolean("tutor")) "Tutor" else "Student"

                nameTextView.text = name
                roleTextView.text = role
                break
            }
        }
    }

    /**
     * Handles user logout, clearing the task stack and starting the LoginActivity.
     */
    private fun handleLogout() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Initializes the main layout for the activity.
     * @return The main layout as a ViewGroup.
     */
    override fun initializeMainLayout(): ViewGroup {
        return findViewById<ConstraintLayout>(R.id.main_layout)
    }

    /**
     * Sets the active item in the bottom navigation to highlight the profile page.
     */
    override fun setActiveBottomNavigationItem() {
        findViewById<BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.navigation_profile
    }
}
