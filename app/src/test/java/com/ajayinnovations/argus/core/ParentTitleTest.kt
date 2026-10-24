package com.ajayinnovations.argus.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ParentTitleTest {

    @Test
    fun `known keys map to their title`() {
        assertEquals(ParentTitle.MUMMY, ParentTitle.fromKey("mummy"))
        assertEquals(ParentTitle.DADDY, ParentTitle.fromKey("daddy"))
        assertEquals(ParentTitle.GRANDMA, ParentTitle.fromKey("grandma"))
    }

    @Test
    fun `keys are case insensitive`() {
        assertEquals(ParentTitle.GRANDMA, ParentTitle.fromKey("Grandma"))
    }

    @Test
    fun `unknown or missing keys fall back to neutral`() {
        assertEquals(ParentTitle.NEUTRAL, ParentTitle.fromKey(null))
        assertEquals(ParentTitle.NEUTRAL, ParentTitle.fromKey("uncle"))
    }

    @Test
    fun `every title round trips through its key`() {
        ParentTitle.entries.forEach { assertEquals(it, ParentTitle.fromKey(it.key)) }
    }
}
