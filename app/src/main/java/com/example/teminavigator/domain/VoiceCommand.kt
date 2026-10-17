package com.example.teminavigator.domain

import java.util.Locale

fun String.isPauseNavigationCommand(): Boolean {
    val words = lowercase(Locale.ROOT)
        .split(Regex("[^\\p{L}\\p{N}]+"))
        .filter(String::isNotEmpty)
    val commandWords = when {
        words.take(2) == listOf("hey", "temi") -> words.drop(2)
        words.firstOrNull() == "temi" -> words.drop(1)
        else -> words
    }
    return commandWords.size == 1 && commandWords.single() in setOf("stop", "halt")
}
