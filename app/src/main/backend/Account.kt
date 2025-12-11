package com.tm.backend


import java.util.ArrayList
import java.util.UUID;



/* ==================================================================================================================

CORE DATA STRUCTURE FOR ALL ACCOUNTS/PROFILES WITH TUTOR MATCH

**PROPERTIES**

- NAME (STRING) : this is the first and last name of the user

- EMAIL (STRING) : university issued email

- PASSWORD (STRING) : password used for login

- TUTOR (BOOLEAN) : this is used for the "permission hierarchy"
- - students with this field set FALSE do not have the tutor privileges
- - students with this field set TRUE do have the tutor privileges

- RATING (INTEGER) : this is the running sum of all ratings given to a tutor [1 - 5]

- STUDYTIME (INTEGER) : this is a running sum of all of the sessions attended by this.`back end`.Account

- ID (UUID) : This is a unique ID that is used exclusively by the program to identify users
- - this 'ID' is used as the 'token' to access the other database fields associated with this user

 ==================================================================================================================== */
class Account(var name: String, var email: String, var password: String, var tutor: Boolean, var rating: Float = 0f, var studyTime: Int? = null, var ID: UUID, var token: String)
{

    var mySessions: SessionDB = SessionDB()    // database for all sessions associated with this.`back end`.Account
    var mySchedule: Schedule = Schedule()  // the schedule associated with this.`back end`.Account
    var myCourses: CoursesDB = CoursesDB()     // database for all courses associated with this.`back end`.Account




    /**
     * Initializes the schedule object with empty or null values
     * - ID is passed to the 'generateSchedule' function so the ID can be used as the KEY to retrieve the schedule for this.`back end`.Account
     */
//    fun generateSchedule()
//    {
//        ID.let { mySchedule.generateSchedule(it) }
//    }

    /**
     * Assigns values to this schedule object
     */
    fun updateSchedule(newSched: HashMap<Int, MutableList<Schedule.TimeRange>>)
    {
        mySchedule.update(newSched)
    }

    /**
     * Initializes the courses object with empty or null values
     * - ID is passed to the 'generateCourses' function so the ID can be used as the KEY to retrieve the courses for this.`back end`.Account
     */
    fun generateCourses()
    {
        ID.let { myCourses.generateCourses(it) }
    }

    /**
     * Assigns values to the courses object
     */
    fun updateCourses(newCourses: ArrayList<Course>)
    {
        myCourses.updateCourses(ID, newCourses)
    }

    /**
     * Initializes the sessions object with empty or null values
     * - ID is passed to the 'generateSession' function so the ID can be used as the KEY to retrieve the current sessions for this.`back end`.Account
     */
    fun generateSession()
    {
        mySessions.generateSession(ID)
    }

    /**
     * Assigns values to the sessions object
     */
    fun updateSession(newSession: Session)
    {
        ID.let { mySessions.updateSession(it, newSession) }
    }

    /**
     * Validates password
     *
     * @param password : password to be validated
     * @return boolean : false is password isn't valid; true if valid
     */
//    fun passIsValid(pass: String): Boolean
//    {
//        return (BCrypt.checkpw(pass, hashedPassword))
//    }


    /**
     * Updates this.Rating by adding a value 1-5
     */
    fun addRating(newRating: Int)
    {
        rating += newRating
    }

    /**
     * Returns the average of this.Rating
     * - studyTime is a running sum of all sessions attended
     * - therefore rating / studyTime returns an average rating
     */
    fun getRating(): String
    {
        if(studyTime != null)
        {
            // since studyTime is initialized with a null value, this method of division is SAFE
            return String.format("%.${1}f", rating / studyTime!!) // The use of the '!!' is Kotlin syntax for SAFE CALLS
        }
        return "This Tutor has yet to be rated!"
    }

    /**
     * Increments number of sessions
     * - since studyTime is initialized with a null value, the manner in which studyTime is incremented is SAFE
     */
    fun addTime()
    {
        studyTime = studyTime?.plus(1)
    }


/**
 * Returns this.`back end`.Account as a string
 */
    override fun toString(): String
    {
        return String.format("$name%n$email%n$password%n$tutor%n$rating%n$studyTime%n$ID%n%n")
    }

/**
 * Returns String of public tutor info that is displayed to students
 */
    fun toStringName(): String
    {
        return String.format("Name: $name Email: $email");
    }
}
