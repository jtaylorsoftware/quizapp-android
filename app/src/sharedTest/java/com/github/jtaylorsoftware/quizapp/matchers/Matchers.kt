package com.github.jtaylorsoftware.quizapp.matchers


import com.github.jtaylorsoftware.quizapp.data.domain.QuizValidationErrors
import com.github.jtaylorsoftware.quizapp.data.domain.models.User
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizDto
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizFormDto
import com.github.jtaylorsoftware.quizapp.data.network.dto.QuizResultDto
import com.github.jtaylorsoftware.quizapp.data.network.dto.UserDto
import org.hamcrest.Description
import org.hamcrest.Matchers.containsInAnyOrder
import org.hamcrest.TypeSafeMatcher

class SameQuizAs(private val expected: QuizDto?) : TypeSafeMatcher<QuizDto>() {
    override fun describeTo(description: Description?) {
        description?.appendText("equal to in any order: $expected")
    }

    override fun matchesSafely(actual: QuizDto?): Boolean {
        return actual === expected ||
                actual != null &&
                expected != null &&
                actual.id == expected.id &&
                actual.date == expected.date &&
                actual.user == expected.user &&
                actual.title == expected.title &&
                actual.expiration == expected.expiration &&
                actual.isPublic == expected.isPublic &&
                listsMatchInAnyOrder(actual, expected)
    }

    private fun listsMatchInAnyOrder(actual: QuizDto, expected: QuizDto): Boolean =
        containsInAnyOrder(*expected.questions.toTypedArray()).matches(actual.questions)
                && containsInAnyOrder(*expected.results.toTypedArray()).matches(actual.results)
                && containsInAnyOrder(*expected.allowedUsers.toTypedArray()).matches(actual.allowedUsers)
}

class SameQuizFormAs(private val expected: QuizFormDto?) : TypeSafeMatcher<QuizFormDto>() {
    override fun describeTo(description: Description?) {
        description?.appendText("equal to in any order: $expected")
    }

    override fun matchesSafely(actual: QuizFormDto?): Boolean {
        return actual === expected ||
                actual != null &&
                expected != null &&
                actual.id == expected.id &&
                actual.date == expected.date &&
                actual.username == expected.username &&
                actual.title == expected.title &&
                actual.expiration == expected.expiration &&
                listsMatchInAnyOrder(actual, expected)
    }

    private fun listsMatchInAnyOrder(actual: QuizFormDto, expected: QuizFormDto): Boolean =
        containsInAnyOrder(*expected.questions.toTypedArray()).matches(actual.questions)
}

class SameResultAs(private val expected: QuizResultDto?) : TypeSafeMatcher<QuizResultDto>() {
    override fun describeTo(description: Description?) {
        description?.appendText("equal to in any order: $expected")
    }

    override fun matchesSafely(actual: QuizResultDto?): Boolean {
        return actual === expected ||
                actual != null &&
                expected != null &&
                actual.id == expected.id &&
                actual.date == expected.date &&
                actual.user == expected.user &&
                actual.quiz == expected.quiz &&
                actual.score == expected.score &&
                actual.quizTitle == expected.quizTitle &&
                actual.createdBy == expected.createdBy &&
                listsMatchInAnyOrder(actual, expected)
    }

    private fun listsMatchInAnyOrder(actual: QuizResultDto, expected: QuizResultDto): Boolean =
        containsInAnyOrder(*expected.answers.toTypedArray()).matches(actual.answers)
}

class SameQuizValidationErrorAs(private val expected: QuizValidationErrors?) :
    TypeSafeMatcher<QuizValidationErrors>() {
    override fun describeTo(description: Description?) {
        description?.appendText("equal to in any order: $expected")
    }

    override fun matchesSafely(actual: QuizValidationErrors?): Boolean {
        return actual === expected ||
                actual != null &&
                expected != null &&
                actual.title == expected.title &&
                actual.expiration == expected.expiration &&
                actual.allowedUsers == expected.allowedUsers &&
                actual.questions == expected.questions &&
                listsMatchInAnyOrder(actual, expected)
    }

    private fun listsMatchInAnyOrder(
        actual: QuizValidationErrors,
        expected: QuizValidationErrors
    ): Boolean =
        containsInAnyOrder(*expected.questionErrors.toTypedArray()).matches(actual.questionErrors)
}

class SameUserAs(private val expected: User?) :
    TypeSafeMatcher<User>() {
    override fun describeTo(description: Description?) {
        description?.appendText("equal to in any order: $expected")
    }

    override fun matchesSafely(actual: User?): Boolean {
        return actual === expected ||
                actual != null &&
                expected != null &&
                actual.id == expected.id &&
                actual.date == expected.date &&
                actual.username == expected.username &&
                actual.email == expected.email &&
                listsMatchInAnyOrder(actual, expected)
    }

    private fun listsMatchInAnyOrder(
        actual: User,
        expected: User
    ): Boolean =
        containsInAnyOrder(*expected.quizzes.toTypedArray()).matches(actual.quizzes)
                && containsInAnyOrder(*expected.results.toTypedArray()).matches(actual.results)
}

class SameUserDtoAs(private val expected: UserDto?) :
    TypeSafeMatcher<UserDto>() {
    override fun describeTo(description: Description?) {
        description?.appendText("equal to in any order: $expected")
    }

    override fun matchesSafely(actual: UserDto?): Boolean {
        return actual === expected ||
                actual != null &&
                expected != null &&
                actual.id == expected.id &&
                actual.date == expected.date &&
                actual.username == expected.username &&
                actual.email == expected.email &&
                listsMatchInAnyOrder(actual, expected)
    }

    private fun listsMatchInAnyOrder(
        actual: UserDto,
        expected: UserDto
    ): Boolean =
        containsInAnyOrder(*expected.quizzes.toTypedArray()).matches(actual.quizzes)
                && containsInAnyOrder(*expected.results.toTypedArray()).matches(actual.results)
}

