# Sample Data for Initial SQL Database Testing

This repository contains sample CSV files for testing SQL databases. It includes two CSV files:

- `accounts.csv`: Contains sample data for accounts.
- `courses.csv`: Contains sample data for courses.

## File Descriptions

### `accounts.csv`

This file contains sample data for accounts, with the following columns:

1. `id`: The unique identifier for each account.
2. `username`: The username associated with the account.
3. `email`: The email address associated with the account.
4. `password`: The password for the account.
5. `is_student`: A boolean value indicating whether the account is a student account (`true`) or not (`false`).
6. `student_id`: The student ID associated with the account (if it is a student account).
7. `instructor_id`: The instructor ID associated with the account (if it is an instructor account).
8. `role_id`: The role ID associated with the account.

### `courses.csv`

This file contains sample data for courses, with the following columns:

1. `field`: The field of study for the course (e.g., "CSCI" for Computer Science).
2. `course_num`: The course number.
3. `title`: The title of the course.

## Usage

You can use these CSV files to populate your SQL database for testing purposes. Simply import them into your database management tool and use the data for testing queries and operations.
