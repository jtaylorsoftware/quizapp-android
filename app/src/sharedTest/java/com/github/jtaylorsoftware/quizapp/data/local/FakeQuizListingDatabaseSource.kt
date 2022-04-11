package com.github.jtaylorsoftware.quizapp.data.local

import com.github.jtaylorsoftware.quizapp.data.local.entities.QuizListingEntity

class FakeQuizListingDatabaseSource(
    data: List<QuizListingEntity> = emptyList()
) : QuizListingDatabaseSource {
    private val cache = mutableMapOf<String, QuizListingEntity>()
    init {
        data.forEach {
            cache[it.id] = it
        }
    }

    override suspend fun getById(id: String): QuizListingEntity? = cache[id]

    override suspend fun getAllCreatedByUser(user: String): List<QuizListingEntity> = cache.mapNotNull { (_, quiz) ->
        if (quiz.user == user) quiz else null
    }

    override suspend fun insertAll(listings: List<QuizListingEntity>) {
        listings.forEach { cache[it.id] = it }
    }

    override suspend fun delete(id: String) {
        cache.remove(id)
    }

    override suspend fun deleteAllByUser(user: String) {
        cache.entries.removeAll { (_, it) -> it.user == user }
    }

    override suspend fun deleteAll() {
        cache.clear()
    }
}