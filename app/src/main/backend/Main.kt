package com.tm.backend
import java.nio.file.Paths

class Main
{

/*===============================================================================================================================================================

        Code has been drastically overhauled and cleaned up

        All databases have been revised and standardized
        - all DBs are now dictionaries, with each `back end`.Account's UUID as the key
        - the UUID is the token to retrieve all databases specific to one '`back end`.Account'

        The newly adapted 'FileManager' class will handle all saving and loading of databases

================================================================================================================================================================*/



    // ABSOLUTE PATHS
    // Directory base path
    private val DIR_PATH = Paths.get("").toAbsolutePath().toString()

    // Database file paths
    val ACCOUNTS_FILE: String = Paths.get(DIR_PATH,"account_db.json").toString()
    val COURSES_FILE: String = Paths.get(DIR_PATH,"course_db.json").toString()
    val SCHEDULES_FILE: String = Paths.get(DIR_PATH,"schedule_db.json").toString()
    val SESSIONS_FILE: String = Paths.get(DIR_PATH,"session_db.json").toString()


    // RUNTIME storage for databases
    var student_database_master: AccountDB? = null
    var course_database_master: CoursesDB? = null
    var schedules_database_master: ScheduleDB? = null
    var sessions_database_master: SessionDB? = null

    // Web Scraping for ECU course catalog
    val URL: String = "https://catalog.ecu.edu/preview_program.php?catoid=28&poid=7403&hl=%22computer+science%22&returnto=search"
    var ecu_master_catalog: CourseCatalog? = null

    fun main(args: Array<String>)
    {
        // __INIT__ student database
        student_database_master = AccountDB(ACCOUNTS_FILE)

        // __INIT__ MASTER course list database
        course_database_master = CoursesDB(COURSES_FILE)

        // __INIT__ MASTER schedule list database
        schedules_database_master = ScheduleDB(SCHEDULES_FILE)

        // __INIT__ MASTER sessions list database
        sessions_database_master = SessionDB(SESSIONS_FILE)

        // __INIT__ ECU Course Catalog from website
        ecu_master_catalog = CourseCatalog(URL)

    }
}