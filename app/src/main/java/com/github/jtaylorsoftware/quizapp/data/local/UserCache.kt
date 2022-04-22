package com.github.jtaylorsoftware.quizapp.data.local

import android.content.Context
import android.content.SharedPreferences
import com.github.jtaylorsoftware.quizapp.data.local.entities.UserEntity

/**
 * A data source that saves the [UserEntity] to some local persistence mechanism.
 */
interface UserCache {
    /**
     * Saves the [UserEntity] such that it can be retrieved later with the same
     * properties.
     */
    fun saveUser(user: UserEntity)

    /**
     * Saves an auth token associated with the user.
     */
    fun saveToken(token: String)

    /**
     * Retrieves the previously saved [UserEntity], or `null` if it isn't
     * currently saved.
     */
    fun loadUser(): UserEntity?

    /**
     * Retrieves the previously stored auth token, or `null` if it is not
     * currently stored, or the data is incomplete.
     */
    fun loadToken(): String?

    /**
     * Clears the stored [UserEntity].
     */
    fun clearUser()

    /**
     * Clears the stored auth token.
     */
    fun clearToken()
}

class UserSharedPrefCache constructor(context: Context) : UserCache {
    private var sharedPref: SharedPreferences =
        context.getSharedPreferences(
            "com.github.jtaylorsoftware.quizapp.PREFERENCES_USER_CACHE",
            Context.MODE_PRIVATE
        )

    override fun saveUser(user: UserEntity) {
        with(sharedPref.edit()) {
            putString("user_id", user.id)
            putString("user_date", user.date)
            putString("user_username", user.username)
            putString("user_email", user.email)
            putStringSet("user_quizzes", user.quizzes.toSet())
            putStringSet("user_results", user.results.toSet())
            apply()
        }
    }

    override fun saveToken(token: String) {
        with(sharedPref.edit()) {
            putString("user_auth_token", token)
            apply()
        }
    }

    override fun loadUser(): UserEntity? = run {
        UserEntity(
            id = sharedPref.getString("user_id", null) ?: return@run null,
            date = sharedPref.getString("user_date", null) ?: return@run null,
            username = sharedPref.getString("user_username", null) ?: return@run null,
            email = sharedPref.getString("user_email", null) ?: return@run null,
            quizzes = sharedPref.getStringSet("user_quizzes", null)?.toList()
                ?: return@run null,
            results = sharedPref.getStringSet("user_results", null)?.toList()
                ?: return@run null,
        )
    }

    override fun loadToken(): String? = sharedPref.getString("user_auth_token", null)

    override fun clearUser() {
        with(sharedPref.edit()) {
            remove("user_id")
            remove("user_date")
            remove("user_username")
            remove("user_email")
            remove("user_quizzes")
            remove("user_results")
            apply()
        }
    }

    override fun clearToken() {
        with(sharedPref.edit()) {
            remove("user_auth_token")
            apply()
        }
    }
}