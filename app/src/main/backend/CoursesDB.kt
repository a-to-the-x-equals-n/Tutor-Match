package com.tm.backend

import com.google.gson.reflect.TypeToken
import java.util.ArrayList
import java.util.UUID
import kotlin.collections.*


/* ==================================================================================================================

CORE DATABASE STRUCTURE THAT MANAGES ALL COURSES

**PROPERTIES**

- FILEPATH (STRING) : this is the file name / path as a string
- - NOTE : may soon be deprecated as file management is changed into .json files

- COURSES (DICTIONARY[UUID] = ARRAYLIST<COURSE>) : this is a KEY VALUE pair data structure
- - KEY : `back end`.Account UUIDs
- - VALUE : list of courses

- FILEMANAGER (DATABASEMANAGER) : this is the class used to handle all file management
- - this class performs all reading and writing to / from files
- - a separate class was made to manage these functions so as the database storage methods change, only the 'fileManager' class needs to be revised

 ==================================================================================================================== */

class CoursesDB(val filepath: String = "")
{
    var courses: HashMap<UUID, ArrayList<Course>> = HashMap<UUID, ArrayList<Course>>()
    var fileManager: DatabaseManager = DatabaseManager()
    val type = object : TypeToken<HashMap<UUID, ArrayList<Course>>>() {}.type



    override fun toString(): String
    {
        val stringBuilder = StringBuilder("Courses:\n")
        for ((studentId, courseList) in courses)
        {
            stringBuilder.appendLine("Student ID: $studentId")
            stringBuilder.appendLine("Courses:")
            for (course in courseList)
            {
                stringBuilder.appendLine("$course")
            }
        }
        return stringBuilder.toString()
    }
    /**
     * Loads Course Database
     */
    fun load()
    {
        courses = filepath.let { fileManager.load(it, type)!! }
    }

    /**
     * Saves / updates course database
     */
    fun save()
    {
        filepath.let { fileManager.save(it, courses, type) }
    }

    /**
     * Initializes the courses object with empty or null values
     * - ID is passed to the 'generateCourses' function so the ID can be used as the KEY to retrieve the courses for this.`back end`.Account
     */
    fun generateCourses(id: UUID)
    {
        var temp: ArrayList<Course> = ArrayList<Course>()
        courses.put(id, temp)
    }

    /**
     * Adds or updates a course list for a specific account.
     *
     * @param id  The unique identifier of the account.
     * @param accountCourses The list of courses to associate with the account.
     */
    fun updateCourses(id: UUID, newCourses: ArrayList<Course>)
    {
        courses[id] = newCourses

        // save()
    }

    /**
     * Retrieves the course list for a specific account.
     *
     * @param id The unique identifier of the account.
     * @return The list of courses associated with the specified account, or null if
     *         the account is not found.
     */
    operator fun get(id: UUID): ArrayList<Course>?
    {
        return courses[id]
    }



    /**
     * This function is used for the Student / Tutor pairing process
     * Finds account IDs that have the identical courses in their course list.
     *
     * @param courseName The name of the course to search for.
     * @return A list of account IDs that have the course in their course list. An
     *         empty list is returned if no matches are found.
     */
    fun getAccountsMatchedByCourse(courseName: String): ArrayList<String>
    {
        /* ==================================================================================================

            NOTE : This function needs to be revised / improve upon

        ====================================================================================================*/
        val matchingAccounts: ArrayList<String> = ArrayList<String>()

        for(key in courses.keys)
        {
            val courseList = courses[key]

            if (courseList != null)
            {
                for (course in courseList)
                {
                    if (course.title == courseName)
                    {
                        matchingAccounts.add(key.toString())
                    }
                }
            }
        }
        return matchingAccounts
    }


    /**
     * Removes the course list associated with a specific account.
     *
     * @param id The unique identifier of the account.
     * @return true if the course list was successfully removed, false if the
     *         account does not exist.
     */
    fun removeCourseList(id: UUID): Boolean
    {
        if(courses.containsKey(id))
        {
            courses.remove(id)

            save() // Save the updated course list

            return true // Course list successfully removed.
        }

        return false // `back end`.Account does not exist, course list not removed.
    }
}