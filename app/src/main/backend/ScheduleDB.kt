package com.tm.backend


import com.google.gson.reflect.TypeToken
import java.util.UUID

/* ==================================================================================================================

CORE DATABASE STRUCTURE THAT MANAGES ALL SCHEDULES

**PROPERTIES**

- FILEPATH (STRING) : this is the file name / path as a string
- - NOTE : may soon be deprecated as file management is changed into .json files

- SCHEDULES (DICTIONARY[UUID] = SCHEDULE) : this is a KEY VALUE pair data structure
- - KEY : `back end`.Account UUIDs
- - VALUE : schedules

- FILEMANAGER (DATABASEMANAGER) : this is the class used to handle all file management
- - this class performs all reading and writing to / from files
- - a separate class was made to manage these functions so as the database storage methods change, only the 'fileManager' class needs to be revised

 ==================================================================================================================== */

class ScheduleDB(var filepath: String = "")
{
    var schedules: HashMap<UUID, Schedule> = HashMap<UUID, Schedule>()
    var fileManager: DatabaseManager = DatabaseManager()

    val type = object : TypeToken<HashMap<UUID, Schedule>>() {}.type



    // the 'as' is used to cast the object being assigned to 'courses' as the appropriate data structure
    // - fileManager is using one function to read/write all database data, so the cast is mandatory
    fun load() // Load accounts
    {
        schedules = filepath.let { fileManager.load(it, type)!! }
    }

    /**
     * Save / update database
     */
    fun save()
    {
        filepath.let { fileManager.save(it, schedules, type) }
    }

    /**
     * Initializes the schedule object with empty or null values
     * - ID is passed to the 'generateSchedule' function so the ID can be used as the KEY to retrieve the schedule for this.`back end`.Account
     */
    fun generateSchedule(id: UUID)
    {
        var temp: Schedule = Schedule()
        schedules.put(id, temp)
    }


    /**
     * Adds or updates an entire new schedule.
     *
     * @param id  The unique identifier of the account.
     * @param newSched the new schedule to update.
     */
    fun updateSchedule(ID: UUID, newSched: Schedule)
    {
        schedules[ID] = newSched
        //save()
    }

    operator fun get(id: UUID): Schedule?
    {
        return schedules[id]
    }

}