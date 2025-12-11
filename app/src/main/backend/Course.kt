package com.tm.backend

/* ==================================================================================================================

CORE COURSE OBJECT FOR ALL COURSES

**PROPERTIES**

- FIELD (STRING) : the department or program descriptor associated with the course

- COURSENUM (INTEGER) : course number for this.Course

- TITLE (STRING) : name of the course

IMPLEMENTS COMPARABLE so the course lists can be sorted if needed

 ==================================================================================================================== */

class Course(val field: String = "CSCI",var courseNum: Int = 0, var title: String = ""): Comparable<Course>
{

    /**
     * Function used for sorting in the collections.sort framework
     */
    override fun compareTo(other: Course): Int
    {
        return courseNum.compareTo(other.courseNum)
    }


    /**
     * Checks this.object to argument object for equality
     * - part of collections framework
     */
    override fun equals(other: Any?): Boolean
    {
        if (other is Course)
        {
            val temp: Course = other

            return this.courseNum == temp.courseNum
        }

        return false
    }


    /**
     * Returns this.Course as a string
     */
    override fun toString(): String
    {
        return String.format("$field%n$courseNum%n$title%n")
    }
}
