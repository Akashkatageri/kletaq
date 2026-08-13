package com.kletaq.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsernameValidatorTest {

    @Test
    fun testValidUsernames() {
        val validUsernames = listOf(
            "akash",
            "akash_k1",
            "student_2026",
            "john_doe",
            "usr",
            "a_b_c_d_e_f_g_h_i_j"
        )
        for (username in validUsernames) {
            assertTrue("Expected valid: $username", UsernameValidator.isValidUsername(username))
            assertNull("Expected no error for: $username", UsernameValidator.getValidationError(username))
            assertFalse("Expected no blocked word for: $username", UsernameValidator.containsBlockedWord(username))
        }
    }

    @Test
    fun testOffensiveUsernames() {
        val offensiveUsernames = listOf(
            "gandu",
            "gandukumar",
            "fuck",
            "nigga",
            "rape",
            "bitch",
            "bastard",
            "asshole",
            "porn",
            "sex",
            "slut",
            "whore",
            "penis",
            "pussy",
            "dick",
            "cunt"
        )
        for (username in offensiveUsernames) {
            assertFalse("Expected invalid: $username", UsernameValidator.isValidUsername(username))
            assertTrue("Expected containsBlockedWord true for: $username", UsernameValidator.containsBlockedWord(username))
            assertEquals("Username contains inappropriate language.", UsernameValidator.getValidationError(username))
        }
    }

    @Test
    fun testBypassAttempts() {
        val bypassAttempts = listOf(
            "g4ndu",
            "f_u_c_k",
            "n1gga",
            "ra.pe",
            "s3x",
            "p0rn",
            "b1tch",
            "d1ck",
            "b4st4rd"
        )
        for (username in bypassAttempts) {
            assertFalse("Expected bypass attempt to be caught for: $username", UsernameValidator.isValidUsername(username))
            assertTrue("Expected containsBlockedWord true for bypass attempt: $username", UsernameValidator.containsBlockedWord(username))
        }
    }

    @Test
    fun testNormalization() {
        assertEquals("gandu", UsernameValidator.normalizeUsername("g4ndu"))
        assertEquals("fuck", UsernameValidator.normalizeUsername("f_u_c_k"))
        assertEquals("nigga", UsernameValidator.normalizeUsername("n1gga"))
        assertEquals("rape", UsernameValidator.normalizeUsername("ra.pe"))
        assertEquals("sex", UsernameValidator.normalizeUsername("s3x"))
        assertEquals("porn", UsernameValidator.normalizeUsername("p0rn"))
    }

    @Test
    fun testEdgeCasesAndFormatRules() {
        // Too short (< 3)
        assertFalse(UsernameValidator.isValidUsername("ab"))
        assertEquals("Username is too short.", UsernameValidator.getValidationError("ab"))

        // Too long (> 20)
        val longUsername = "a".repeat(21)
        assertFalse(UsernameValidator.isValidUsername(longUsername))
        assertEquals("Username must be 20 characters or less.", UsernameValidator.getValidationError(longUsername))

        // Starts with underscore
        assertFalse(UsernameValidator.isValidUsername("_user"))
        assertEquals("Only letters, numbers, and underscores are allowed.", UsernameValidator.getValidationError("_user"))

        // Ends with underscore
        assertFalse(UsernameValidator.isValidUsername("user_"))
        assertEquals("Only letters, numbers, and underscores are allowed.", UsernameValidator.getValidationError("user_"))

        // Consecutive underscores
        assertFalse(UsernameValidator.isValidUsername("user__name"))
        assertEquals("Only letters, numbers, and underscores are allowed.", UsernameValidator.getValidationError("user__name"))

        // Invalid characters
        assertFalse(UsernameValidator.isValidUsername("user@name"))
        assertEquals("Only letters, numbers, and underscores are allowed.", UsernameValidator.getValidationError("user@name"))
    }
}
