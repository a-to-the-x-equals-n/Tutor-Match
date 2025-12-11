package com.tm.frontend


import android.annotation.SuppressLint
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.GridLayout
import android.widget.GridView
import android.widget.ImageButton
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
import java.text.SimpleDateFormat
import java.time.ZonedDateTime
import java.util.*
import com.tm.backend.Session
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.nio.charset.Charset
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max


class CalendarActivity : BaseActivity() {
    private var calendar: Calendar = Calendar.getInstance()
    private lateinit var dateFormat: SimpleDateFormat
    private lateinit var tvDate: TextView
    private lateinit var weekLayout: GridLayout
    private lateinit var calendarGrid: GridView
    private var dates = ArrayList<String>()
    private var tutorSchedule: Schedule? = null
    private var userSchedule: Schedule? = null

    private var currentSelectedDay: String = ""
    private var pickingStartTime: Boolean = true
    private var selectedTimes: MutableMap<String, MutableList<Pair<String, String>>> = mutableMapOf()
    private var tempStartTime: String = ""

    private var tutorId: String? = null
    private var studentId: String? = null
    private var tutorSessions: List<Session> = listOf()
    private var userId: String? = null
    private var course: Course? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("CalendarActivity", "onCreate started")

        userId = getCurrentUserUUID()
        setContentView(R.layout.activity_calendar) // Always use the month view layout

        studentId = getCurrentUserUUID()
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
        tutorId = intent.getStringExtra("TUTOR_ID")

