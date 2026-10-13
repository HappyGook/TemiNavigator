package com.example.teminavigator.ui.langs

enum class AppLanguage (
    val code : String,
    val nativeName: String, // name of language in this language, for settings
    val strings: AppStrings
) {
    GERMAN("de","Deutsch",  germanStrings), // TODO: make correct translations in all files
    ENGLISH("en","English",  englishStrings),
    PIRATE("en","Yarrr",pirateStrings),
    NADSAT("en","Nadsat",nadsatStrings),
    RUSSIAN("ru","Русский", russianStrings);

    companion object{
        val DEFAULT = GERMAN
        fun fromCode(code: String?) = entries.firstOrNull{it.code == code}?: DEFAULT
    }
}