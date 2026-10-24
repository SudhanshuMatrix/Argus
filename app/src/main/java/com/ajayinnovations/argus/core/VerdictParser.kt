package com.ajayinnovations.argus.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * Small models wrap JSON in code fences or chat, use lowercase levels, or drop fields.
 * Finds the first complete {...} object and reads it leniently. Null means unusable.
 */
object VerdictParser {

    private val json = Json { isLenient = true }

    fun parse(raw: String): Verdict? {
        val obj = firstJsonObject(raw)
        return if (obj != null) {
            verdict(obj.string("level"), obj.string("reason"), obj.string("advice"))
        } else {
            // Not valid JSON even leniently (e.g. single quotes): read the fields one by one
            verdict(field(raw, "level"), field(raw, "reason"), field(raw, "advice"))
        }
    }

    private fun verdict(level: String?, reason: String?, advice: String?): Verdict? {
        val parsed = when (level?.trim()?.uppercase()) {
            "SCAM" -> Level.SCAM
            "CAREFUL" -> Level.CAREFUL
            // Only OtpCheck may produce OTP; the model saying so means "be careful"
            "OTP" -> Level.CAREFUL
            "NONE" -> Level.NONE
            else -> return null
        }
        return Verdict(parsed, reason.orEmpty().trim(), advice.orEmpty().trim())
    }

    /** `key: "value"` or `'key': 'value'`, quoted with matching quotes. */
    private fun field(raw: String, key: String): String? =
        Regex("""["']?$key["']?\s*:\s*(["'])(.*?)\1""", RegexOption.IGNORE_CASE).find(raw)?.groupValues?.get(2)

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    /** Tries each '{' in turn and returns the first balanced object that parses. */
    private fun firstJsonObject(raw: String): JsonObject? {
        var start = raw.indexOf('{')
        while (start >= 0) {
            val end = matchingBrace(raw, start)
            if (end > start) {
                runCatching { json.parseToJsonElement(raw.substring(start, end + 1)).jsonObject }
                    .getOrNull()?.let { return it }
            }
            start = raw.indexOf('{', start + 1)
        }
        return null
    }

    /** Index of the brace closing the one at [open], skipping braces inside strings; -1 if none. */
    private fun matchingBrace(s: String, open: Int): Int {
        var depth = 0
        var inString = false
        var escaped = false
        for (i in open until s.length) {
            val c = s[i]
            when {
                escaped -> escaped = false
                inString && c == '\\' -> escaped = true
                c == '"' -> inString = !inString
                inString -> Unit
                c == '{' -> depth++
                c == '}' -> if (--depth == 0) return i
            }
        }
        return -1
    }
}
