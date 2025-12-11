package com.tm.frontend


import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.tm.R
import com.tm.backend.Course
import com.tm.backend.Schedule
import java.io.IOException
import java.util.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.nio.charset.Charset


class WeekViewActivity : BaseActivity() {
    private var calendar: Calendar = Calendar.getInstance()
    private var isWeekView: Boolean = false
    private lateinit var weekLayout: GridLayout
    private var tutorSchedule: Schedule? = null
    private var userSchedule: Schedule? = null


    private var currentSelectedDay: String = ""
    private var pickingStartTime: Boolean = true
    private var selectedTimes: MutableMap<String, MutableList<Pair<String, String>>> = mutableMapOf()
    private var tempStartTime: String = ""


    private var tutorId: String? = null
    private var studentId: String? = null
    private var userId: String? = null
    private var course: Course? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("CalendarActivity", "onCreate started")

        userId = getCurrentUserUUID()
        isWeekView = true // Always set isWeekView to true, ignoring the intent extra
        setContentView(R.layout.activity_week_view) // Always use the week view layout

        tutorId = intent.getStringExtra("TUTOR_ID")
        studentId = getCurrentUserUUID()

        Log.d("CalendarActivity", "Tutor ID: $tutorId, Student ID: $studentId")

        val courseTitle = intent.getStringExtra("COURSE_TITLE")
        val courseNum = intent.getStringExtra("COURSE_NUM")

        course = if (courseTitle != null && courseNum != null) {
            loadCourse(courseTitle, courseNum)
        } else {
            fetchLatestCourseForUser(studentId!!)
        }

        if (course == null) {
            Log.e("CalendarActivity", "No course data could be loaded.")
            finish()
            return
        }

        Log.d("CalendarActivity", "Loaded Course: $course")
        tutorSchedule = tutorId?.let { loadTutorSchedule(it) }

