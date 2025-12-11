import java.sql.DriverManager
import java.sql.SQLException


// kotlinc Main.kt -include-runtime -d Main.jar

fun main(args: Array<String>) 
{

    // Define constants for the PostgreSQL port and host
    val PORT = 5432
    val HOST = "localhost"
    val TABLE = "users"
    val DATABASE = "tm"
    // courses

    // Specify the username and password for the database
    val USERNAME = "postgres"
    val PASSWORD = "test"
    val COLUMN = "email"

    // Construct the URL for the PostgreSQL database
    val URL = "jdbc:postgresql://$HOST:$PORT/$DATABASE"

    var RESULT: String = ""
    
    // try-catch
    try
    {
        // Establish a connection to the database using the DriverManager class
        val connection = DriverManager.getConnection(URL, USERNAME, PASSWORD)
    

        // Create a statement object for executing SQL queries
        val statement = connection.createStatement()
    

        // Execute a SELECT query and store the results in a ResultSet object
        val resultSet = statement.executeQuery("SELECT * FROM $TABLE")


        // Iterate over the results in the ResultSet object
        while (resultSet.next()) 
        {
            RESULT = resultSet.getString(COLUMN)

        }
    
        print(RESULT)

        // Close the statement and connection to release resources
        statement.close()
        connection.close()
    }


    // Catch and handle any SQLException that may occur during database operations
    catch (e: SQLException) 
    {
        println("SQL Exception: ${e.message}")
    }
}

