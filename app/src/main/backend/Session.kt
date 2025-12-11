package com.tm.backend

import java.time.ZonedDateTime
import java.util.Date
import java.util.UUID

data class Session(
    val id: String = UUID.randomUUID().toString(),
    val tutorID: String,
    val studentID: String,
    val startDate: ZonedDateTime,
    val course: Course,
    var sessionLength: Int,
    var sessionComplete: Boolean = false,  // Default value provided
    var rating: Int? = null  // Nullable with default value of null
)

