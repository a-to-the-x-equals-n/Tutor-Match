package com.tm.backend


fun main()
{
    val dbm = DatabaseManager()

    // name, email, password, tutor: Boolean,
    // var account1 = dbm.newUser("name","email", "test", false)
    var account = dbm.login("reinel22@students.ecu.edu", "test")



//    account = dbm.getCourses(account!!)


//    dbm.postCourses(account!!)


    //    -- SCHEDULE TESTING --

//    account = dbm.getSchedules(account!!)
//
//    if (account != null)
//    {
//        println("Schedules created. ${account.mySchedule}")
//    }
//    else
//    {
//        println("Get courses failed.")
//    }
//    dbm.postSchedules(account!!)
}

