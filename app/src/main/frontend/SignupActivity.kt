package com.tm.frontend

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.tm.R
import com.tm.backend.Account
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.*
import com.tm.backend.DatabaseManager

import java.util.UUID


class SignupActivity : BaseActivity() {
    private lateinit var nameInput: EditText
    private lateinit var emailInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var tutorCheckbox: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        nameInput = findViewById(R.id.nameInput)
        emailInput = findViewById(R.id.emailInput)
        passwordInput = findViewById(R.id.passwordInput)
        tutorCheckbox = findViewById(R.id.tutorCheckbox)
        val signupToHomeButton: Button = findViewById(R.id.signupToHomeButton)
        val signupToLoginButton: Button = findViewById(R.id.signupToLoginButton)

        signupToLoginButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        signupToHomeButton.setOnClickListener {
            if (validateInput()) {
                if (addAccount() == true) {
                    val intent = Intent(this, LoginActivity::class.java).apply {
                        putExtra("signupSuccess", true)
                    }
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Signup failed. Try again.", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Please enter all details correctly.", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Validates that all input fields are non-empty.
     * @return Boolean indicating whether inputs are valid (true) or not (false).
     */
    private fun validateInput(): Boolean {
        return nameInput.text.isNotEmpty() && emailInput.text.isNotEmpty() && passwordInput.text.isNotEmpty()
    }

    /**
     * Adds a new user account if the email does not already exist.
     * @return Boolean indicating success (true) or failure (false) of the account creation.
     */
    private fun addAccount(): Boolean? {


        val name = nameInput.text.toString()
        val email = emailInput.text.toString()
        val password = passwordInput.text.toString()
        val isTutor = tutorCheckbox.isChecked

        val db = DatabaseManager()
        val newAccount = db.newUser(name, email, password, isTutor)
        if (newAccount == null)
        {
            Toast.makeText(this, "Account Creation Error", Toast.LENGTH_LONG).show()
        }

//        val newAccount = Account(
//            name = name,
//            email = email,
//            password = password,
//            tutor = isTutor,
//            rating = 0f,
//            studyTime = null,
//            ID = UUID.randomUUID(),
//            token = ""
//        )

        if (accountExists(email)) {
            Toast.makeText(this, "Account already exists with this email.", Toast.LENGTH_LONG).show()
            return false
        }

        return newAccount?.let { saveAccountToFile(it) }
    }

    /**
     * Checks if an account with the given email already exists.
     * @param email The email to check against existing accounts.
     * @return Boolean indicating if the account exists (true) or not (false).
     */
    private fun accountExists(email: String): Boolean {
        val fileName = "account_db.json"
        val file = File(filesDir, fileName)
        if (!file.exists()) return false

        val jsonArray = JSONArray(file.readText(Charsets.UTF_8))
        for (i in 0 until jsonArray.length()) {
            val account = jsonArray.getJSONObject(i)
            if (account.getString("email") == email) {
                return true
            }
        }
        return false
    }

    /**
     * Saves the newly created account to a JSON file.
     * @param account The Account object to be saved.
     * @return Boolean indicating if the account was successfully saved (true) or not (false).
     */
    private fun saveAccountToFile(account: Account): Boolean {
        val fileName = "account_db.json"
        val file = File(filesDir, fileName)
        Log.d("SignupActivity", "Saving account to file: ${file.absolutePath}")

        return try {
            val jsonArray = if (file.exists()) {
                JSONArray(file.readText(Charsets.UTF_8))
            } else {
                JSONArray()  // create a new JSON array
            }

            val accountJson = JSONObject().apply {
                put("name", account.name)
                put("email", account.email)
                put("password", account.password)
                put("tutor", account.tutor)
                put("rating", account.rating ?: JSONObject.NULL)
                put("studyTime", account.studyTime ?: JSONObject.NULL)
                put("ID", account.ID.toString())
            }
            jsonArray.put(accountJson)

            file.writeText(jsonArray.toString(2), Charsets.UTF_8)  // Write with indentation
            true
        } catch (e: IOException) {
            Log.e("SignupActivity", "Failed to save account data", e)
            false
        }
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
        // SignupActivity does not use the bottom navigation
    }
}
