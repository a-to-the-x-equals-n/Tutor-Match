package com.tm.frontend

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.tm.R
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.nio.charset.Charset

class CoursesActivity : BaseActivity() {

    companion object {
        private const val COURSE_REQUEST_CODE = 1
    }

    private lateinit var coursesGrid: GridLayout
    private val courseViews = mutableListOf<TextView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_courses)

        mainLayout = findViewById(R.id.main_layout)
        coursesGrid = findViewById(R.id.coursesGrid)
        loadCoursesFromFile()

        findViewById<ImageView>(R.id.addButton).setOnClickListener {
            val intent = Intent(this, CourseSearchActivity::class.java)
            startActivityForResult(intent, COURSE_REQUEST_CODE)
        }
    }

    /**
     * Initializes and returns the main layout of the activity.
     * @return The ViewGroup that is the main layout of the activity.
     */
    override fun initializeMainLayout(): ViewGroup {
        return findViewById(R.id.main_layout)
    }

    /**
     * Sets the active item in the bottom navigation to highlight the current section.
     */
    override fun setActiveBottomNavigationItem() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_courses
    }

    /**
     * Handles the result returned from launching activities for result, specifically from CourseSearchActivity.
     * @param requestCode The integer request code originally supplied, allowing you to identify who this result came from.
     * @param resultCode The integer result code returned by the child activity through its setResult().
     * @param data An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == COURSE_REQUEST_CODE && resultCode == RESULT_OK) {
            data?.let {
                // Extracting the course field, number, and title from the Intent data.
                val field = it.getStringExtra("COURSE_FIELD") ?: ""
                val title = it.getStringExtra("COURSE_TITLE") ?: ""
                val num = it.getStringExtra("COURSE_NUM") ?: ""

                // Create a JSONObject with the course details
                val newCourse = JSONObject().apply {
                    put("field", field)
                    put("courseNum", num.toIntOrNull() ?: 0)  // Convert num to integer
                    put("title", title)
                }

                // Add this new course to the grid
                addCircleToGrid(newCourse)

                // Save the course to the file
                saveCourseToFile(field, num.toIntOrNull() ?: 0, title)
            }
        }
    }

    /**
     * Adds a visual representation of a course to the grid layout.
     * @param course A JSONObject containing details about the course.
     */
    private fun addCircleToGrid(course: JSONObject) {
        val courseDetails = "${course.getString("field")} ${course.getInt("courseNum")} ${course.getString("title")}"
        val textView = TextView(this).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                width = resources.getDimensionPixelSize(R.dimen.circle_diameter)
                height = resources.getDimensionPixelSize(R.dimen.circle_diameter)
                setMargins(
                    resources.getDimensionPixelSize(R.dimen.circle_margin),
                    resources.getDimensionPixelSize(R.dimen.circle_margin),
                    resources.getDimensionPixelSize(R.dimen.circle_margin),
                    resources.getDimensionPixelSize(R.dimen.circle_margin)
                )
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
            setBackgroundResource(R.drawable.circle_shape)
            text = course.getString("title")  // Display only the title
            setTextColor(Color.WHITE)
            textSize = 12f
            gravity = Gravity.CENTER
            isClickable = true
        }
        textView.setOnClickListener {
            showCourseDetails(courseDetails, textView)
        }
        coursesGrid.addView(textView)
        courseViews.add(textView)
    }


    /**
     * Displays a dialog with detailed information about a course and options to find a tutor or remove the course.
     * @param courseInfo String containing the details of the course.
     * @param courseView The view representing the course in the UI, for potential removal.
     */
    private fun showCourseDetails(courseInfo: String, courseView: TextView) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.course_detail_dialog)

        dialog.findViewById<TextView>(R.id.courseDetailsTextView).text = courseInfo

        val findTutorButton = dialog.findViewById<Button>(R.id.findTutorButton)
        if (getCurrentUserRole() == "tutor") {
            findTutorButton.visibility = View.GONE
        } else {
            findTutorButton.setOnClickListener {
                val parts = courseInfo.split(" ")
                val courseNum = parts[1].toIntOrNull() ?: 0
                val courseTitle = parts.subList(2, parts.size).joinToString(" ")
                val intent = Intent(this, TutorListActivity::class.java)
                intent.putExtra("COURSE_TITLE", courseTitle)
                intent.putExtra("COURSE_NUM", courseNum)
                startActivity(intent)
                dialog.dismiss()
            }
        }

        dialog.findViewById<Button>(R.id.removeCourseButton).setOnClickListener {
            val parts = courseInfo.split(" ")
            val courseTitle = parts.subList(2, parts.size).joinToString(" ")
            removeCourseFromFile(courseTitle)  // Assuming you now want to remove by title
            coursesGrid.removeView(courseView)
            courseViews.remove(courseView)
            dialog.dismiss()
        }
        dialog.show()
    }

    /**
     * Removes a course from the local JSON file based on its title.
     * @param title The title of the course to be removed.
     */
    private fun removeCourseFromFile(title: String) {
        val fileName = "course_db.json"
        val file = File(filesDir, fileName)
        if (!file.exists()) {
            Toast.makeText(this, "Course file does not exist", Toast.LENGTH_SHORT).show()
            return
        }

        val json = JSONObject(file.readText(Charset.forName("UTF-8")))
        val userUUID = getCurrentUserUUID()
        val coursesArray = json.optJSONArray(userUUID) ?: JSONArray()
        val newCoursesArray = JSONArray()

        var removed = false
        for (i in 0 until coursesArray.length()) {
            val course = coursesArray.getJSONObject(i)
            if (course.getString("title").trim() != title.trim()) {
                newCoursesArray.put(course)
            } else {
                removed = true
            }
        }

        if (removed) {
            json.put(userUUID, newCoursesArray)
            file.writeText(json.toString(), Charset.forName("UTF-8"))
            Toast.makeText(this, "Course removed successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "No matching course found", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Retrieves the current user's UUID from SharedPreferences.
     * @return String representing the UUID of the logged-in user.
     */
    private fun getCurrentUserUUID(): String {
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        return sharedPreferences.getString("userUUID", "") ?: ""
    }

    /**
     * Retrieves the current user's UUID from shared preferences.
     * @return The UUID of the current user as a String.
     */
    private fun getCurrentUserRole(): String {
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        return sharedPreferences.getString("userRole", "unknown") ?: "student"
    }

    /**
     * Loads and displays courses from a file specific to the logged-in user.
     */
    private fun loadCoursesFromFile() {
        val fileName = "course_db.json"
        val file = File(filesDir, fileName)
        val userUUID = getCurrentUserUUID()



//        val db = DatabaseManager()
//        var account = db.login(email, password)
//        account = db.getCourses(account)


        if (!file.exists()) {
            Log.e("CoursesActivity", "File does not exist")
            Toast.makeText(this, "Courses file does not exist", Toast.LENGTH_SHORT).show()
            return  // Exits the function if the file does not exist
        }

        try {
            val json = JSONObject(file.readText(Charset.forName("UTF-8")))
            Log.d("CoursesActivity", "File content: ${json.toString(2)}") // Debugging to see file content

            val coursesArray = json.optJSONArray(userUUID) ?: JSONArray()  // Safely attempts to get the JSON array for the user
            Log.d("CoursesActivity", "Found ${coursesArray.length()} courses for user $userUUID")

            for (i in 0 until coursesArray.length()) {
                val course = coursesArray.getJSONObject(i)
                Log.d("CoursesActivity", "Loading course: ${course.getString("title")}") // Log each course being loaded

                addCircleToGrid(course)  // Pass the whole JSONObject
            }
        } catch (e: JSONException) {
            Log.e("CoursesActivity", "Error parsing JSON data", e)
            Toast.makeText(this, "Error loading courses.", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Saves a new course to a JSON file associated with the current user's UUID.
     * @param field The academic field of the course.
     * @param courseNum The course number.
     * @param title The title of the course.
     */
    private fun saveCourseToFile(field: String, courseNum: Int, title: String) {
        val fileName = "course_db.json"
        val file = File(filesDir, fileName)
        val userUUID = getCurrentUserUUID()
        val json = if (file.exists()) JSONObject(file.readText(Charset.forName("UTF-8"))) else JSONObject()
        val coursesArray = json.optJSONArray(userUUID) ?: JSONArray()
        coursesArray.put(JSONObject().apply {
            put("field", field)
            put("courseNum", courseNum)
            put("title", title)
        })
        json.put(userUUID, coursesArray)
        file.writeText(json.toString(), Charset.forName("UTF-8"))
    }

    // Utility to iterate through JSONArray
    private fun JSONArray.forEach(action: (JSONObject) -> Unit) {
        for (i in 0 until length()) {
            action(getJSONObject(i))
        }
    }
}
