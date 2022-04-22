package com.github.jtaylorsoftware.quizapp.data.domain

// TODO - Replace with strings with resources
enum class SuccessStrings(val value: String) {
     DEFAULT("Success."),
     UPLOADED_QUIZ("Quiz uploaded successfully."),
     SUBMITTED_QUIZ_RESPONSE("Submitted responses successfully."),
     DELETED_QUIZ("Quiz deleted successfully."),
     EMAIL_CHANGED("Email updated successfully."),
     PASSWORD_CHANGED("Password updated successfully.")
}

// TODO - Replace with strings with resources
/**
 * Generic reasons for an operation to fail.
 */
enum class FailureReason(val value: String) {
     UNAUTHORIZED("You must login again to access this."),
     FORBIDDEN("You are not allowed to access this."),
     NOT_FOUND("The requested data was not found."),
     UNKNOWN("Unknown error occurred."),
     NETWORK("Network error occurred."),
     FORM_HAS_ERRORS("Please fix any errors and try again."),
     QUIZ_EXPIRED("This quiz has expired.")
}
