package com.ajayinnovations.argus.core

/**
 * What the warning screen shows the phone's owner: the verdict, what was found, who sent it and
 * the start of the message. Held in memory only (VerdictStore); never logged, written to disk
 * or put in an Intent. [sender] and [preview] are null for test alerts and after a restart.
 */
data class Alert(
    val verdict: Verdict,
    val findings: List<Finding> = emptyList(),
    val source: SourceType? = null,
    val sender: String? = null,
    val preview: String? = null,
    /**
     * The whole message for the warning screen (scrollable, links shown but never clickable),
     * at most [MessagePreview.FULL_MAX] characters. Memory only, like [preview].
     */
    val message: String? = null,
    /** Hash of the chat it came from, for "one spoken be-careful per chat". Null for test alerts. */
    val chatId: Int? = null,
) {
    companion object {
        fun from(message: IncomingMessage, result: CheckResult) = Alert(
            verdict = result.verdict,
            findings = result.findings,
            source = message.type,
            sender = message.sender.takeIf { it.isNotBlank() && it != IncomingMessage.UNKNOWN_SENDER },
            preview = MessagePreview.keepingMarks(message.text, result.findings).takeIf { it.isNotEmpty() },
            message = MessagePreview.full(message.text).takeIf { it.isNotEmpty() },
            chatId = message.chatKey.hashCode(),
        )
    }
}
