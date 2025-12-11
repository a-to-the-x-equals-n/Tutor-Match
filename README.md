
# Tutor Matching Android App

Kotlin/Android app for matching students with tutors. Users can sign up or log in, add courses, browse tutors by course, view weekly availability, and manage upcoming sessions. The codebase includes a simple backend data layer that stores user, course, schedule, and session data in JSON and can also talk to a remote API.

## Feature Highlights
- Role-based auth flow (student vs tutor) with signup/login screens.
- Course catalog browser backed by `course_catalog_db.json`, add/remove personal courses, and launch tutor search for a course.
- Tutor discovery and scheduling: view tutors for a course, inspect profiles, and open a week view to manage availability (tutor-only in the bottom nav).
- Session list with delete actions plus basic profile display and logout.
- Animated background and shared bottom navigation via `BaseActivity`.

## Project Layout
- `app/src/main/frontend`: Activities and UI flow (`LoginActivity`, `SignupActivity`, `CoursesActivity`, `TutorListActivity`, `WeekViewActivity`, `SessionsActivity`, `ProfileActivity`, `SplashActivity`, etc.). `BaseActivity` wires the bottom nav and background animation.
- `app/src/main/backend`: Core models and data helpers (`Account`, `Course`, `Schedule`, `Session`), local JSON DB helpers (`AccountDB`, `CoursesDB`, `ScheduleDB`, `SessionDB`), and `DatabaseManager` for HTTP + JSON persistence.
- `app/src/main/assets`: Seed JSON used by the app (`account_db.json`, `course_db.json`, `schedule_db.json`, `session_db.json`, `course_catalog_db.json`).
- `app/src/main/res`: Layouts, drawables, and animations for the screens.
- `app/build.gradle`: Android configuration (compileSdk 34, minSdk 33, AGP 8.3.2, Kotlin 1.9.0) and dependencies (Material, Compose BOM, ConstraintLayout, RecyclerView, Gson, OkHttp, Jsoup, PostgreSQL driver, Ktor client libs).

## Data & Backend Notes
- API calls live in `app/src/main/backend/DatabaseManager.kt` and use `URL = "http://34.148.172.29:5000"` for `/new_user`, `/login`, and course/schedule sync. Update this constant if you run a different backend or are offline.
- User/session metadata is also saved locally under the app's internal storage (`account_db.json`, `course_db.json`, `schedule_db.json`, `session_db.json`). SharedPreferences (`AppPreferences`) store `userUUID` and `userRole` after login.
- Course search reads directly from the packaged `course_catalog_db.json`. Tutor schedules can be read from `schedule_db.json` (assets) and written to local storage when tutors edit availability.

## Getting Started
1. Install Android Studio (Koala or newer) with Android SDK 34 and a JDK 17 runtime (AGP 8.x requirement).  
2. Open the repository as an Android project or sync Gradle on the command line.
3. Run on a device/emulator (Android 13+, minSdk 33) from Android Studio, or use the wrapper:
   ```bash
   ./gradlew assembleDebug          # builds the APK
   ./gradlew installDebug           # installs to a connected device/emulator
   ```
4. Ensure the backend at `DatabaseManager.URL` is reachable for signup/login. If you are testing offline, stub those calls or point the constant at a local server.

## Development Tips
- Main launcher is `SplashActivity` (see `AndroidManifest.xml`), which routes to `LoginActivity`.
- Bottom navigation items that need a tutor role (e.g., calendar) are enforced in `BaseActivity`.
- Sample data for quick UI checks lives in `app/src/main/assets`; clearing app storage resets local JSON persistence.
- Basic tests use the default Android/JUnit setup; run `./gradlew test` for JVM tests or `./gradlew connectedAndroidTest` with a device for instrumentation.
