package com.tm.frontend

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tm.R
import com.tm.backend.Account
import com.tm.backend.Course
import com.tm.backend.Session
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.charset.Charset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID

class SessionsActivity : BaseActivity() {

    private lateinit var sessionsLayout: LinearLayout
    private lateinit var currentUserRole: String
    private lateinit var currentUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sessions)

        currentUserRole = getCurrentUserRole()
        currentUserId = getCurrentUserUUID()

        sessionsLayout = findViewById(R.id.sessionsLayout)
        val mainLayout = initializeMainLayout()

        Log.d("SessionsActivity", "UUID: $currentUserId, Role: $currentUserRole")

        val currentUser = loadUserAccount("account_db.json", currentUserId)
        currentUser?.let { user ->
            Log.d("SessionsActivity", "User found: ${user.email}, loading sessions now")
            val userSessions = loadSessions("session_db.json", user.ID.toString(), currentUserRole)
            userSessions.forEach { session ->
                val sessionView = LayoutInflater.from(this).inflate(R.layout.session_item, sessionsLayout, false)
                configureSessionView(sessionView, session)
                sessionsLayout.addView(sessionView)
            }
        } ?: Log.d("SessionsActivity", "No user found with the UUID $currentUserId")
    }

    /**
     * Initializes and returns the main layout of the activity.
     * @return The ViewGroup that is the main layout of the activity.
     */
    override fun initializeMainLayout(): ViewGroup = findViewById<ConstraintLayout>(R.id.main_layout)

    /**
     * Sets the active item in the bottom navigation to highlight the current section.
     */
    override fun setActiveBottomNavigationItem() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_sessions
    }

    /**
     * Configures a view for displaying session details within the UI.
     * @param view The view to configure.
     * @param session The session data to display.
     */
    private fun configureSessionView(view: View, session: Session) {
        val sessionInfo = view.findViewById<TextView>(R.id.sessionInfo)
        val deleteButton = view.findViewById<Button>(R.id.deleteSessionButton)

        // Format date and time for display
        val formattedDate = session.startDate.toLocalDate().toString()
        val formattedTime = session.startDate.toLocalTime().truncatedTo(ChronoUnit.HOURS).format(DateTimeFormatter.ofPattern("HH:mm"))

        val sessionText = "Course: ${session.course.title}\nDate: $formattedDate\nTime: $formattedTime\nLength: ${session.sessionLength} hours"
        sessionInfo.text = sessionText

        Log.d("SessionsActivity", "Configuring view for Session ID: ${session.id}")

        deleteButton.setOnClickListener {
            Log.d("SessionsActivity", "Delete button clicked for Session ID: ${session.id}")
            showSessionDeleteDialog(session, view)
        }
    }


    /**
     * Shows a dialog for confirming deletion of a session.
     * @param session The session to potentially delete.
     * @param sessionView The view representing the session in the UI.
     */
    private fun showSessionDeleteDialog(session: Session, sessionView: View) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.session_delete_dialog)
        val confirmButton = dialog.findViewById<Button>(R.id.confirmDeleteButton)
        val cancelButton = dialog.findViewById<Button>(R.id.cancelButton)

        confirmButton.setOnClickListener {
            sessionsLayout.removeView(sessionView)
            removeSessionFromFile(session.id, currentUserRole.toLowerCase(Locale.ROOT) + "s")
            dialog.dismiss()
            Toast.makeText(this, "Session deleted", Toast.LENGTH_SHORT).show()
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * Removes a session from the local file storage based on the session ID.
     * @param sessionId The ID of the session to remove.
     * @param role The role of the user, used to access the correct section of the JSON structure.
     */
    private fun removeSessionFromFile(sessionId: String, role: String) {
        val fileName = "session_db.json"
        val file = File(filesDir, fileName)
        if (!file.exists()) {
            Log.e("SessionsActivity", "File does not exist.")
            return
        }

        val jsonText = file.readText(Charset.forName("UTF-8"))
        val rootObject = JSONObject(jsonText)
        val roleObject = rootObject.optJSONObject(role)
        if (roleObject == null) {
            Log.e("SessionsActivity", "Role $role not found.")
            return
        }

        val userNode = roleObject.optJSONObject(currentUserId)
        if (userNode == null) {
            Log.e("SessionsActivity", "User ID $currentUserId not found under the role $role.")
            return
        }

        val sessionsNode = userNode.optJSONObject("sessions")
        if (sessionsNode == null) {
            Log.e("SessionsActivity", "Sessions node not found for user ID $currentUserId.")
            return
        }

        if (sessionsNode.has(sessionId)) {
            sessionsNode.remove(sessionId)
            file.writeText(rootObject.toString(), Charset.forName("UTF-8"))
            Log.d("SessionsActivity", "Session removed successfully for ID: $sessionId")
        } else {
            Log.e("SessionsActivity", "Session ID $sessionId not found for user ID $currentUserId.")
        }
    }

    /**
     * Loads sessions from a local JSON file based on user ID and role.
     * @param fileName The name of the file containing session data.
     * @param userId The ID of the user whose sessions are to be loaded.
     * @param userRole The role of the user (e.g., "tutor" or "student") which determines the JSON structure.
     * @return A list of Session objects, each representing a user session.
     */
    private fun loadSessions(fileName: String, userId: String, userRole: String): List<Session> {
        val file = File(filesDir, fileName)
        if (!file.exists()) {
            Log.d("SessionsActivity", "No session file exists.")
            return emptyList()
        }

        val jsonStr = file.readText(Charset.forName("UTF-8"))
        val rootObject = JSONObject(jsonStr)
        val roleNode = rootObject.optJSONObject("${userRole}s")

        val userSessions = mutableListOf<Session>()
        roleNode?.optJSONObject(userId)?.let { userNode ->
            val sessionsNode = userNode.optJSONObject("sessions")
            sessionsNode?.keys()?.forEach { sessionId ->
                val sessionJson = sessionsNode.getJSONObject(sessionId)
                userSessions.add(parseSession(sessionId, sessionJson))
            }
        } ?: Log.d("SessionsActivity", "User node for user ID $userId not found.")

        Log.d("SessionsActivity", "Total sessions loaded: ${userSessions.size}")
        return userSessions
    }

    /**
     * Parses a JSON object into a Session object.
     * @param sessionId The ID of the session.
     * @param sessionJson The JSONObject representing session data.
     * @return A Session object containing the parsed data.
     */
    private fun parseSession(sessionId: String, sessionJson: JSONObject): Session {
        val dateStr = sessionJson.optString("date", "")
        val time = sessionJson.optInt("time", 10) // Defaulting to 10 if not found

        // Ensure the time is always two digits; this corrects the formatting issue
        val formattedTime = String.format("%02d:00:00Z", time)
        val dateTimeStr = dateStr + "T" + formattedTime  // Correct concatenation

        // Use the corrected dateTimeStr to parse the ZonedDateTime
        val startDate = if (dateStr.isNotEmpty()) {
            ZonedDateTime.parse(dateTimeStr, DateTimeFormatter.ISO_ZONED_DATE_TIME)
        } else {
            ZonedDateTime.now()  // Default to current time if the date string is empty
        }

        val courseJson = sessionJson.optJSONObject("course")
        val course = Course(
            field = courseJson.optString("field", "Unknown"),
            courseNum = courseJson.optInt("courseNum", 0),
            title = courseJson.optString("title", "Untitled")
        )

        val sessionLength = sessionJson.optInt("sessionLength", 0)
        val sessionComplete = sessionJson.optBoolean("sessionComplete", false)
        val rating = if (sessionJson.isNull("rating")) null else sessionJson.getInt("rating")

        return Session(
            id = sessionId,
            tutorID = sessionJson.optString("tutorID"),
            studentID = sessionJson.optString("studentID"),
            startDate = startDate,
            course = course,
            sessionLength = sessionLength,
            sessionComplete = sessionComplete,
            rating = rating
        )
    }

    /**
     * Loads user account data from a local JSON file.
     * @param fileName The name of the file containing user account data.
     * @param uuid The UUID of the user whose account is to be loaded.
     * @return An Account object if the user is found; otherwise, null.
     */
    private fun loadUserAccount(fileName: String, uuid: String): Account? {
        val file = File(filesDir, fileName)
        if (!file.exists()) return null

        val jsonStr = file.readText(Charset.forName("UTF-8"))
        val jsonArray = JSONArray(jsonStr)

        for (i in 0 until jsonArray.length()) {
            val userJson = jsonArray.getJSONObject(i)
            if (UUID.fromString(userJson.getString("ID")).toString() == uuid) {
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
     * Retrieves the current user's UUID from shared preferences.
     * @return The UUID of the current user as a String.
     */
    private fun getCurrentUserUUID(): String {
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        return sharedPreferences.getString("userUUID", "unknown") ?: "unknown"
    }

    /**
     * Retrieves the current user's UUID from shared preferences.
     * @return The UUID of the current user as a String.
     */
    private fun getCurrentUserRole(): String {
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        return sharedPreferences.getString("userRole", "unknown") ?: "student"
    }
}
