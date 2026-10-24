package com.ajayinnovations.argus.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class VoiceClipsTest {

    @Test
    fun `picks the parent's clip, then neutral in the same language`() =
        assertEquals(
            listOf("scam_en_mummy", "scam_en_neutral"),
            VoiceClips.candidates(Level.SCAM, Language.ENGLISH, ParentTitle.MUMMY),
        )

    @Test
    fun `Hindi tries its own clips, then falls back to English`() =
        assertEquals(
            listOf("scam_hi_mummy", "scam_hi_neutral", "scam_en_mummy", "scam_en_neutral"),
            VoiceClips.candidates(Level.SCAM, Language.HINDI, ParentTitle.MUMMY),
        )

    @Test
    fun `neutral title has one candidate`() =
        assertEquals(listOf("otp_en_neutral"), VoiceClips.candidates(Level.OTP, Language.ENGLISH, ParentTitle.NEUTRAL))

    @Test
    fun `NONE has no clip`() = assertTrue(VoiceClips.candidates(Level.NONE, Language.ENGLISH, ParentTitle.DADDY).isEmpty())

    @Test
    fun `12 clip names`() = assertEquals(12, VoiceClips.all().toSet().size)

    @Test
    fun `every clip is bundled in res raw`() {
        // Unit tests run with the module directory as the working directory
        val raw = File("src/main/res/raw")
        val missing = VoiceClips.all().filterNot { File(raw, "$it.mp3").isFile }
        assertTrue("Missing clips: $missing", missing.isEmpty())
    }
}
