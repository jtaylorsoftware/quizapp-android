package com.github.jtaylorsoftware.quizapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import java.time.DateTimeException
import java.time.LocalDate
import java.time.Month
import java.time.Year
import java.time.format.TextStyle
import java.util.*

/**
 * Displays a Dialog that allows the user to select a [LocalDate] by using dropdowns. Only valid dates can
 * be selected by combining dropdown items - if an invalid date is created, the day of month
 * is reset to 1.
 *
 * The years are not infinite, so reasonable bounds should be provided if the defaults are not acceptable.
 *
 * Month button's `contentDescription` is `"Select month"`. Month items use [Text] with the Month formatted using
 * [TextStyle.SHORT] in the current locale.
 *
 * Day button's `contentDescription` is `"Select day of the month"`. Day items use [Text] with just the day value.
 *
 * Year button's `contentDescription` is `"Select year"`. Year items use [Text] with just the year value.
 *
 * @param value The current [LocalDate] value
 *
 * @param minYear The minimum year to offer as a selection. Defaults to current year.
 *
 * @param maxYear The maximum year to offer as a selection. Defaults to current year + 10.
 *
 * @param open Flag controlling visibility of the dialog.
 *
 * @param onDismiss Callback invoked when the user clicks outside the dialog.
 */
@Composable
fun AppDatePicker(
    value: LocalDate,
    onValueChange: (LocalDate) -> Unit,
    minYear: Int = Year.now().value,
    maxYear: Int = minYear + 10,
    open: Boolean,
    onDismiss: () -> Unit,
) {
    if (open) {
        Dialog(onDismissRequest = onDismiss) {
            AppDatePickerContent(
                value = value,
                onDateChange = onValueChange,
                minYear = minYear,
                maxYear = maxYear
            )
        }
    }
}

/**
 * The body of the AppDatePicker without the Dialog.
 */
@Composable
private fun AppDatePickerContent(
    value: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    minYear: Int,
    maxYear: Int,
) {
    fun adjustDate(newYear: Int = value.year, newMonth: Month = value.month, newDay: Int = value.dayOfMonth) {
        try {
            onDateChange(LocalDate.of(newYear, newMonth, newDay))
        } catch (ex: DateTimeException) {
            // Day is not valid for the month, reset to safe value (first of month)
            onDateChange(LocalDate.of(newYear, newMonth, 1))
        }
    }

    Surface(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colors.primary)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Select Date",
                    color = MaterialTheme.colors.onPrimary,
                    style = MaterialTheme.typography.h5,
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                MonthPicker(value.month, onMonthSelected = {
                    adjustDate(newMonth = it)
                })
                Spacer(Modifier.width(8.dp))
                DayPicker(
                    month = value.month,
                    year = value.year,
                    day = value.dayOfMonth,
                    onDaySelected = {
                        adjustDate(newDay = it)
                    })
                Spacer(Modifier.width(8.dp))
                YearPicker(year = value.year, onYearSelected = {
                    adjustDate(newYear = it)
                }, min = minYear, max = maxYear)
            }
        }
    }
}

@Preview(widthDp = 300, heightDp = 208, showBackground = true)
@Composable
private fun AppDatePickerContentPreview() {
    val year = Year.now().value
    QuizAppTheme {
        AppDatePickerContent(
            value = LocalDate.now(),
            onDateChange = {},
            minYear = year,
            maxYear = year + 10
        )
    }
}

/**
 * A dropdown menu allowing selection of a Month.
 *
 * @param month The default selected month.
 * @param onMonthSelected Callback invoked when a dropdown item is clicked.
 */
@Composable
private fun MonthPicker(month: Month, onMonthSelected: (Month) -> Unit) {
    var expanded: Boolean by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier.padding(horizontal = 0.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Month")
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.semantics {
            contentDescription = "Select month"
        }) {
            Text(month.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.requiredHeightIn(max = 208.dp)
        ) {
            Month.values().forEach {
                DropdownMenuItem(onClick = {
                    onMonthSelected(it)
                    expanded = false
                }) {
                    Text(it.getDisplayName(TextStyle.SHORT, Locale.getDefault()))
                }
            }
        }
    }
}

@Preview
@Composable
private fun MonthPickerPreview() {
    MonthPicker(Month.APRIL, {})
}

/**
 * A dropdown menu giving a range of days in a month to pick from.
 *
 * @param month The month to use when calculating days in the month.
 * @param year The year to use when calculating days in the month.
 * @param day The default selected day.
 * @param onDaySelected Called when a dropdown item is selected.
 */
@Composable
private fun DayPicker(month: Month, year: Int, day: Int, onDaySelected: (Int) -> Unit) {
    var expanded: Boolean by rememberSaveable { mutableStateOf(false) }
    val daysInMonth: Int by derivedStateOf {
        LocalDate.of(year, month.value, 1).lengthOfMonth()
    }

    Column(
        modifier = Modifier.padding(horizontal = 0.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Day")
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.semantics {
            contentDescription = "Select day of the month"
        }) {
            Text("$day")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.requiredHeightIn(max = 208.dp)
        ) {
            (1..daysInMonth).forEach {
                DropdownMenuItem(onClick = {
                    onDaySelected(it)
                    expanded = false
                }) {
                    Text("$it")
                }
            }
        }
    }
}

@Preview
@Composable
private fun DayPickerPreview() {
    val now = LocalDate.now()
    DayPicker(now.month, now.year, 1, {})
}

/**
 * A dropdown menu giving a range of years to pick from.
 *
 * @param year The default selected year.
 * @param min The minimum year to list. Defaults to the current year.
 * @param max The maximum year to list. Defaults to min + 10.
 * @param onYearSelected Callback invoked when a dropdown item is clicked.
 */
@Composable
private fun YearPicker(
    year: Int,
    min: Int = Year.now().value,
    max: Int = min + 10,
    onYearSelected: (Int) -> Unit
) {
    var expanded: Boolean by rememberSaveable { mutableStateOf(false) }
    Column(
        modifier = Modifier.padding(horizontal = 0.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Year")
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.semantics {
            contentDescription = "Select year"
        }) {
            Text("$year")
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.requiredHeightIn(max = 208.dp)
        ) {
            (min..max).forEach {
                DropdownMenuItem(onClick = {
                    onYearSelected(it)
                    expanded = false
                }) {
                    Text("$it")
                }
            }
        }
    }
}

@Preview
@Composable
private fun YearPickerPreview() {
    val now = LocalDate.now()
    YearPicker(now.year, now.year, onYearSelected = {})
}