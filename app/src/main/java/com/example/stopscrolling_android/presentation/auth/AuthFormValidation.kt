package com.example.stopscrolling_android.presentation.auth

object AuthFormValidation {
    const val MINIMUM_PASSWORD_LENGTH = 8

    fun signInError(email: String, password: String): String? {
        val trimmedEmail = normalizedEmail(email)
        if (trimmedEmail.isEmpty()) return "Enter your email address."
        if (!isValidEmail(trimmedEmail)) return "Enter a valid email address."
        if (password.isEmpty()) return "Enter your password."
        return null
    }

    fun signUpError(
        email: String,
        password: String,
        confirmPassword: String,
        phoneNumber: String
    ): String? {
        val trimmedEmail = normalizedEmail(email)
        if (trimmedEmail.isEmpty()) return "Enter your email address."
        if (!isValidEmail(trimmedEmail)) return "Enter a valid email address."
        if (password.length < MINIMUM_PASSWORD_LENGTH) {
            return "Password must be at least $MINIMUM_PASSWORD_LENGTH characters."
        }
        if (confirmPassword != password) return "Passwords do not match."
        val trimmedPhone = phoneNumber.trim()
        if (trimmedPhone.isNotEmpty() && !trimmedPhone.startsWith("+")) {
            return "Phone numbers must use international format, e.g. +15551234567."
        }
        return null
    }

    fun normalizedEmail(email: String): String {
        return email.trim().lowercase()
    }

    private fun isValidEmail(email: String): Boolean {
        return email.matches(Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
    }
}