        Log.d("CalendarActivity", "Week View Selected")
        tutorSchedule = tutorId?.let { loadTutorSchedule(it) } ?: Schedule().also {
            Log.e("CalendarActivity", "Failed to load tutor schedule.")
        }
        setupWeekView()
    }

    /**
     * Loads a specific course based on the course title and number from local storage.
     * @param title The title of the course.
     * @param num The course number as a string.
     * @return The loaded Course object, or null if not found.
     */
    private fun loadCourse(title: String, num: String): Course? {
        val fileName = "course_db.json"
        val file = File(filesDir, fileName)
        if (!file.exists()) return null


        val json = JSONObject(file.readText(Charset.forName("UTF-8")))
        for (userUUID in json.keys()) {
            val coursesArray = json.optJSONArray(userUUID)
            if (coursesArray != null) {
                for (i in 0 until coursesArray.length()) { // Safely using length() directly
                    val courseObj = coursesArray.getJSONObject(i)
                    if (courseObj.optString("title") == title && courseObj.optInt("courseNum").toString() == num) {
                        return Course(
                            field = courseObj.optString("field", ""),
                            courseNum = courseObj.optInt("courseNum"),
                            title = courseObj.optString("title", "")
                        )
                    }
                }
            }
        }
        return null
    }


    /**
     * Loads the user's schedule from a JSON file based on their UUID.
     */
    /**
     * Loads the user's schedule from a JSON file based on their UUID.
     */
    private fun loadUserSchedule() {
        val userId = this.userId ?: run {
            Log.e("CalendarActivity", "UserID is null")
            return
        }
        Log.d("CalendarActivity", "Loading schedule for user ID: $userId")

        try {
            val file = File(filesDir, "schedule_db.json")
            if (!file.exists()) {
                Log.e("CalendarActivity", "Schedule file does not exist")
                return
            }

            val jsonString = file.readText(Charset.forName("UTF-8"))
            val jsonObject = JSONObject(jsonString)

            // Directly attempt to get the user's schedule using their userId
            val userJson = jsonObject.optJSONObject(userId)
            if (userJson == null) {
                Log.e("CalendarActivity", "No schedule found for user in JSON")
                return
            }

            userSchedule = Schedule()
            Log.d("CalendarActivity", "Initializing Schedule object for user")

            for (day in 1..7) {
                val dayArray = userJson.optJSONArray(day.toString())
                if (dayArray == null) {
                    Log.d("CalendarActivity", "No data for day $day")
                    continue
                }

                for (i in 0 until dayArray.length()) {
                    val timeRangeObj = dayArray.getJSONObject(i)
                    val start = timeRangeObj.getInt("start")
                    val end = timeRangeObj.getInt("end")
                    val status = Schedule.Access.valueOf(timeRangeObj.getString("status"))
                    userSchedule?.setAccess(day, start, end, status)
                    Log.d("CalendarActivity", "Loaded time range for day $day: start=$start, end=$end, status=$status")
                }
            }
        } catch (e: JSONException) {
            Log.e("CalendarActivity", "Error parsing schedule JSON", e)
        } catch (e: IOException) {
            Log.e("CalendarActivity", "Error reading schedule file", e)
        }
    }


    /**
     * Retrieves the current user's UUID from shared preferences.
     * @return The UUID of the current user as a String, or an empty string if not found.
     */
    private fun getCurrentUserUUID(): String {
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        return sharedPreferences.getString("userUUID", "") ?: ""
    }

    /**
     * Fetches the latest course for a user based on their UUID from local storage.
     * @param userUUID The UUID of the user whose latest course is to be fetched.
     * @return The most recent Course object, or null if no courses are found.
     */
    private fun fetchLatestCourseForUser(userUUID: String): Course? {
        val fileName = "course_db.json"
        val file = File(filesDir, fileName)
        if (!file.exists()) return null


        val json = JSONObject(file.readText(Charset.forName("UTF-8")))
        val coursesArray = json.optJSONArray(userUUID)
        if (coursesArray != null && coursesArray.length() > 0) {
            val courseJson = coursesArray.getJSONObject(coursesArray.length() - 1)
            return Course(
                field = courseJson.optString("field", ""),
                courseNum = courseJson.optInt("courseNum", 0),
                title = courseJson.optString("title", "")
            )
        }
        return null
    }


    /**
     * Initializes the main layout container for the activity.
     * @return The main layout as a ViewGroup.
     */
    override fun initializeMainLayout(): ViewGroup = findViewById(R.id.main_layout)


    /**
     * Sets the currently active item in the bottom navigation menu to the calendar item.
     */
    override fun setActiveBottomNavigationItem() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = R.id.navigation_calendar
    }


    /**
     * Loads the tutor's schedule from a JSON file using Gson.
     * @param tutorId The UUID of the tutor as a String.
     * @return The loaded Schedule object, or null if an error occurs.
     */
    private fun loadTutorSchedule(tutorId: String): Schedule? {
        try {
            val json = assets.open("schedule_db.json").bufferedReader().use { it.readText() }
            val type = object : TypeToken<Map<UUID, HashMap<Int, List<Schedule.TimeRange>>>>() {}.type
            return Gson().fromJson<Map<UUID, HashMap<Int, List<Schedule.TimeRange>>>>(json, type)[UUID.fromString(tutorId)]?.let { scheduleData ->
                Schedule().apply {
                    scheduleData.forEach { (day, timeRanges) ->
                        timeRanges.forEach { timeRange ->
                            setAccess(day, timeRange.start, timeRange.end, timeRange.status)
                        }
                    }
                }
            }
        } catch (e: IOException) {
            Log.e("CalendarActivity", "Failed to load tutor schedules", e)
            return null
        }
    }

    private fun saveTutorSchedule(userId: String, schedule: Schedule) {
        val fileName = "schedule_db.json"
        val file = File(filesDir, fileName)

        // Read or initialize the JSON object
        val rootObject = if (file.exists()) {
            JSONObject(file.readText(Charset.forName("UTF-8")))
        } else {
            JSONObject()  // Create a new JSON object if the file does not exist
        }

        // This will directly access or create a JSON object for the userId at the root level
        val userScheduleNode = rootObject.optJSONObject(userId) ?: JSONObject()

        for (day in 1..7) {
            val existingDayArray = userScheduleNode.optJSONArray(day.toString()) ?: JSONArray()
            val timeRanges = schedule.schedule[day] ?: continue  // If no new time ranges, use existing

            val dayArray = mergeTimeRanges(existingDayArray, timeRanges)

            userScheduleNode.put(day.toString(), dayArray)
        }

        // Update the root object directly with the user's schedule
        rootObject.put(userId, userScheduleNode)

        // Write the updated JSON object back to the file
        file.writeText(rootObject.toString(2), Charset.forName("UTF-8"))
    }



    /**
     * Merges new time ranges with existing ones, maintaining any gaps.
     */
    private fun mergeTimeRanges(existingDayArray: JSONArray, newTimeRanges: List<Schedule.TimeRange>): JSONArray {
        val dayArray = JSONArray()
        var lastEnd = 0
        val sortedTimeRanges = newTimeRanges.sortedBy { it.start }

        sortedTimeRanges.forEach { range ->
            if (range.start > lastEnd) {
                // Maintain existing data for gaps or default to "NOT"
                fillGapsWithExisting(dayArray, existingDayArray, lastEnd, range.start)
            }

            // Add the current range and ensure status is "FREE"
            dayArray.put(JSONObject().apply {
                put("start", range.start)
                put("end", range.end)
                put("status", "FREE")  // Ensuring the status is set to FREE instead of BUSY
            })

            lastEnd = range.end
        }

        // Fill in the remaining time up to 23 with existing data or "NOT"
        if (lastEnd < 23) {
            fillGapsWithExisting(dayArray, existingDayArray, lastEnd, 23)
        }
        return dayArray
    }

    /**
     * Fills gaps with existing data if it exists or defaults to "NOT" if no data exists.
     */
    private fun fillGapsWithExisting(dayArray: JSONArray, existingDayArray: JSONArray, start: Int, end: Int) {
        var filled = false
        for (i in 0 until existingDayArray.length()) {
            val obj = existingDayArray.getJSONObject(i)
            val existingStart = obj.getInt("start")
            val existingEnd = obj.getInt("end")
            if (existingStart < end && existingEnd > start) {
                // Adjust start and end to not overwrite existing data
                val newStart = Math.max(start, existingStart)
                val newEnd = Math.min(end, existingEnd)
                dayArray.put(JSONObject().apply {
                    put("start", newStart)
                    put("end", newEnd)
                    put("status", obj.getString("status"))
                })
                filled = true
            }
        }
        if (!filled) {
            dayArray.put(JSONObject().apply {
                put("start", start)
                put("end", end)
                put("status", "NOT")
            })
        }
    }


    private fun setupWeekView() {
        loadUserSchedule()  // Load the schedule before setting up the view

        weekLayout = findViewById(R.id.availability_grid)
        val daysOfWeek = arrayOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        weekLayout.removeAllViews()
        daysOfWeek.forEachIndexed { index, day ->
            val dayIndex = index + 1
            val textView = TextView(this).apply {
                text = buildDayText(day, dayIndex)
                textSize = 20f
                gravity = TextView.TEXT_ALIGNMENT_CENTER
                tag = day
            }
            weekLayout.addView(textView, GridLayout.LayoutParams().apply {
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            })
            textView.setOnClickListener {
                currentSelectedDay = day
                pickingStartTime = true
                showTimePickerDialog()
            }
            textView.setOnLongClickListener {
                showRemoveTimesDialog(day)
                true
            }
        }
    }


    /**
     * Builds the display text for each day, showing free/busy status.
     * @param day String representation of the day.
     * @param dayIndex Index of the day in the week (1-7).
     * @return String with day and status.
     */
    private fun buildDayText(day: String, dayIndex: Int): String {
        val schedule = userSchedule?.schedule?.get(dayIndex)
        if (schedule == null) {
            Log.d("CalendarActivity", "No schedule available for day $dayIndex ($day)")
            return "$day\nNot Entered"
        }


        val statusText = schedule.joinToString("\n") { "${it.start}:00-${it.end}:00 ${it.status}" }
        Log.d("CalendarActivity", "Building text for $day: $statusText")
        return "$day\n$statusText"
    }


    /**
     * Displays a TimePicker dialog for selecting time ranges either for starting or ending times.
     */
    private fun showTimePickerDialog() {
        val timePickerDialog = TimePickerDialog(this, { _, hourOfDay, minute ->
            val time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
            if (pickingStartTime) {
                tempStartTime = time
                pickingStartTime = false
                showTimePickerDialog()
            } else {
                handleTimeSelection(tempStartTime, time)
                pickingStartTime = true
            }
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false)
        timePickerDialog.show()
    }

    /**
     * Handles the selection of start and end times for a session, updating the UI accordingly.
     * @param startTime The start time of the session.
     * @param endTime The end time of the session.
     */
    private fun handleTimeSelection(startTime: String, endTime: String) {
        val timesList = selectedTimes.getOrPut(currentSelectedDay) { mutableListOf() }
        if (timesList.size < 3) {
            timesList.add(startTime to endTime)
            updateDayView(currentSelectedDay)

            // Convert startTime and endTime to integer hours for booking
            val startHour = startTime.substringBefore(':').toInt()
            val endHour = endTime.substringBefore(':').toInt()

            // Assume the day of the week from the currentSelectedDay
            val dayOfWeek = weekLayout.indexOfChild(weekLayout.findViewWithTag<TextView>(currentSelectedDay)) + 1

            // Booking the time slot, ensure tutorSchedule is initialized
            tutorSchedule?.setAccess(dayOfWeek, startHour, endHour, Schedule.Access.BUSY)

            // Ensure there is always an ID to use for saving the schedule
            val userId = tutorId ?: studentId
            if (userId != null) {
                saveTutorSchedule(userId, tutorSchedule!!)
            } else {
                Log.e("CalendarActivity", "No user ID available to save schedule.")
            }
        } else {
            AlertDialog.Builder(this)
                .setTitle("Limit Reached")
                .setMessage("You can only select up to 3 time ranges per day.")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    /**
     * Updates the day view in the week calendar to reflect selected time slots.
     * @param day The day of the week being updated.
     */
    @SuppressLint("SetTextI18n")
    private fun updateDayView(day: String) {
        val dayView: TextView? = weekLayout.findViewWithTag(day)
        val timesText = selectedTimes[day]?.joinToString("\n") { (start, end) -> "$start - $end" } ?: ""
        dayView?.text = "$day\n$timesText"
    }


    /**
     * Shows a dialog to allow the user to remove selected time ranges for a specific day.
     * @param day The day for which times should be removed.
     */
    private fun showRemoveTimesDialog(day: String) {
        val dayIndex = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").indexOf(day) + 1
        val times = userSchedule?.schedule?.get(dayIndex)?.filter { it.status == Schedule.Access.FREE }

        if (times.isNullOrEmpty()) {
            Toast.makeText(this, "No times to remove for $day", Toast.LENGTH_LONG).show()
            return
        }

        val items = times.map { "${it.start}:00 - ${it.end}:00" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Remove Times")
            .setItems(items) { _, which ->
                val selectedTimeRange = times[which]
                removeTime(day, selectedTimeRange.start, selectedTimeRange.end)
            }
            .show()
    }

    private fun removeTime(day: String, startHour: Int, endHour: Int) {
        val dayIndex = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").indexOf(day) + 1
        val schedule = userSchedule?.schedule?.get(dayIndex) ?: return

        // Mark the selected time range as "NOT"
        schedule.find { it.start == startHour && it.end == endHour }?.status = Schedule.Access.NOT

        // Remove and merge time ranges
        val merged = removedMergeTimeRanges(schedule)
        userSchedule?.schedule?.put(dayIndex, merged)

        // Reflect changes in the user interface
        updateDayView(day)

        // Optionally save changes to persistent storage
        userId?.let { saveUserSchedule(it, userSchedule!!) }
    }

    private fun removedMergeTimeRanges(schedule: List<Schedule.TimeRange>): MutableList<Schedule.TimeRange> {
        val merged = mutableListOf<Schedule.TimeRange>()
        val sorted = schedule.sortedBy { it.start }

        var last = sorted.firstOrNull()?.copy() ?: return merged

        sorted.drop(1).forEach { current ->
            if (last.end == current.start && last.status == current.status) {
                last.end = current.end // Extend the last range
            } else {
                merged.add(last) // Add the completed range
                last = current.copy() // Start a new range
            }
        }

        merged.add(last) // Add the last range
        return merged
    }

    private fun saveUserSchedule(userId: String, schedule: Schedule) {
        val fileName = "schedule_db.json"
        val file = File(filesDir, fileName)
        val rootObject = if (file.exists()) JSONObject(file.readText(Charset.forName("UTF-8"))) else JSONObject()
        val schedulesNode = rootObject.optJSONObject("schedules") ?: JSONObject()
        val userScheduleNode = schedulesNode.optJSONObject(userId) ?: JSONObject()

        schedule.schedule.forEach { (dayIndex, timeRanges) ->
            val dayArray = JSONArray()
            timeRanges.forEach {
                val obj = JSONObject().apply {
                    put("start", it.start)
                    put("end", it.end)
                    put("status", it.status.name)
                }
                dayArray.put(obj)
            }
            userScheduleNode.put(dayIndex.toString(), dayArray)
        }

        schedulesNode.put(userId, userScheduleNode)
        rootObject.put("schedules", schedulesNode)

        file.writeText(rootObject.toString(2), Charset.forName("UTF-8"))
    }
}


