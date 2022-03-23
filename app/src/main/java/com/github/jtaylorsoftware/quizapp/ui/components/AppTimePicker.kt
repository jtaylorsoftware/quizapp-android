package com.github.jtaylorsoftware.quizapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.github.jtaylorsoftware.quizapp.ui.theme.QuizAppTheme
import java.time.LocalTime

/**
 * Displays a Dialog that allows the user to manually input a [LocalTime] by using
 * numeric [TextField] inputs.
 *
 * The hour [TextField] uses `contentDescription` of `"Hour of day"`
 * The minute [TextField] uses `contentDescription` of `"Minute of hour"`
 * The AM [Button] uses `contentDescription` of `"Set time to AM"`
 * The PM [Button] uses `contentDescription` of `"Set time to PM"`
 *
 * @param value The default time value.
 * @param open Whether this Dialog should be open.
 * @param onDismiss Callback invoked when the Dialog is dismissed. Receives the current time input
 *                  as its parameter.
 */
@Composable
fun AppTimePicker(
    value: LocalTime,
    open: Boolean,
    onDismiss: (LocalTime) -> Unit,
) {
    var localValue: LocalTime by remember { mutableStateOf(LocalTime.of(value.hour, value.minute)) }
    if (open) {
        Dialog(onDismissRequest = {
            onDismiss(localValue)
        }) {
            AppTimePickerContent(localValue, onTimeChange = { localValue = it })
        }
    }
}

/**
 * The body of the AppTimePicker without the Dialog.
 */
@Composable
private fun AppTimePickerContent(
    value: LocalTime,
    onTimeChange: (LocalTime) -> Unit
) {
    var hour: Int by remember { mutableStateOf(if (value.hour % 12 == 0) 12 else value.hour % 12) }
    var minute: Int by remember { mutableStateOf(value.minute) }
    var period: String by remember(value.hour) { mutableStateOf(if (value.hour < LocalTime.NOON.hour) "AM" else "PM") }
    val minuteFocusRequester: FocusRequester = remember { FocusRequester() }

    fun adjustTime(newHour: Int = hour, newMinute: Int = minute, newPeriod: String = period) {
        if (newPeriod == "AM") {
            onTimeChange(LocalTime.of(newHour, newMinute))
        } else {
            onTimeChange(LocalTime.of((newHour % 12) + 12, newMinute))
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
                    "Select Time",
                    color = MaterialTheme.colors.onPrimary,
                    style = MaterialTheme.typography.h5,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                HourPicker(hour = hour, onHourChange = {
                    hour = it
                    adjustTime(newHour = it)
                }, minuteFocusRequester = minuteFocusRequester)
                Spacer(Modifier.width(8.dp))
                MinutePicker(minute = minute, onMinuteChange = {
                    minute = it
                    adjustTime(newMinute = it)
                }, focusRequester = minuteFocusRequester)
                Spacer(Modifier.width(8.dp))
                PeriodPicker(period = period, onPeriodChange = {
                    period = it
                    adjustTime(newPeriod = it)
                })
            }
        }
    }
}

@Preview
@Composable
private fun AppTimePickerContentPreview() {
    QuizAppTheme {
        AppTimePickerContent(value = LocalTime.now(), onTimeChange = {})
    }
}

/**
 * Transforms a String into an appropriate hour value. Should be called
 * on every progressive input character.
 *
 * @return A valid hour value, or -1 if the input should be discarded.
 */
private fun String.transformHour(): Int =
    try {
        val hour = this.trim(' ', '-', '+', ',', '.').toInt()
        if (hour <= 12) {
            hour
        } else -1
    } catch (ex: java.lang.NumberFormatException) {
        -1
    }


/**
 * Displays a column with the [Text] label "Hour" and a [TextField] for inputting the hour.
 * [onHourChange] will only be called with a valid hour in `[0,12]`
 */
