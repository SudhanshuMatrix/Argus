package com.ajayinnovations.argus.core

/**
 * Names of the bundled clips in res/raw: `{level}_{language}_{title}`, e.g. `scam_en_mummy`.
 * Only [RECORDED] languages ship clips: 3 levels × 1 language × 4 titles = 12 files. Hindi
 * clips aren't recorded yet, so Hindi asks for its own clips first and then falls back to
 * English; adding `scam_hi_mummy.mp3` and friends to res/raw is enough to switch them on.
 */
object VoiceClips {

    /** Languages whose clips are bundled. */
    val RECORDED: Set<Language> = setOf(Language.ENGLISH)

    /**
     * The clip for this parent, then the neutral one, in the parent's language and then in
     * English. Missing clips are skipped by the player. Empty for NONE.
     */
    fun candidates(level: Level, language: Language, title: ParentTitle): List<String> {
        val prefix = when (level) {
            Level.SCAM -> "scam"
            Level.CAREFUL -> "careful"
            Level.OTP -> "otp"
            Level.NONE -> return emptyList()
        }
        return listOf(language, Language.ENGLISH).distinct().flatMap { lang ->
            val base = "${prefix}_${lang.tag}"
            listOf("${base}_${title.key}", "${base}_${ParentTitle.NEUTRAL.key}")
        }.distinct()
    }

    /** Every bundled clip name. */
    fun all(): List<String> =
        listOf(Level.SCAM, Level.CAREFUL, Level.OTP).flatMap { level ->
            RECORDED.flatMap { language -> ParentTitle.entries.map { candidates(level, language, it).first() } }
        }
}
