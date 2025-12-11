package com.tm.backend

import com.google.gson.Gson
import com.google.gson.JsonParseException
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken

import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.reflect.Type
import java.util.UUID



val URL = "http://34.148.172.29:5000"
val MEDIATYPE = "application/json; charset=utf-8".toMediaType()
val CLIENT = OkHttpClient()
var token: UUID? = null
var jwt: String? = null

///* ==================================================================================================================
//
//CORE CLASS THAT WILL MANAGE ALL READING AND WRITING OF DATABASES
//
//- UNFINISHED
//
//- NOTE : this class will eventually manage the interactions between the SQL database
//
//**PROPERTIES**
//
//- FILEPATH (STRING) : this is the file name / path as a string
//- - NOTE : may soon be deprecated as file management is changed into .json files
//
//- DATA (T) : this is the structure being passed from the databases to have their contents loaded into
//
// ==================================================================================================================== */


class DatabaseManager {
    /**
     * 'load' function accepts a mutable reference 'data' of type 'T', where 'T' is the type you want to deserialize the JSON data into
     */
    inline fun <reified T> load(filePath: String, data: Type): T? {
        try {
            val file = File(filePath)
            val gson = Gson()

            // val type = object : TypeToken<HashMap<UUID, `back end`.Account>>() {}.type

            return if (file.exists()) {
                // 'data!!::class.java' is used to find what 'object' type 'data' is (Gson needs this for deserialization)
                file.bufferedReader().use { gson.fromJson(it, data) }
            } else {
                null
            }
        } catch (e: FileNotFoundException) {
            println("File not found: $filePath")
            return null
        } catch (e: JsonParseException) {
            println("Error parsing JSON file: ${e.message}")
            return null
        } catch (e: IOException) {
            println("Error reading file: ${e.message}")
            return null
        }
    }

    /**
     * Saves database
     */
    inline fun <reified T> save(filepath: String, data: T, type: Type): Boolean {
        try {
            val file = File(filepath)
            val gson = Gson()

            // Convert data to JSON string
            val jsonString = gson.toJson(data)

            // Write JSON string to file
            file.bufferedWriter().use { it.write(jsonString) }

            return true
        } catch (e: IOException) {
            println("Error writing to file: ${e.message}")
            return false
        }
    }

// ***********************************************************************************************
//    HERE IS THE FUNCTION I WAS USING TO CONNECT TO THE SERVER ELLIOT AND JACOB
// *******************************************************************************************

