package com.example.teminavigator.domain

import java.text.Normalizer

/**
 * match method for the List<Destination> matching based on aliases and display names
 * for speech recognition
  */

fun List<Destination>.match(text: String): Destination? {
    val query = text.normalizeForMatch()
    if (query.isEmpty()) return null

    // exact match on id/displayname/alias
    firstOrNull { dest ->
        (dest.aliases + dest.displayName + dest.id).any { it.normalizeForMatch() == query }
    }
        ?.let { return it }

    // query contains a known name
    val containingMatch = filter {
        dest -> (dest.aliases + dest.displayName + dest.id).any { name ->
            name.normalizeForMatch().let { normalizedName ->
                normalizedName.isNotEmpty() && query.contains(normalizedName)
            }
        }
    }.maxByOrNull { dest ->
        (dest.aliases + dest.displayName + dest.id).maxOf { it.normalizeForMatch().length }
    }
    if (containingMatch != null) return containingMatch

    val queryWords = query.split(' ')
    return mapNotNull { destination ->
        val bestMatch = (destination.aliases + destination.displayName + destination.id)
            .asSequence()
            .map(String::normalizeForMatch)
            .filter(String::isNotEmpty)
            .mapNotNull { name ->
                val nameWords = name.split(' ')
                val minWindow = (nameWords.size - 1).coerceAtLeast(1)
                val maxWindow = (nameWords.size + 1).coerceAtMost(queryWords.size)
                val distance = (minWindow..maxWindow).flatMap { windowSize ->
                    queryWords.windowed(windowSize).map { window ->
                        editDistance(name.replace(" ", ""), window.joinToString(""))
                    }
                }.minOrNull() ?: return@mapNotNull null
                val compactName = name.replace(" ", "")
                val allowedDistance = when {
                    compactName.length < 4 -> 0
                    compactName.length < 9 -> 1
                    else -> 2
                }
                if (distance > allowedDistance) return@mapNotNull null
                FuzzyMatch(distance.toFloat() / compactName.length, compactName.length)
            }
            .minByOrNull { it.errorRatio }
        bestMatch?.let { destination to it }
    }.minWithOrNull(
        compareBy<Pair<Destination, FuzzyMatch>> { it.second.errorRatio }
            .thenByDescending { it.second.nameLength }
    )?.first
}

/**
 * Folds German umlaut spellings, strips accents and punctuation, and normalizes whitespace.
 */
private data class FuzzyMatch(val errorRatio: Float, val nameLength: Int)

private fun String.normalizeForMatch(): String {
    val folded = lowercase()
        .replace("ä", "a")
        .replace("ae", "a")
        .replace("ö", "o")
        .replace("oe", "o")
        .replace("ü", "u")
        .replace("ue", "u")
        .replace("ß", "ss")
    val withoutAccents = Normalizer.normalize(folded, Normalizer.Form.NFD)
        .filterNot { Character.getType(it) == Character.NON_SPACING_MARK.toInt() }
    return withoutAccents.trim()
        .replace(Regex("[^\\p{L}\\p{N} ]"), "")
        .replace(Regex("\\s+"), " ")
}

private fun editDistance(first: String, second: String): Int {
    var previous = IntArray(second.length + 1) { it }
    for (i in first.indices) {
        val current = IntArray(second.length + 1)
        current[0] = i + 1
        for (j in second.indices) {
            val substitutionCost = if (first[i] == second[j]) 0 else 1
            current[j + 1] = minOf(
                current[j] + 1,
                previous[j + 1] + 1,
                previous[j] + substitutionCost
            )
        }
        previous = current
    }
    return previous[second.length]
}