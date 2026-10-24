package com.ajayinnovations.argus.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LanguageTest {

    @Test
    fun `known tags map to their language`() {
        assertEquals(Language.ENGLISH, Language.fromTag("en"))
        assertEquals(Language.HINDI, Language.fromTag("hi"))
    }

    @Test
    fun `tags are case insensitive`() {
        assertEquals(Language.HINDI, Language.fromTag("HI"))
    }

    @Test
    fun `unknown or missing tags fall back to English`() {
        assertEquals(Language.ENGLISH, Language.fromTag(null))
        assertEquals(Language.ENGLISH, Language.fromTag(""))
        assertEquals(Language.ENGLISH, Language.fromTag("yo"))
    }

    @Test
    fun `every language round trips through its tag`() {
        Language.entries.forEach { assertEquals(it, Language.fromTag(it.tag)) }
    }

    @Test
    fun `per-app locale tags map to a language`() {
        assertEquals(Language.HINDI, Language.fromLocaleTags("hi"))
        assertEquals(Language.ENGLISH, Language.fromLocaleTags("en"))
        assertEquals(Language.ENGLISH, Language.fromLocaleTags("en-IN"))
        assertEquals(Language.HINDI, Language.fromLocaleTags("hi-IN"))
        assertEquals(Language.ENGLISH, Language.fromLocaleTags("en_GB"))
    }

    @Test
    fun `first supported tag wins`() {
        assertEquals(Language.HINDI, Language.fromLocaleTags("fr,hi,en"))
        assertEquals(Language.ENGLISH, Language.fromLocaleTags(" en-US , hi"))
    }

    @Test
    fun `unset or unsupported per-app locale keeps the current choice`() {
        assertNull(Language.fromLocaleTags(null))
        assertNull(Language.fromLocaleTags(""))
        assertNull(Language.fromLocaleTags("fr,yo"))
    }
}
