package com.ajayinnovations.argus.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlertTest {

    private val result = CheckResult(Verdict(Level.SCAM), DecidedBy.HARD_RULE, 2, findings = listOf(Finding.Link))

    @Test
    fun `keeps sender, source, findings and a short preview`() {
        val text = "Dear customer,\nyour BVN is blocked. " + "Click the link now. ".repeat(20)
        val alert = Alert.from(IncomingMessage(SourceType.WHATSAPP, text, "GTBank Care"), result)
        assertEquals("GTBank Care", alert.sender)
        assertEquals(SourceType.WHATSAPP, alert.source)
        assertEquals(listOf(Finding.Link), alert.findings)
        assertEquals(MessagePreview.keepingMarks(text, result.findings), alert.preview)
        assertEquals(true, (alert.preview?.length ?: 0) <= MessagePreview.HARD_MAX + 1)
    }

    @Test
    fun `an unknown sender is not shown`() =
        assertNull(Alert.from(IncomingMessage(SourceType.SMS, "hello there", IncomingMessage.UNKNOWN_SENDER), result).sender)
}
