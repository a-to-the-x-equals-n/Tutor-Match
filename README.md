
# Tutor-Matching Application Backend

## Overview

This document describes the backend classes for a tutor-matching application developed in Kotlin. These classes handle user accounts, course catalog management, scheduling, and session tracking.

### Account Class

- **Purpose**: Manages user accounts, including details such as name, email, password, tutor status, ratings, and study time.
- **Key Features**:
    - Tutor privilege management
    - Rating and study time tracking
    - Session and course association

### AccountDB Class

- **Purpose**: Core database structure for managing all accounts with CRUD operations.
- **Key Features**:
    - File management integration for account data persistence
    - Email-based account search and validation

### Course and CourseCatalog Classes

- **Purpose**: Represents individual courses and manages a catalog of courses available for tutoring.
- **Key Features**:
    - Course comparison and sorting
    - Web scraping for course catalog population

### CoursesDB Class

- **Purpose**: Manages the association of courses to user accounts.
- **Key Features**:
    - Course list initialization and update for accounts
    - Matching accounts by course for tutor-student pairing

### Schedule and ScheduleDB Classes

- **Purpose**: Tracks personal schedules and availability for tutoring sessions.
- **Key Features**:
    - Hourly availability management
    - Schedule initialization and updating

### Session and SessionDB Classes

- **Purpose**: Manages tutoring sessions between students and tutors.
- **Key Features**:
    - Session creation, completion tracking, and rating
    - Session list management for accounts

### DatabaseManager Class

- **Purpose**: Provides generic read and write operations for database management.
- **Key Features**:
    - JSON file management for database persistence

### Main Class

- **Purpose**: Initializes and manages the application's backend databases.
- **Key Features**:
    - Master database initialization for accounts, courses, schedules, and sessions
    - Course catalog web scraping

## Setup and Usage

To use these classes, ensure Kotlin is properly set up in your development environment. Initialize the `Main` class to set up the databases. Use the individual database classes (`AccountDB`, `CoursesDB`, `ScheduleDB`, `SessionDB`) for managing specific aspects of the application's backend.
