package com.tm.backend


/* ==================================================================================================================

CORE SCHEDULE CLASS THAT TRACKS PERSONAL SCHEDULES AND AVAILABILITY

**PROPERTIES**

- ACCESS (ENUMERATION) : tertiary data type
- - FREE(1) : open availability and no event scheduled
- - BUSY(0) : normally available, but currently an event is scheduled
- - NOT(-1) : blacked out date, never available

- HOURS (DICTIONARY[INT] = ACCESS) : this is a KEY VALUE pair structure
- - KEY : hours in the day (24)
- - VALUE : 'Access'

- SCHEDULE (DICTIONARY[DAY] = DICTIONARY[HOURS] = ACCESS) : this is a KEY VALUE structure where the VALUE is another dictionary with KEY VALUE pairs
- - KEY : days of the week
- - VALUE : KEY VALUE structure
- - - KEY : hours of the day
- - - VALUE : the 'Access' type for every hour

 ==================================================================================================================== */

class Schedule {
    // var hours = HashMap<Int, Access>() // Commented out: old structure for individual hour tracking
    var schedule = HashMap<Int, MutableList<TimeRange>>() // New structure for range-based availability

    init {
        // Initializes the new structure
        val day = (1..7).toList()

        for (d in day) {
            schedule[d] = mutableListOf() // Initialize the new structure with empty lists
        }
    }

    // Commented out: old method to set individual hour access
    // fun setAccess(day: Int, hour: Int, avail: Access) {
    //     hours[hour] = avail
    // }

    fun setAccess(day: Int, start: Int, end: Int, status: Access) {
        // New method to set range-based access
        val timeRange = TimeRange(start, end, status)
        schedule.getOrPut(day) { mutableListOf() }.add(timeRange)
    }

    // Commented out: old method to get access by hour as integer
    // fun getAccessByInt(day: Int, hour: Int): Int? {
    //     return schedule[day]?.find { hour in it.start until it.end }?.status?.getAccess()
    // }

    // Commented out: old method to get access by hour as Access enum
    // fun getAccessByVal(day: Int, hour: Int): Access? {
    //     return schedule[day]?.find { hour in it.start until it.end }?.status
    // }

    // Commented out: example method to create a session (booking an hour)
    // fun createSession(day: Int, hour: Int): Boolean {
    //     val access = getAccessByVal(day, hour)
    //     if (access == Access.FREE) {
    //         setAccess(day, hour, Access.BUSY)
    //         return true
    //     }
    //     return false
    // }

    enum class Access(val availability: Int) {
        FREE(1),
        BUSY(0),
        NOT(-1);

        fun getAccess(): Int {
            return availability
        }
    }

    /**
     * Time range during a specific day with availability status.
     *
     * @property start The beginning hour of the time range (0-23).
     * @property end The ending hour of the time range (0-23), exclusive. For example, if the end is 9, it means up to but not including hour 9 itself.
     * @property status The availability status for this time range, which can be FREE, BUSY, or NOT.
     *                 FREE indicates the time range is available for booking or scheduling,
     *                 BUSY indicates the time range is already booked or not available for scheduling,
     *                 NOT indicates the time range is blocked and cannot be used for scheduling.
     */
    data class TimeRange(val start: Int, var end: Int, var status: Access)


    fun update(newSched: HashMap<Int, MutableList<TimeRange>>)
    {
        this.schedule = newSched
        //save()
    }

}
