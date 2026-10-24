package com.ajayinnovations.argus.core

/** Languages the app speaks. [tag] is the BCP 47 tag used for resources and locales. */
enum class Language(val tag: String) {
    ENGLISH("en"),
    HINDI("hi");

    companion object {
        /** Unknown or missing tags fall back to English. */
        fun fromTag(tag: String?): Language =
            entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) } ?: ENGLISH

        /**
         * Android's per-app language as a comma-separated tag list ("hi", "en-IN,fr").
         * The first tag whose language we support wins; null when none is set or supported,
         * so the caller keeps its current choice.
         */
        fun fromLocaleTags(tags: String?): Language? =
            tags.orEmpty().split(',')
                .map { it.trim().substringBefore('-').substringBefore('_') }
                .firstNotNullOfOrNull { base -> entries.firstOrNull { it.tag.equals(base, ignoreCase = true) } }
    }
}
