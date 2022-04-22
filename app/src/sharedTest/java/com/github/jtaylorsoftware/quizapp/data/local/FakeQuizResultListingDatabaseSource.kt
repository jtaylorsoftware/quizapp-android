package com.github.jtaylorsoftware.quizapp.data.local

import com.github.jtaylorsoftware.quizapp.data.local.entities.QuizResultListingEntity
import com.github.jtaylorsoftware.quizapp.data.network.FallibleNetworkSource

class FakeQuizResultListingDatabaseSource(
    data: List<QuizResultListingEntity> = emptyList()
) : QuizResultListingDatabaseSource, FallibleNetworkSource() {
    private val cache = mutableMapOf<String, QuizResultListingEntity>()

    init {
        data.forEach {
            cache[it.id] = it
        }
    }

    override suspend fun getById(id: String): QuizResultListingEntity? = cache[id]

    override suspend fun getAllByUser(user: String): List<QuizResultListingEntity> = cache.mapNotNull { (_, result) ->
        if (result.user == user) result else null
    }

    override suspend fun getAllByQuiz(quiz: String): List<QuizResultListingEntity> = cache.mapNotNull { (_, result) ->
        if (result.quiz == quiz) result else null
    }

    override suspend fun getByQuizAndUser(quiz: String, user: String): QuizResultListingEntity? = cache.firstNotNullOfOrNull { (_, result) ->
        if (result.quiz == quiz && result.user == user) result else null
    }

    override suspend fun insertAll(listings: List<QuizResultListingEntity>) {
        listings.forEach { cache[it.id] = it }
    }

    override suspend fun deleteByQuizAndUser(quiz: String, user: String) {
        cache.entries.removeAll { (_, it) -> it.quiz == quiz && it.user == user }
    }

    override suspend fun deleteAllByQuiz(quiz: String) {
        cache.entries.removeAll { (_, it) -> it.quiz == quiz }
    }

    override suspend fun deleteAllByUser(user: String) {
        cache.entries.removeAll { (_, it) -> it.user == user }
    }

    override suspend fun deleteAll() {
        cache.clear()
    }
}