    fun newUser(name: String, email: String, password: String, tutor: Boolean, endpoint: String = "/new_user"): Account?
    {
        val gson = Gson()
        val userData =
            mapOf("name" to name, "email" to email, "password" to password, "tutor" to tutor)
        val json = gson.toJson(userData)
        val requestBody = json.toRequestBody(MEDIATYPE)

        val request = Request.Builder()
            .url(URL + endpoint)
            .post(requestBody)
            .build()

        CLIENT.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            val responseBody = response.body?.string()

            println("Response: $responseBody")

            return responseBody?.let {
                val responseJson = gson.fromJson(it, JsonObject::class.java)
                val accountJson = responseJson.getAsJsonObject("Account")
                val jwt = responseJson.get("jwt").asString  // Get JWT from the response

                if (accountJson != null)
                {
                    // Create and return the Account object from the response
                    return@let Account(
                        name = accountJson.get("name")?.asString ?: name,
                        email = accountJson.get("email")?.asString ?: email,
                        password = accountJson.get("password")?.asString ?: password,
                        tutor = accountJson.get("tutor")?.asBoolean ?: tutor,
                        ID = UUID.fromString(accountJson.get("ID").asString),
                        rating = accountJson.get("rating")?.asFloat ?: 0.0f,
                        studyTime = accountJson.get("studyTime")?.asInt ?: 0,
                        token = jwt
                    )
                }
                else
                {
                    // Optionally handle the error case or return null
                    println("Error in user creation: ${responseJson.get("message").asString}")
                    return@let null
                }
            }
        }
    }


    fun login(email: String, password: String, endpoint: String = "/login"): Account?
    {
        val loginData = mapOf("email" to email, "password" to password)
        val gson = Gson()
        val jsonData = gson.toJson(loginData)
        val requestBody = jsonData.toRequestBody(MEDIATYPE)

        val request = Request.Builder().url(URL + endpoint).post(requestBody).build()

        try
        {
            CLIENT.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseString = response.body?.string()
                    val responseJson = gson.fromJson(responseString, JsonObject::class.java)

                    val accountJson = responseJson.getAsJsonObject("Account")
                    val jwt = responseJson.get("jwt").asString  // Get JWT from the response

                    if (accountJson != null) {
                        return Account(
                            name = accountJson.get("name").asString,
                            email = accountJson.get("email").asString,
                            password = accountJson.get("password").asString,
                            tutor = accountJson.get("tutor").asBoolean,
                            ID = UUID.fromString(accountJson.get("ID").asString),
                            rating = accountJson.get("rating").asFloat,
                            studyTime = accountJson.get("studyTime").asInt,
                            token = jwt
                        )
                    }
                }
                return null  // Return null if response is not successful or JWT is missing
            }
        }
        catch (e: IOException)
        {
            println("Error during login: ${e.message}")
            return null
        }
    }

    fun getCourses(user: Account, endpoint: String = "/get_courses"): Account?
    {
        val request = Request.Builder()
            .url("$URL$endpoint/${user.ID}")
            .addHeader("Authorization", "Bearer ${user.token}")
            .build()

        CLIENT.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            val responseBody = response.body?.string()
            responseBody?.let {
                val gson = Gson()
                val type = object : TypeToken<Map<String, ArrayList<Course>>>() {}.type
                val coursesData: Map<String, ArrayList<Course>> = gson.fromJson(it, type)

                // Use the updateCourses method to update the myCourses database inside the Account object
                coursesData[user.ID.toString()]?.let { newCourses ->
                    user.updateCourses(newCourses)
                }
            }
        }
        return user
    }

    fun postCourses(user: Account, endpoint: String = "/post_courses")
    {
        // Convert the courses ArrayList to JSON
        val gson = Gson()
        val jsonData = gson.toJson(user.myCourses[user.ID])

        // Prepare the JSON as a request body
        val requestBody: RequestBody = jsonData.toRequestBody(MEDIATYPE)

        // Create the request with Authorization header
        val request = Request.Builder()
            .url("$URL$endpoint/${user.ID}")
            .addHeader("Authorization", "Bearer ${user.token}")
            .post(requestBody)
            .build()

        // Execute the request
        CLIENT.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            val responseBody = response.body?.string()
            responseBody?.let {
                // Extract the "message" directly from the response body
                val message = gson.fromJson(it, JsonObject::class.java).get("message").asString

                // Do something with the message (print it, etc.)
                println("Server message: $message")
            }
        }
    }


    fun getSchedules(user: Account, endpoint: String = "/get_schedules"): Account? {
        val request = Request.Builder()
            .url("$URL$endpoint/${user.ID}")
            .addHeader("Authorization", "Bearer ${user.token}")
            .build()

        CLIENT.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")

            val responseBody = response.body?.string()
            responseBody?.let {
                val gson = Gson()
                val responseData: Any = gson.fromJson(it, Any::class.java)

                // Check the type of the response data (e.g., using is or as)
                if (responseData is HashMap<*, *>)
                {
                    // Cast the data to the expected type and use it
                    val schedData = responseData as HashMap<Int, MutableList<Schedule.TimeRange>>
                    user.updateSchedule(schedData)
                }
                else if (responseData is String && responseData == "error")
                {
                    // Handle error message
                    println("Error response received")
                }
                else
                {
                    // Handle unexpected response format
                    println("Unexpected response format: $responseData")
                }
            }
        }
        return user
    }

    fun postSchedules(user: Account, endpoint: String = "/post_schedules")
    {
        // Convert the user's schedule to JSON
        val gson = Gson()
        val scheduleData = user.mySchedule
        val jsonData = gson.toJson(scheduleData)
        println("The Schedules received: $jsonData")  // Debugging output

        // Prepare the JSON as a request body
        val requestBody: RequestBody = jsonData.toRequestBody(MEDIATYPE)

        // Create the request with Authorization header
        val request = Request.Builder()
            .url("$URL$endpoint/${user.ID}")
            .addHeader("Authorization", "Bearer ${user.token}")
            .post(requestBody)
            .build()

        // Execute the request
        CLIENT.newCall(request).execute().use { response ->
            if (!response.isSuccessful)
            {
                val errorBody = response.body?.string()
                println("Error response body: $errorBody")  // Debugging output
                throw IOException("Unexpected code $response with body $errorBody")
            }
            val responseBody = response.body?.string()
            responseBody?.let {
                // Extract the "message" directly from the response body if needed
                val message = gson.fromJson(it, JsonObject::class.java).get("message").asString

                // Do something with the message (print it, etc.)
                println("Server message: $message")
            }
        }
    }

//    fun getSession(user: Account, endpoint: String = "/get_session"): SessionDB?
//    {
//        val request = Request.Builder()
//            .url("$URL$endpoint/${user.ID}")
//            .addHeader("Authorization", "Bearer ${user.token}")
//            .build()
//
//        CLIENT.newCall(request).execute().use { response ->
//            if (!response.isSuccessful) throw IOException("Unexpected code $response")
//
//            // Assuming the response body contains the session details as JSON
//            response.body?.let { responseBody ->
//                val sessionJson = responseBody.string()
//                // Parse the JSON to create and return a Session object
//                return gson.fromJson(sessionJson, Session::class.java)
//            }
//
//            return null // Return null if response body is empty
//        }
//    }


}