        // Conditional loading based on the presence of tutorId
        if (tutorId != null) {
            tutorSessions = loadTutorSessions(tutorId!!)
            tutorSchedule = loadTutorSchedule(tutorId!!)
        } else {
            Log.e("CalendarActivity", "Tutor ID is required for month view.")
            Toast.makeText(this, "Error: Tutor ID is required for month view.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupMonthView()
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
            val schedulesNode = jsonObject.optJSONObject("schedules")
            val userJson = schedulesNode?.optJSONObject(userId)
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
    private fun loadTutorSchedule(tutorId: String): Schedule {
        val file = File(filesDir, "schedule_db.json")
        if (!file.exists()) {
            Log.e("CalendarActivity", "Schedule file does not exist")
            return createDefaultSchedule()
        }

        return try {
            val json = file.bufferedReader().use { it.readText() }
            val type = object : TypeToken<Map<UUID, HashMap<Int, List<Schedule.TimeRange>>>>() {}.type
            Gson().fromJson<Map<UUID, HashMap<Int, List<Schedule.TimeRange>>>>(json, type)[UUID.fromString(tutorId)]?.let { scheduleData ->
                Schedule().apply {
                    scheduleData.forEach { (day, timeRanges) ->
                        timeRanges.forEach { timeRange ->
                            setAccess(day, timeRange.start, timeRange.end, timeRange.status)
                        }
                    }
                }
            } ?: createDefaultSchedule()
        } catch (e: IOException) {
            Log.e("CalendarActivity", "Failed to load tutor schedules", e)
            createDefaultSchedule()
        }
    }


    private fun createDefaultSchedule(): Schedule {
        return Schedule().apply {
            for (day in 1..7) {
                setAccess(day, 0, 24, Schedule.Access.NOT) // Every day marked as NOT available
            }
        }
    }




    /**
     * Saves the tutor's schedule JSON file.
     * @param userId The ID of the user whose schedule is being saved.
     * @param schedule The Schedule object to be saved.
     * Ensures that the schedule is merged correctly with any existing data and writes back to the file.
     */
    private fun saveTutorSchedule(userId: String, schedule: Schedule) {
        val fileName = "schedule_db.json"
        val file = File(filesDir, fileName)


        // Read or initialize the JSON object
        val rootObject = if (file.exists()) {
            JSONObject(file.readText(Charset.forName("UTF-8")))
        } else {
            JSONObject()  // Create a new JSON object if the file does not exist
        }


        val schedulesNode = rootObject.optJSONObject("schedules") ?: JSONObject()
        val userScheduleNode = schedulesNode.optJSONObject(userId) ?: JSONObject()


        for (day in 1..7) {
            val existingDayArray = userScheduleNode.optJSONArray(day.toString()) ?: JSONArray()
            val timeRanges = schedule.schedule[day] ?: continue  // If no new time ranges, use existing


            // Convert JSONArray to List<Schedule.TimeRange>
            val existingRanges = mutableListOf<Schedule.TimeRange>()
            for (i in 0 until existingDayArray.length()) {
                existingDayArray.getJSONObject(i).let {
                    existingRanges.add(Schedule.TimeRange(
                        it.getInt("start"),
                        it.getInt("end"),
                        Schedule.Access.valueOf(it.getString("status"))
                    ))
                }
            }


            // Merge the time ranges
            val mergedRanges = mergeTimeRanges(existingRanges, timeRanges)


            // Convert List<Schedule.TimeRange> back to JSONArray
            val mergedDayArray = JSONArray()
            mergedRanges.forEach {
                mergedDayArray.put(JSONObject().apply {
                    put("start", it.start)
                    put("end", it.end)
                    put("status", it.status.name)
                })
            }


            userScheduleNode.put(day.toString(), mergedDayArray)
        }


        // Update the schedules node and the root object
        schedulesNode.put(userId, userScheduleNode)
        rootObject.put("schedules", schedulesNode)


        // Write the updated JSON object back to the file
        file.writeText(rootObject.toString(2), Charset.forName("UTF-8"))
    }




    /**
     * Merges new time ranges with existing ones, ensuring correct placement of 'BUSY' times.
     */
    private fun mergeTimeRanges(existingRanges: List<Schedule.TimeRange>, newRanges: List<Schedule.TimeRange>): List<Schedule.TimeRange> {
        val allRanges = (existingRanges + newRanges).sortedBy { it.start }
        val mergedRanges = mutableListOf<Schedule.TimeRange>()


        var lastEnd = 0
        allRanges.forEach { currentRange ->
            if (currentRange.start > lastEnd) {
                // Fill gap with NOT if there's space between the last end and the current start
                mergedRanges.add(Schedule.TimeRange(lastEnd, currentRange.start, Schedule.Access.NOT))
            }
            if (mergedRanges.isNotEmpty() && mergedRanges.last().end > currentRange.start) {
                // Current range starts before the last range ends; adjust last range end
                mergedRanges.last().end = currentRange.start
            }


            if (currentRange.end > lastEnd) {
                // Add or extend the current range
                if (mergedRanges.isNotEmpty() && mergedRanges.last().start == currentRange.start) {
                    // Last range starts at the same point as current, adjust the status if needed
                    if (mergedRanges.last().status == Schedule.Access.NOT && currentRange.status == Schedule.Access.BUSY) {
                        mergedRanges.last().status = currentRange.status
                        mergedRanges.last().end = currentRange.end
                    } else {
                        mergedRanges.last().end = max(mergedRanges.last().end, currentRange.end)
                    }
                } else {
                    // No overlap or adjacent, add new range
                    mergedRanges.add(currentRange)
                }
            }
            lastEnd = max(lastEnd, currentRange.end)
        }


        // Ensure covering the whole day till 24:00 with NOT if necessary
        if (lastEnd < 24) {
            mergedRanges.add(Schedule.TimeRange(lastEnd, 24, Schedule.Access.NOT))
        }


        return mergedRanges
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








    private fun loadTutorSessions(tutorId: String): List<Session> {
        val file = File(filesDir, "session_db.json")
        if (!file.exists()) return emptyList()


        val jsonStr = file.readText(Charset.forName("UTF-8"))
        val rootObject = JSONObject(jsonStr)
        val tutorSessionsNode = rootObject.optJSONObject("tutors")?.optJSONObject(tutorId)?.optJSONObject("sessions") ?: return emptyList()


        return tutorSessionsNode.keys().asSequence().mapNotNull { sessionId ->
            val sessionJson = tutorSessionsNode.getJSONObject(sessionId)
            if (!sessionJson.optBoolean("sessionComplete")) {
                parseSession(sessionId, sessionJson)
            } else null
        }.toList()
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
     * Sets up the view for the month calendar.
     */
    private fun setupMonthView() {
        calendar = Calendar.getInstance()
        tvDate = findViewById(R.id.tv_date)
        dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        calendarGrid = findViewById(R.id.calendar_grid)
        loadAndUpdateCalendar()


        findViewById<ImageButton>(R.id.btn_previous).setOnClickListener {
            calendar.add(Calendar.MONTH, -1)
            loadAndUpdateCalendar()
        }
        findViewById<ImageButton>(R.id.btn_next).setOnClickListener {
            calendar.add(Calendar.MONTH, 1)
            loadAndUpdateCalendar()
        }
    }


    private fun loadAndUpdateCalendar() {
        tutorId?.let {
            val loadedSchedule = loadTutorSchedule(it)
            val loadedSessions = loadTutorSessions(it)
            updateMonthCalendar(loadedSchedule, loadedSessions)
        }
    }


    /**
     * Sets up the view for the week calendar.
     */
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
     * Updates the month view calendar with days and availability based on the tutor's schedule.
     */
    private fun updateMonthCalendar(tutorSchedule: Schedule?, tutorSessions: List<Session>) {
        dates.clear()
        val currentDate = Calendar.getInstance()
        val endDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 30) }


        val monthStart = calendar.clone() as Calendar
        monthStart.set(Calendar.DAY_OF_MONTH, 1)
        val monthBeginningCell = monthStart.get(Calendar.DAY_OF_WEEK) - 1
        val daysInMonth = monthStart.getActualMaximum(Calendar.DAY_OF_MONTH)


        for (i in 0 until monthBeginningCell) {
            dates.add("")
        }
        for (dayOfMonth in 1..daysInMonth) {
            monthStart.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            dates.add(dayOfMonth.toString())
        }


        val adapter = object : ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dates) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val textView = view.findViewById<TextView>(android.R.id.text1)
                val adjustedPosition = position - monthBeginningCell + 1


                if (adjustedPosition in 1..daysInMonth) {
                    monthStart.set(Calendar.DAY_OF_MONTH, adjustedPosition)
                    val dayOfWeek = monthStart.get(Calendar.DAY_OF_WEEK)


                    // Determine if the date is within the next 30 days
                    val isWithin30Days = monthStart.before(endDate) && monthStart.after(currentDate)
                    val dateKey = SimpleDateFormat("yyyy-MM-dd").format(monthStart.time)


                    // Fetch the sessions for this particular day
                    val daySessions = tutorSessions.filter { it.startDate.toLocalDate().toString() == dateKey }


                    // Check availability from the schedule
                    val daySchedules = tutorSchedule?.schedule?.get(dayOfWeek)
                    val isCompletelyBooked = daySessions.all { it.sessionComplete } && daySessions.isNotEmpty()


                    if (isCompletelyBooked) {
                        textView.setBackgroundColor(Color.RED)  // Red if completely booked
                    } else if (daySessions.isNotEmpty()) {
                        textView.setBackgroundColor(Color.YELLOW)  // Yellow if partially booked
                    } else if (isWithin30Days && daySchedules?.any { it.status == Schedule.Access.FREE } == true) {
                        textView.setBackgroundColor(Color.GREEN)  // Green if free
                    } else {
                        textView.setBackgroundColor(Color.TRANSPARENT)  // Transparent if not within the next 30 days
                    }
                } else {
                    textView.setBackgroundColor(Color.TRANSPARENT)
                }
                return view
            }
        }


