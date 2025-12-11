package com.tm.backend

import com.google.gson.reflect.TypeToken
import java.util.UUID
import kotlin.streams.*


/* ==================================================================================================================

CORE DATABASE STRUCTURE THAT MANAGES ALL ACCOUNTS

**PROPERTIES**

- FILEPATH (STRING) : this is the file name / path as a string
- - NOTE : may soon be deprecated as file management is changed into .json files

- ACCOUNTS (DICTIONARY[UUID] = ACCOUNT) : this is a KEY VALUE pair data structure
- - KEY : `back end`.Account UUIDs
- - VALUE : the `back end`.Account object

- FILEMANAGER (DATABASEMANAGER) : this is the class used to handle all file management
- - this class handles all reading and writing to / from files
- - a separate class was made to manage these functions so as the database storage methods change, only the 'fileManager' class needs to be revised


 ==================================================================================================================== */

class AccountDB(var filepath: String = "")
{

    var accounts: HashMap<UUID, Account> = HashMap<UUID, Account>()
    var fileManager: DatabaseManager = DatabaseManager()
    val type = object : TypeToken<HashMap<UUID, Account>>() {}.type

    /**
     * Loads `back end`.Account database
     */
    fun load() // Load accounts
    {
        // the 'as' is used to cast the object being assigned to 'accounts' as the appropriate data structure
        // - fileManager is using one function to read/write all database data
        accounts = filepath.let { fileManager.load(it, type)!! }
    }

    /**
     * Saves / updates `back end`.Account database
     */
    fun save()
    {
        filepath.let { fileManager.save(it, accounts, type) }
    }

    /**
     * Add a new account to the database.
     *
     * @param email    The email address of the new account.
     * @param password The password of the new account.
     * @param name     The name associated with the new account.
     * @param isTutor  Indicates if the new account is a tutor or not.
     * @return boolean
     * - The return value is for the confirmation for successful account creation
     */
    fun addAccount(email: String, pass: String, name: String, tutor: Boolean): Boolean
    {
        if(!emailExists(email))
        {
            val newID: UUID = UUID.randomUUID()

            // The `back end`.Account class normally creates a random UUID upon class instantiation
            // since the Database needs the ID (Key) to add the new `back end`.Account, a UUID is created here
            // This UUID is passed to the `back end`.Account constructor, and replaces the `back end`.Account classes default UUID creation
            val newAccount: Account = Account(name, email, pass, tutor, ID = newID, token = "")

            accounts[newID] = newAccount

            save()

            return true // returns a bool
        }
        return false
    }


    /**
     * Remove an account from the database by its unique ID.
     *
     * @param id The unique ID of the account to be removed.
     *
     * @return a boolean for confirmation of deletion
     */
    fun deleteAccount(id: UUID): Boolean
    {
        if(accounts.containsKey(id))
        {
            accounts.remove(id)

            save()
            return true
        }
        return false
    }


    /**
     * Update an existing account's information.
     *
     * @param id   The unique ID of the account to update.
     * @param newEmail    The updated email for the account.
     * @param newPass The updated password for the account.
     * @param newName     The updated name for the account.
     * @return true if the account was successfully updated, false if the account
     *         was not found.
     */
    fun updateAccount(id: UUID, newEmail: String, newPass: String, newName: String): Boolean
    {
        if (accounts[id] is Account)
        {
            accounts[id]?.email = newEmail
            accounts[id]?.name = newName
            accounts[id]?.password = newPass

            save() // Save the updated account data to the DB

            return true // `back end`.Account successfully updated.
        }

        return false // `back end`.Account not found, update failed.
    }

    /**
     * Get an account by its unique ID (UUID).
     *
     * @param id The unique ID of the account to retrieve.
     * @return The `back end`.Account object associated with the given ID, or null if not
     *         found.
     */
    fun getAccountByID(id: UUID): Account?
    {
        return accounts[id]
    }

    /**
     * Get an account by email address.
     *
     * @param email The email address to search for.
     * @return The `back end`.Account object associated with the given email, or null if not
     *         found.
     */
    fun getAccountByEmail(email: String): Account?
    {
        return accounts.entries.asSequence().map { it.value } .firstOrNull {it.email == email}
    }

    /**
     * Gets UUID by searching `back end`.Account emails
     * @param email this.`back end`.Account's email
     * Returns this.Accounts UUID or null if email not found
     */
    fun getKeyByEmail(email: String): UUID?
    {
        return accounts.entries.firstOrNull { it.value.email == email } ?.key
    }

    /**
     * This function is used after a user has logged in, and the program needs the users ID token
     */
    fun getAccountToken(email: String): UUID?
    {
        return getKeyByEmail(email)
    }


    /**
     * Check if a given email and password combination is valid.
     *
     * @param email    The email to be checked.
     * @param pass The password to be checked.
     * @return True if the combination is valid, false otherwise.
     */
    fun isLoginValid(email: String, pass: String): Boolean
    {
        return accounts.values.any { it.email == email && it.password == pass }
    }


    /**
     * Check if a given email is already taken by an existing account.
     *
     * @param checkEmail The email to be checked.
     * @return True if the email is already taken, false otherwise.
     */
    fun emailExists(checkEmail: String): Boolean
    {
        return accounts.values.any { it.email == checkEmail }
    }
}




