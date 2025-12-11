package com.tm.frontend

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tm.R
import com.tm.backend.Account
import com.tm.backend.Course
import java.io.File
import java.io.IOException
import java.util.UUID

class TutorListActivity : AppCompatActivity() {

    private lateinit var tutorListView: ListView
    private var courseTitle: String? = null

    /**
     * Populates the list of tutors based on the course title received from the intent.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutor_list)

        tutorListView = findViewById(R.id.tutorListView)
        courseTitle = intent.getStringExtra("COURSE_TITLE")

        if (courseTitle.isNullOrEmpty()) {
            Log.d("TutorListActivity", "No course title provided.")
            finish()
            return
        }

        val tutors = loadTutorsForCourse(courseTitle)

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, tutors.map { it.name })
        tutorListView.adapter = adapter

        tutorListView.setOnItemClickListener { _, _, position, _ ->
            val selectedTutor = tutors[position]
            showTutorProfile(selectedTutor)
        }
    }

    /**
     * Loads the list of tutors teaching a specific course.
     * @param courseTitle The title of the course for which tutors are to be loaded.
     * @return A list of Account objects representing the tutors for the given course.
     */
    private fun loadTutorsForCourse(courseTitle: String?): List<Account> {
        Log.d("TutorListActivity", "Loading tutors for course title: $courseTitle")
        if (courseTitle.isNullOrEmpty()) return emptyList()

        val accounts = loadAccounts().filter { it.tutor }
        val coursesDb = loadCoursesDb()

        return accounts.filter { account ->
            val hasCourse = coursesDb[account.ID]?.any { course ->
                Log.d("TutorListActivity", "Checking course ${course.title} for tutor ${account.name}")
                course.title.equals(courseTitle, ignoreCase = true)
            } ?: false
            Log.d("TutorListActivity", "Tutor ${account.name} teaches the course: $hasCourse")
            hasCourse
        }
    }

    /**
     * Loads the list of all accounts from a JSON file in internal storage.
     * @return A list of Account objects representing all accounts.
     */
    private fun loadAccounts(): List<Account> {
        val file = File(filesDir, "account_db.json")
        if (!file.exists()) {
            Log.e("DataLoader", "Account file does not exist")
            return emptyList()
        }

        val json = try {
            file.bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            return emptyList()
        }

        val gson = Gson()
        val type = object : TypeToken<List<Account>>() {}.type
        return gson.fromJson(json, type)
    }


    /**
     * Loads the course database from a JSON file.
     * @return A map of course IDs to Course objects representing all courses.
     */
    private fun loadCoursesDb(): Map<UUID, List<Course>> {
        val file = File(filesDir, "course_db.json")
        if (!file.exists()) {
            Log.e("CalendarActivity", "Course file does not exist")
            return emptyMap()
        }

        val json = try {
            file.bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            return emptyMap()
        }

        val gson = Gson()
        val type = object : TypeToken<HashMap<UUID, ArrayList<Course>>>() {}.type
        return gson.fromJson(json, type)
    }


    /**
     * Shows a dialog with the profile of a selected tutor.
     * @param tutor The Account object representing the tutor whose profile is to be displayed.
     */
    private fun showTutorProfile(tutor: Account) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.other_profile)

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val width = displayMetrics.widthPixels
        val height = displayMetrics.heightPixels

        dialog.window?.setLayout((width * 0.9).toInt(), (height * 0.8).toInt())

        dialog.findViewById<TextView>(R.id.nameTextView).text = tutor.name
        dialog.findViewById<TextView>(R.id.roleTextView).text = if (tutor.tutor) "Tutor" else "Student"

        dialog.findViewById<ImageView>(R.id.backButton).setOnClickListener {
            dialog.dismiss()
        }

        dialog.findViewById<Button>(R.id.scheduleButton).setOnClickListener {
            // Retrieve course details from the current activity's intent
            val courseTitle = intent.getStringExtra("COURSE_TITLE")
            val courseNum = intent.getStringExtra("COURSE_NUM")

            // Create a new intent for CalendarActivity and pass the tutor's ID and course details
            val calendarIntent = Intent(this, CalendarActivity::class.java)
            calendarIntent.putExtra("TUTOR_ID", tutor.ID.toString())
            calendarIntent.putExtra("COURSE_TITLE", courseTitle)
            calendarIntent.putExtra("COURSE_NUM", courseNum)
            startActivity(calendarIntent)
            dialog.dismiss()
        }

        dialog.show()
    }
}