        calendarGrid.adapter = adapter
        tvDate.text = dateFormat.format(calendar.time)


        calendarGrid.setOnItemClickListener { _, _, position, _ ->
            val adjustedPosition = position - monthBeginningCell + 1
            if (adjustedPosition in 1..daysInMonth) {
                monthStart.set(Calendar.DAY_OF_MONTH, adjustedPosition)
                val dayOfWeek = monthStart.get(Calendar.DAY_OF_WEEK)
                val isWithin30Days = monthStart.before(endDate) && monthStart.after(currentDate)
                val daySchedules = tutorSchedule?.schedule?.get(dayOfWeek)
                val isFree = isWithin30Days && daySchedules?.any { it.status == Schedule.Access.FREE } == true


                if (isFree) {
                    showTimePickerDialogForBooking(adjustedPosition)
                } else if (isWithin30Days) { // Check if the day is within the next 30 days but not free
                    AlertDialog.Builder(this)
                        .setTitle("Unavailable")
                        .setMessage("No free slots available on this day.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }
        }
    }








    /**
     * Shows a dialog to pick a time slot for booking based on available slots on a specific day.
     * @param dayOfMonth The day of the month for which the booking is to be made.
     */
    @SuppressLint("SimpleDateFormat")
    private fun showTimePickerDialogForBooking(dayOfMonth: Int) {
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val dateKey = SimpleDateFormat("yyyy-MM-dd").format(calendar.time)

        // Load active sessions for this specific day
        val daySessions = tutorSessions.filter {
            it.startDate.toLocalDate().toString() == dateKey && !it.sessionComplete
        }

        // Start with all free time ranges for that day of the week
        var freeTimeRanges = tutorSchedule?.schedule?.get(dayOfWeek)?.filter { it.status == Schedule.Access.FREE } ?: listOf()

        // Remove time ranges that overlap with active sessions
        daySessions.forEach { session ->
            val sessionStart = session.startDate.toLocalTime().hour
            val sessionEnd = sessionStart + session.sessionLength

            freeTimeRanges = freeTimeRanges.filterNot { range ->
                // Check if the session time overlaps with the free time range
                sessionStart < range.end && sessionEnd > range.start
            }
        }

        // Prepare the dialog items
        val timeSlots = freeTimeRanges.map { "${it.start}:00 - ${it.end}:00" }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select a Time Slot")
            .setItems(timeSlots) { dialog, which ->
                val selectedTimeRange = freeTimeRanges[which]
                if (isBookingDataComplete()) {
                    bookTimeSlot(selectedTimeRange, dayOfMonth)
                } else {
                    Toast.makeText(this, "Missing booking information. Please ensure all details are filled.", Toast.LENGTH_LONG).show()
                }
            }
            .show()
    }

    /**
     * Displays a TimePicker dialog for selecting time ranges either for starting or ending times.
     */
    private fun showTimePickerDialog() {
        val timePickerDialog = TimePickerDialog(this, TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute ->
            val time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
            if (pickingStartTime) {
                tempStartTime = time
                pickingStartTime = false
                showTimePickerDialog()  // Show again for end time
            } else {
                handleTimeSelection(tempStartTime, time)
                pickingStartTime = true  // Reset for next use
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
        if (currentSelectedDay.isEmpty()) {
            Log.e("CalendarActivity", "handleTimeSelection: Missing currentSelectedDay.")
            return
        }

        val timesList = selectedTimes.getOrPut(currentSelectedDay) { mutableListOf() }
        if (timesList.size < 3) {
            timesList.add(startTime to endTime)
            updateDayView(currentSelectedDay)

            val startHour = startTime.substringBefore(':').toInt()
            val endHour = endTime.substringBefore(':').toInt()
            val dayOfWeek = weekLayout.indexOfChild(weekLayout.findViewWithTag<TextView>(currentSelectedDay)) + 1

            // Ensure tutorSchedule is initialized, create a new one if null
            if (tutorSchedule == null) {
                tutorSchedule = Schedule() // Potentially initialize with default 'NOT' status for all days
                // Initialize all days to NOT available
                for (i in 1..7) {
                    tutorSchedule?.setAccess(i, 0, 24, Schedule.Access.NOT)
                }
            }

            tutorSchedule?.setAccess(dayOfWeek, startHour, endHour, Schedule.Access.BUSY)

            val userId = tutorId ?: studentId
            if (userId != null) {
                tutorSchedule?.let { saveTutorSchedule(userId, it) }
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
        val times = selectedTimes[day] ?: return
        val items = times.map { (start, end) -> "$start - $end" }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Remove Times")
            .setItems(items) { _, which ->
                times.removeAt(which)
                updateDayView(day)
            }
            .show()
    }


    /**
     * Checks if all required data for booking a session is complete.
     * @return True if all data is complete, False otherwise.
     */
    private fun isBookingDataComplete(): Boolean = tutorId != null && studentId != null && course != null


    /**
     * Books a time slot for a session and updates the session database.
     * @param timeRange The time range selected for booking the session.
     */
    private fun bookTimeSlot(timeRange: Schedule.TimeRange, dayOfMonth: Int) {
        if (!isBookingDataComplete()) {
            Toast.makeText(this, "Booking failed due to incomplete information.", Toast.LENGTH_SHORT).show()
            return
        }


        // Creating the start date and time from the selected day and time range
        val selectedDate = calendar.time.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().withDayOfMonth(dayOfMonth)
        val selectedTime = LocalTime.of(timeRange.start, 0)
        val startDateTime = ZonedDateTime.of(selectedDate, selectedTime, ZoneId.systemDefault())


        val session = Session(
            tutorID = tutorId!!,
            studentID = studentId!!,
            startDate = startDateTime,
            course = course!!,
            sessionLength = timeRange.end - timeRange.start,
            sessionComplete = false
        )


        saveSession(session)
        AlertDialog.Builder(this)
            .setTitle("Booking Confirmed")
            .setMessage("Session booked from ${timeRange.start}:00 to ${timeRange.end}:00 on ${startDateTime.toLocalDate()}")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                startActivity(Intent(this, SessionsActivity::class.java))
            }
            .show()
    }


    /**
     * Saves a new or updated session to the local JSON database.
     * @param session The Session object to be saved.
     */
    private fun saveSession(session: Session) {
        if (!isBookingDataComplete()) {
            Log.e("CalendarActivity", "Booking information is incomplete.")
            return
        }


        val fileName = "session_db.json"
        val file = File(filesDir, fileName)
        val rootObject = if (file.exists()) {
            JSONObject(file.readText(Charset.forName("UTF-8")))
        } else {
            JSONObject()
        }


        // Initialize or retrieve existing nodes
        val studentsNode = rootObject.optJSONObject("students") ?: JSONObject()
        val tutorsNode = rootObject.optJSONObject("tutors") ?: JSONObject()


        val studentSessionsNode = studentsNode.optJSONObject(session.studentID)?.optJSONObject("sessions") ?: JSONObject()
        val tutorSessionsNode = tutorsNode.optJSONObject(session.tutorID)?.optJSONObject("sessions") ?: JSONObject()


        // Create session details for the student section
        val studentSessionDetails = JSONObject().apply {
            put("tutorID", session.tutorID)
            put("date", session.startDate.toLocalDate().toString())
            put("time", session.startDate.hour.toString())
            put("course", JSONObject().apply {
                put("field", session.course.field)
                put("courseNum", session.course.courseNum)
                put("title", session.course.title)
            })
            put("sessionLength", session.sessionLength)
            put("sessionComplete", session.sessionComplete)
            put("rating", session.rating ?: JSONObject.NULL)
        }


        // Create session details for the tutor section with "studentID" at the top
        val tutorSessionDetails = JSONObject().apply {
            put("studentID", session.studentID) // Placing "studentID" at the top
            put("date", session.startDate.toLocalDate().toString())
            put("time", session.startDate.hour.toString())
            put("course", JSONObject().apply {
                put("field", session.course.field)
                put("courseNum", session.course.courseNum)
                put("title", session.course.title)
            })
            put("sessionLength", session.sessionLength)
            put("sessionComplete", session.sessionComplete)
            put("rating", session.rating ?: JSONObject.NULL)
        }


        // Use an existing session ID or generate a new one if necessary
        val sessionId = session.id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        studentSessionsNode.put(sessionId, studentSessionDetails)
        tutorSessionsNode.put(sessionId, tutorSessionDetails)


        // Reinsert the sessions nodes back under the respective user nodes
        val studentUserNode = studentsNode.optJSONObject(session.studentID) ?: JSONObject()
        studentUserNode.put("sessions", studentSessionsNode)
        studentsNode.put(session.studentID, studentUserNode)


        val tutorUserNode = tutorsNode.optJSONObject(session.tutorID) ?: JSONObject()
        tutorUserNode.put("sessions", tutorSessionsNode)
        tutorsNode.put(session.tutorID, tutorUserNode)


        rootObject.put("students", studentsNode)
        rootObject.put("tutors", tutorsNode)


        // Write the formatted JSON to the file using 2-space indentation
        file.writeText(rootObject.toString(2), Charset.forName("UTF-8"))
    }
}
