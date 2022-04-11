package com.github.jtaylorsoftware.quizapp.ui

// TODO - Replace with string resources
enum class ErrorStrings(val message: String) {
     UNAUTHORIZED("You must sign-in again to access this."),
     FORBIDDEN("You are not allowed to access this."),
     NOT_FOUND("The requested quiz was not found."),
     UNKNOWN("Error loading the resource."),
     NETWORK("Network connection error occurred."),
}
