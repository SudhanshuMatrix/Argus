package com.ajayinnovations.argus.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelHeaderTest {

    @Test
    fun `a real model header is valid`() =
        assertTrue(ModelHeader.isValid("LITERTLM".toByteArray() + byteArrayOf(1, 0, 0, 0)))

    @Test
    fun `random bytes, a short file and an empty file are not`() {
        assertFalse(ModelHeader.isValid(byteArrayOf(0x4c, 0x49, 0x54, 0x45, 0x52, 0x54, 0x4c, 0x00)))
        assertFalse(ModelHeader.isValid("LITE".toByteArray()))
        assertFalse(ModelHeader.isValid(ByteArray(0)))
        assertFalse(ModelHeader.isValid("<html>error page</html>".toByteArray()))
    }
}
