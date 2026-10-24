package com.ajayinnovations.argus.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileFormTest {

    private val good = ProfileForm(title = ParentTitle.MUMMY, familyName = "Abraham", familyNumber = "0803 123 4567")

    @Test
    fun `a complete form has no problems`() = assertTrue(good.problems().isEmpty())

    @Test
    fun `each missing piece is reported`() {
        assertEquals(
            listOf(ProfileForm.Problem.NO_TITLE, ProfileForm.Problem.NO_FAMILY_NAME, ProfileForm.Problem.BAD_NUMBER),
            ProfileForm().problems(),
        )
    }

    @Test
    fun `their own name needs a name and saves as neutral`() {
        val custom = good.copy(title = null, usesCustomName = true)
        assertEquals(listOf(ProfileForm.Problem.NO_CUSTOM_NAME), custom.problems())
        val named = custom.copy(customName = " Mama Chidi ")
        assertTrue(named.problems().isEmpty())
        assertEquals(ParentTitle.NEUTRAL, named.savedTitle)
        assertEquals("Mama Chidi", named.savedCustomName)
    }

    @Test
    fun `phone numbers`() {
        listOf("08031234567", "0803 123 4567", "+234 803 123 4567", "(0803) 123-4567").forEach { assertTrue(it, PhoneNumbers.isValid(it)) }
        listOf("", "123", "0803abc4567", "+", "1234567890123456").forEach { assertFalse(it, PhoneNumbers.isValid(it)) }
    }
}
