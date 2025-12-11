package com.tm.frontend

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.tm.R
import org.json.JSONObject
import java.nio.charset.Charset
import com.tm.backend.Course
import org.json.JSONException

class CourseSearchActivity : AppCompatActivity() {
    // GridLayout to display courses or fields
    private lateinit var coursesGrid: GridLayout
    // List to hold course objects
    private val courses = mutableListOf<Course>()
    var tempField: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_course_search)

        // Initialize the GridLayout and set its column count
        coursesGrid = findViewById(R.id.coursesGrid)
        coursesGrid.columnCount = 3

        // Clear existing courses and load new ones from JSON
        courses.clear()
        loadCourses("course_catalog_db.json").let {
            courses.addAll(it)
        }

        // Populate the grid with unique course fields
        updateGridFields()
    }

    // Update the GridLayout to display unique fields
    private fun updateGridFields() {
        // Remove all views to refresh the grid
        coursesGrid.removeAllViews()
        // Extract unique fields from the courses list
        val uniqueFields = courses.map { it.field }.distinct()
        uniqueFields.forEach { field ->
            // Add each unique field to the grid as a clickable item
            addToGrid(field, true)
        }
    }

    // Update the GridLayout to display courses from a specific field
    private fun updateGridCourses(field: String) {
        // Remove all views to refresh the grid
        coursesGrid.removeAllViews()
        // Filter courses by the selected field and add them to the grid
        courses.filter { it.field == field }.forEach { course ->
            addToGrid("${course.title}\n${course.courseNum}", false, course.title, course.courseNum.toString())
        }
    }

// Load courses from a JSON file
private fun loadCourses(fileName: String): List<Course> {
    // Open the JSON file and read its content
    val jsonStr = this.assets.open(fileName).use { inputStream ->
        inputStream.readBytes().toString(Charset.defaultCharset())
    }
    // Parse the JSON content into a JSONObject
    val jsonObject = JSONObject(jsonStr)
    val loadedCourses = mutableListOf<Course>()

    // Iterate through each field in the JSONObject
    jsonObject.keys().forEach { field ->
        val courseArray = jsonObject.getJSONArray(field)
        for (i in 0 until courseArray.length()) {
            val courseObj = courseArray.getJSONObject(i)
            val courseNum = courseObj.getInt("courseNum")
            val title = courseObj.getString("title")
            // Add the course to the list of loaded courses, using 'field' as the field name
            loadedCourses.add(Course(field, courseNum, title))
        }
    }

    return loadedCourses
}

    // Add a TextView to the GridLayout for a field or course
    private fun addToGrid(field: String, isField: Boolean, title: String = "", courseNum: String = "") {
        val textView = TextView(this).apply {
            layoutParams = GridLayout.LayoutParams().apply {
                // Define size and margins for the TextView
                val sizeInPixels = (100 * resources.displayMetrics.density).toInt()
                width = sizeInPixels
                height = sizeInPixels
                val marginInPixels = (8 * resources.displayMetrics.density).toInt()
                setMargins(marginInPixels, marginInPixels, marginInPixels, marginInPixels)
            }
            gravity = Gravity.CENTER
            // Set background, text, text color, and text size
            setBackgroundResource(R.drawable.circle_shape)
            text = if (isField) field else "$title\n$courseNum"
            setTextColor(android.graphics.Color.WHITE)
            textSize = if (isField) 12f else 10f
        }

        // Common onClickListener setup to handle both field and course selections consistently
        textView.setOnClickListener {
            if (isField) {
                // Handle field click, update courses, and log
                Log.d("CourseSearchActivity", "Field clicked: $field")
                tempField = field;
                updateGridCourses(field)
            } else {
                // Handle course click, prepare intent, and finish
                val data = Intent().apply {
                    putExtra("COURSE_TITLE", title)
                    putExtra("COURSE_NUM", courseNum)
                    putExtra("COURSE_FIELD", tempField)
                }
                Log.d("CourseSearchActivity", "Preparing Intent - Field: $tempField, Title: $title, Num: $courseNum")
                setResult(RESULT_OK, data)
                finish()
            }
        }

        // Add the TextView to the GridLayout
        coursesGrid.addView(textView)
    }
}