@Composable
private fun HourPicker(
    hour: Int,
    onHourChange: (Int) -> Unit,
    minuteFocusRequester: FocusRequester
) {
    var hourText: String by remember { mutableStateOf(hour.toString().padStart(2, '0')) }

    Column(
        modifier = Modifier.padding(horizontal = 0.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Hour")
        Spacer(Modifier.height(8.dp))
        TextField(
            value = TextFieldValue(text = hourText, selection = TextRange(hourText.length)),
            onValueChange = {
                val text = it.text
                if (text.isNotBlank()) {
                    onHourChange(text.transformHour().let { hr ->
                        if (hr == -1) {
                            hour
                        } else {
                            hourText = hr.toString()
                            hr
                        }
                    })
                } else {
                    hourText = text
                }
                if (hourText.length == 2) {
                    minuteFocusRequester.requestFocus()
                }
            },
            textStyle = MaterialTheme.typography.h3.copy(textAlign = TextAlign.Center),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .semantics { contentDescription = "Hour of day" }
                .width(112.dp)
        )
    }
}

/**
 * Transforms a String into an appropriate minute value. Should be called
 * on every progressive input character.
 *
 * @return A valid minute value, or -1 if the input should be discarded.
 */
private fun String.transformMinute(): Int =
    try {
        val minute = this.trim(' ', '-', '+', ',', '.').toInt()
        if (minute <= 59) {
            minute
        } else -1
    } catch (ex: java.lang.NumberFormatException) {
        -1
    }


/**
 * Renders a column with the [Text] label "Minute" and a [TextField] for inputting the minute.
 * [onMinuteChange] will only be called with a valid minute value in `[0, 59]`
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun MinutePicker(
    minute: Int,
    onMinuteChange: (Int) -> Unit,
    focusRequester: FocusRequester
) {
    var minuteText: String by remember { mutableStateOf(minute.toString().padStart(2, '0')) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier.padding(horizontal = 0.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Minute")
        Spacer(Modifier.height(8.dp))
        TextField(
            value = TextFieldValue(text = minuteText, selection = TextRange(minuteText.length)),
            onValueChange = {
                val text = it.text
                if (text.isNotBlank()) {
                    onMinuteChange(text.transformMinute().let { mn ->
                        if (mn == -1) {
                            minute
                        } else {
                            minuteText = if (minuteText == "0") {
                                mn.toString().padStart(2, '0')
                            } else mn.toString()

                            mn
                        }
                    })
                } else {
                    minuteText = text
                }
            },
            textStyle = MaterialTheme.typography.h3.copy(textAlign = TextAlign.Center),
            singleLine = true,
            keyboardActions = KeyboardActions(onDone = {
                keyboardController?.hide()
                focusManager.clearFocus()
                minuteText = minuteText.padStart(2, '0')
            }),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier
                .semantics { contentDescription = "Minute of hour" }
                .width(112.dp)
                .focusRequester(focusRequester)
        )
    }
}

@Preview
@Composable
private fun MinutePickerPreview() {
    MinutePicker(10, {}, FocusRequester())
}

/**
 * Displays two selectable buttons, one for "AM" and one for "PM," where pressing a button
 * will invoke [onPeriodChange] with the value displayed on the button.
 */
@Composable
private fun PeriodPicker(period: String, onPeriodChange: (String) -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 0.dp, vertical = 24.dp)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        OutlinedButton(onClick = { onPeriodChange("AM") }, modifier = Modifier.semantics {
            contentDescription = "Set time to AM"
        }) {
            CompositionLocalProvider(
                LocalContentAlpha provides if (period == "AM") ContentAlpha.high else ContentAlpha.disabled
            ) {
                Text("AM")
            }
        }
        OutlinedButton(onClick = { onPeriodChange("PM") }, modifier = Modifier.semantics {
            contentDescription = "Set time to PM"
        }) {
            CompositionLocalProvider(
                LocalContentAlpha provides if (period == "PM") ContentAlpha.high else ContentAlpha.disabled
            ) {
                Text("PM")
            }
        }
    }
}

@Preview(heightDp = 200)
@Composable
private fun PeriodPickerPreview() {
    PeriodPicker("AM", {})
}