package com.ajayinnovations.argus.core

/** Who Argus protects and who to call. Shared by the wizard and the settings screen. */
data class ProfileForm(
    /** Null until one is picked. NEUTRAL with [customName] means "their name". */
    val title: ParentTitle? = null,
    val customName: String = "",
    /** True when "Their name" is picked. */
    val usesCustomName: Boolean = false,
    val familyName: String = "",
    val familyNumber: String = "",
    val familyAlerts: Boolean = false,
) {
    enum class Problem { NO_TITLE, NO_CUSTOM_NAME, NO_FAMILY_NAME, BAD_NUMBER }

    fun problems(): List<Problem> = buildList {
        if (title == null && !usesCustomName) add(Problem.NO_TITLE)
        if (usesCustomName && customName.isBlank()) add(Problem.NO_CUSTOM_NAME)
        if (familyName.isBlank()) add(Problem.NO_FAMILY_NAME)
        if (!PhoneNumbers.isValid(familyNumber)) add(Problem.BAD_NUMBER)
    }

    /** The title to save: their own name uses the neutral voice clips. */
    val savedTitle: ParentTitle get() = if (usesCustomName) ParentTitle.NEUTRAL else title ?: ParentTitle.NEUTRAL
    val savedCustomName: String get() = if (usesCustomName) customName.trim() else ""
}

object PhoneNumbers {
    private val separators = Regex("""[\s\-().]""")

    /** 7 to 15 digits, with an optional leading +; spaces, dashes and brackets are fine. */
    fun isValid(number: String): Boolean {
        val n = number.trim().replace(separators, "")
        val digits = n.removePrefix("+")
        return digits.length in 7..15 && digits.all { it.isDigit() }
    }
}
