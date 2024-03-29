package com.github.jtaylorsoftware.quizapp.util

/**
 * A validator that checks Strings against an implementation-defined rule.
 */
interface TextValidator {
    fun validate(text: String): Boolean
}

object UsernameValidator : TextValidator {
    private val regex = Regex("""^[A-Za-z0-9]{5,12}$""")

    override fun validate(text: String): Boolean = regex.matches(text)
}

object SimplePasswordValidator : TextValidator {
    private val regex = Regex("""^\S{8,20}$""")

    override fun validate(text: String): Boolean = regex.matches(text)
}

object SimpleEmailValidator : TextValidator {
    private val regex = Regex("""^([^\s.,@]+)@([^\s.,@]+\.?)+(?<!\.)$""")

    override fun validate(text: String): Boolean = regex.matches(text)
}

object AllowedUsersValidator : TextValidator {
    private val regex = Regex("""^(([A-Za-z0-9]{5,12})(?:,[^\S\r\n]*|$))+""")
    private val separatorRegex = Regex(""",\s*""")

    override fun validate(text: String): Boolean = text.trim().let {
        it.isBlank() || regex.matches(it)
    }

    /**
     * Splits an input string around occurrences of the expected separator
     * for "allowed users" strings. Does not perform validation before
     * splitting.
     */
    fun split(input: String): List<String> = input.trim().let {
        if (it.isBlank()) {
            emptyList()
        } else separatorRegex.split(it)
    }
}