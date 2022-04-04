package com.github.jtaylorsoftware.quizapp.data.local

import com.github.jtaylorsoftware.quizapp.data.local.entities.UserEntity

class FakeUserCache(
    user: UserEntity? = null,
    token: String? = null
): UserCache {
    private var cache: UserEntity? = user
    private var jwt: String? = token

    override fun saveUser(user: UserEntity) {
        cache = user
    }

    override fun saveToken(token: String) {
        jwt = token
    }

    override fun loadUser(): UserEntity? = cache

    override fun loadToken(): String? = jwt

    override fun clearUser() {
        cache = null
    }

    override fun clearToken() {
        jwt = null
    }
}