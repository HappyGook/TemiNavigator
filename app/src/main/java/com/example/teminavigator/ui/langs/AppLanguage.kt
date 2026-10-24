package com.example.teminavigator.ui.langs

/**
 * Languages the app can be used in.
 *
 * Each entry bundles a language [code], its display name for the settings
 * and the matching [AppStrings]. The entries are listed in the settings so the
 * user can switch between them.
 *
 * @property code Language code, e.g. "de", "en" or "ru". Used to store and
 * restore the selection ([fromCode]). Several entries may share a code, as the
 * variants of English do.
 * @property nativeName Name of the language written in that language
 * (e.g. "Deutsch"), shown in the settings so users recognize their language.
 * @property strings All texts of this language.
 */
enum class AppLanguage(
    val code: String,
    val nativeName: String, // name of language in this language, for settings
    val strings: AppStrings,
) {

    /** German, the default language. */
    GERMAN("de", "Deutsch", germanStrings),

    /** Standard English. */
    ENGLISH("en", "English", englishStrings),

    /** English in pirate speak, a fun variant. Shares the code "en" with [ENGLISH]. */
    PIRATE("en", "Yarrr", pirateStrings),

    /** English in Nadsat, the slang from *A Clockwork Orange*, a fun variant. Shares the code "en" with [ENGLISH]. */
    NADSAT("en", "Nadsat", nadsatStrings),

    /** Russian. */
    RUSSIAN("ru", "Русский", russianStrings),
    ;

    /** Defaults and lookup for [AppLanguage]. */
    companion object {
        val DEFAULT = GERMAN

        /**
         * Finds the language belonging to a stored language code.
         *
         * Returns the first entry in declaration order with a matching [code]. Because
         * [ENGLISH], [PIRATE] and [NADSAT] all use "en", the code "en" always resolves
         * to [ENGLISH].
         *
         * @param code Language code, or null if none is stored.
         * @return The matching language, or [DEFAULT] if [code] is null or unknown.
         */
        fun fromCode(code: String?) = entries.firstOrNull { it.code == code } ?: DEFAULT
    }
}
