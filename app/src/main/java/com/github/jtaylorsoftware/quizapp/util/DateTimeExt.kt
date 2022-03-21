package com.github.jtaylorsoftware.quizapp.util

import java.time.Instant
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Returns a localized string representing this date, with the format "LongMonth Day, Year"
 * (see [FormatStyle.LONG]).
 */
fun Instant.toLocalizedString(): String =
    this.atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))

/**
 * Returns true if this [Instant] represents a time in the past.
 */
fun Instant.isInPast(): Boolean = this < Instant.now()

/**
 * Returns a [Period] with the end date of [LocalDate.now].
 */
fun Instant.periodBetweenNow(): Period = Period.between(
    this.atZone(ZoneId.systemDefault()).toLocalDate(),
    LocalDate.now()
)

/**
 * Returns a String describing the maximum value of the [Period].
 * A [Period] with 1 year would return `"1 year$suffix"` even if
 * months and days are nonzero (and similarly if years are zero with nonzero months).
 *
 * @param suffix The suffix to include after the max String. Excluded if
 *               the [Period.isZero] would return true.
 */
fun Period.describeMax(suffix: String = ""): String = when {
    years != 0 -> {
        "$years year${if (years != 1) "s" else ""}$suffix"
    }
    months != 0 -> {
        "$months month${if (months != 1) "s" else ""}$suffix"
    }
    days != 0 -> {
        "$days day${if (days != 1) "s" else ""}$suffix"
    }
    else -> {
        "Today"
    }
}