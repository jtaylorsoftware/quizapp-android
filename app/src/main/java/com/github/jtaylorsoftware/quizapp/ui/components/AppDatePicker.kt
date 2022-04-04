package com.github.jtaylorsoftware.quizapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
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
 * @param defaultValue The default [LocalDate] to use.
 * @param minYear The minimum year to offer as a selection. Defaults to current year.
 * @param maxYear The maximum year to offer as a selection. Defaults to current year + 10.
 * @param open Flag controlling visibility of the dialog.
 * @param onDismiss Callback invoked when the user clicks outside the dialog. Receives the current input
 *                  as its argument.
 */
@Composable
fun AppDatePicker(
    defaultValue: LocalDate,
    minYear: Int = Year.now().value,
    maxYear: Int = minYear + 10,
    open: Boolean,
    onDismiss: (LocalDate) -> Unit,
) {
    var localValue: LocalDate by remember {
        mutableStateOf(
            LocalDate.of(
                defaultValue.year,
                defaultValue.month,
                defaultValue.dayOfMonth
            )
        )
    }
    if (open) {
        Dialog(onDismissRequest = { onDismiss(localValue) }) {
            AppDatePickerContent(
                value = defaultValue,
                onDateChange = { localValue = it },
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
    var month: Month by remember { mutableStateOf(value.month) }
    var year: Int by remember { mutableStateOf(value.year) }
    var day: Int by remember { mutableStateOf(value.dayOfMonth) }

    fun adjustDate(newYear: Int = year, newMonth: Month = month, newDay: Int = day) {
        try {
            onDateChange(LocalDate.of(newYear, newMonth, newDay))
        } catch (ex: DateTimeException) {
            // Day is not valid for the month, reset to safe value
            day = 1
            onDateChange(LocalDate.of(year, month, day))
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
                MonthPicker(month, onMonthSelected = {
                    month = it
                    adjustDate(newMonth = it)
                })
                Spacer(Modifier.width(8.dp))
                DayPicker(
                    month = month,
                    year = year,
                    day = day,
                    onDaySelected = {
                        day = it
                        adjustDate(newDay = it)
                    })
                Spacer(Modifier.width(8.dp))
                YearPicker(year = year, onYearSelected = {
                    year = it
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
    var expanded: Boolean by remember { mutableStateOf(false) }
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
    var expanded: Boolean by remember { mutableStateOf(false) }
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
    var expanded: Boolean by remember { mutableStateOf(false) }
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