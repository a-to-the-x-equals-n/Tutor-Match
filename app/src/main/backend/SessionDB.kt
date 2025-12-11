package com.tm.backend

import java.time.ZonedDateTime
import java.util.*

class SessionDB(val filepath: String = "") {
    // Map of UUIDs to a list of Sessions for each user
    private val sessions: MutableMap<UUID, ArrayList<Session>> = HashMap()

    fun createSession(
        tutorId: UUID,
        studentId: UUID,
        startDate: ZonedDateTime,
        course: Course,
        sessionLength: Int
    ): Session? {
        val session = Session(
            id = UUID.randomUUID().toString(),
            tutorID = tutorId.toString(),
            studentID = studentId.toString(),
            startDate = startDate,
            course = course,
            sessionLength = sessionLength,
            sessionComplete = false,
            rating = null
        )

        // Add session to tutor's list
        sessions.computeIfAbsent(tutorId) { ArrayList() }.add(session)

        // Add session to student's list
        sessions.computeIfAbsent(studentId) { ArrayList() }.add(session)

        return session
    }

    fun getSessionsForUser(userId: UUID): List<Session> {
        return sessions[userId] ?: emptyList()
    }

    // Update a session in the database
    fun updateSession(userId: UUID, updatedSession: Session) {
        sessions[userId]?.let { userSessions ->
            val index = userSessions.indexOfFirst { it.id == updatedSession.id }
            if (index != -1) {
                userSessions[index] = updatedSession
            }
        }
    }

    fun generateSession(accountId: UUID) {
        // Ensure there's an entry for the account in the sessions map
        // This will initialize the session list for the account if it doesn't already exist
        sessions.computeIfAbsent(accountId) { ArrayList() }
    }
}