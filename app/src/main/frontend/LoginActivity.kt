package com.tm.frontend

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.tm.R
import com.tm.backend.Account

import com.tm.backend.DatabaseManager
import org.json.JSONArray
import java.io.File
import java.util.UUID


class LoginActivity : BaseActivity() {
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        val SDK_INT = Build.VERSION.SDK_INT
        if (SDK_INT > 8) {
            val policy = ThreadPolicy.Builder()
                .permitAll().build()
            StrictMode.setThreadPolicy(policy)
            //your codes here
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        val loginButton: Button = findViewById(R.id.loginToHomeButton)

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString()
            attemptLogin(email, password)
        }

        val signupToHomeButton: Button = findViewById(R.id.loginToSignupButton)
        signupToHomeButton.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }

    /**
     * Attempts to log in a user by checking credentials against stored data.
     * @param email User's email as a String.
     * @param password User's password as a String.
     */
    private fun attemptLogin(email: String, password: String) {
        val db = DatabaseManager()
        val user = db.login(email, password)
        if (user != null) {
            saveUserDetails(user)
            startActivity(Intent(this, CoursesActivity::class.java))
            finish()
        }
        else{
            Toast.makeText(this, "Access Denied", Toast.LENGTH_LONG).show()
        }
       /*val user = loadUserAccount("account_db.json", email)
        if (user != null && user.password == password) {
            saveUserDetails(user)  // Save the user's UUID and role on successful login
            startActivity(Intent(this, CoursesActivity::class.java))
            finish()
        } else {
            Toast.makeText(this, "Access Denied", Toast.LENGTH_LONG).show()
        }*/
    }

    /**
     * Saves user details in SharedPreferences after successful login.
     * @param user The user account object containing the user's details.
     */
    private fun saveUserDetails(user: Account) {
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("userUUID", user.ID.toString())  // Save UUID
            putString("userRole", if (user.tutor) "tutor" else "student")  // Save role
            apply()
        }
    }

    /**
     * Loads a user account from a JSON file if the user exists with the given email.
     * @param fileName Name of the JSON file where user accounts are stored.
     * @param email Email of the user to retrieve.
     * @return Account object if found, null otherwise.
     */
    private fun loadUserAccount(fileName: String, email: String): Account? {
        val file = File(filesDir, fileName)
        if (!file.exists()) {
            Toast.makeText(this, "Account file not found.", Toast.LENGTH_LONG).show()
            return null
        }

        val jsonStr = file.readText(Charsets.UTF_8)
        val jsonArray = JSONArray(jsonStr)

        for (i in 0 until jsonArray.length()) {
            val userJson = jsonArray.getJSONObject(i)
            if (userJson.getString("email") == email) {
                return Account(
                    name = userJson.getString("name"),
                    email = userJson.getString("email"),
                    password = userJson.getString("password"),
                    tutor = userJson.getBoolean("tutor"),
                    rating = userJson.optDouble("rating", 0.0).toFloat(),
                    studyTime = userJson.optInt("studyTime", 0),
                    ID = UUID.fromString(userJson.getString("ID")),
                    token = ""
                )
            }
        }
        return null
    }

    /**
     * Initializes and returns the main layout of the activity.
     * @return The ViewGroup that is the main layout of the activity.
     */
    override fun initializeMainLayout(): ViewGroup {
        return findViewById<ConstraintLayout>(R.id.main_layout)
    }

    /**
     * Sets the active item in the bottom navigation which is not used in this activity.
     */
    override fun setActiveBottomNavigationItem() {
        // LoginActivity does not use the bottom navigation
    }
}