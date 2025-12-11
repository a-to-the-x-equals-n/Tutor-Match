package com.tm.backend


import org.jsoup.Jsoup
import java.util.ArrayList

/* ==================================================================================================================

CORE DATABASE STRUCTURE THAT MANAGES ALL ACCOUNTS

**PROPERTIES**

- URL (STRING) : the university managed website that houses the complete course program for computer science

- CATALOG (ARRAYLIST<COURSE>) : list to hold all courses

** NOTE : this class feels clunky, and may later be deleted as its functions are migrated to other, more fitting classes

 ==================================================================================================================== */
class CourseCatalog(private val URL: String) {

    val catalog: ArrayList<Course> = ArrayList<Course>()

    /**
     * 'init' function in Kotlin function similar to constructors from Java
     * init functions are ran anytime this class is instantiated
     */
    init {
        loadCatalog()
    }

    /**
     * Retrieves and stores courses from the specified URL in the 'courseCatalog'.
     */
    fun loadCatalog(): Boolean {

        // this is the code used for web scraping the course list
        // 'document' is the jsoup structure used to hold webpage text
        val document = try {
            Jsoup.connect(URL).get() // navigates to website
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        // loop to find every course
        for (row in document.select(".acalog-course a")) // finds every segment of HTML matching ".acalog-course a"
        {
            val line = row.text()
            val (field, courseNum, title) = line.split(
                " ",
                limit = 3
            ) // breaks up the text input by whitespace

            // creates a new course, then adds that course to the catalog data structure
            catalog.add(Course(field, courseNum.toInt(), title))
        }

        return true // confirmation of successful web scrape
    }
}