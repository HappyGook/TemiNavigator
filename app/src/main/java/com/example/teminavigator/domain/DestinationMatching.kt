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


    // query contains a known name as whole word(s)
    val containingMatch = mapNotNull { dest ->
        val matchedLength = dest.allNames()
            .map { it.normalizeForMatch() }
            .filter { it.isNotEmpty() && query.containsWholePhrase(it) }
            .maxOfOrNull { it.length }
        matchedLength?.let { dest to it }
    }.maxByOrNull { it.second }?.first

    if (containingMatch != null) return containingMatch

    val queryWords = query.split(' ')
    val supportedFuzzyMatch = mapNotNull { destination ->
        val bestMatch = destination.allNames()
            .asSequence()
            .map(String::normalizeForMatch)
            .filter(String::isNotEmpty)
            .map { it.split(' ') }
            .filter { it.size > 1 }
            .mapNotNull { nameWords ->
                val usedQueryWords = mutableSetOf<Int>()
                val exactMatches = nameWords.count { word ->
                    val exactIndex = queryWords.indices.firstOrNull { index ->
                        index !in usedQueryWords && queryWords[index] == word
                    } ?: return@count false
                    usedQueryWords += exactIndex
                    true
                }
                if (exactMatches == 0) return@mapNotNull null

                val fuzzyMatches = nameWords.filterNot { word ->
                    word in usedQueryWords.map(queryWords::get)
                }.all { word ->
                    val fuzzyIndex = queryWords.indices.firstOrNull { index ->
                        index !in usedQueryWords && queryWords[index].hasNearSubstring(word)
                    } ?: return@mapNotNull null
                    usedQueryWords += fuzzyIndex
                    true
                }
                if (!fuzzyMatches) return@mapNotNull null
                SupportedFuzzyMatch(exactMatches, nameWords.sumOf(String::length))
            }
            .maxWithOrNull(
                compareBy<SupportedFuzzyMatch> { it.exactWordCount }
                    .thenBy { it.nameLength }
            )
        bestMatch?.let { destination to it }
    }.maxWithOrNull(
        compareBy<Pair<Destination, SupportedFuzzyMatch>> { it.second.exactWordCount }
            .thenBy { it.second.nameLength }
    )?.first

    if (supportedFuzzyMatch != null) return supportedFuzzyMatch

    return mapNotNull { destination ->
        val bestMatch = (destination.aliases + destination.displayName + destination.id)
            .asSequence()
            .map(String::normalizeForMatch)
            .filter(String::isNotEmpty)
            .mapNotNull { name ->
                val nameWords = name.split(' ')
                if (nameWords.any { word ->
                        word.any(Char::isDigit) && word !in queryWords
                    }
                ) return@mapNotNull null
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

private data class SupportedFuzzyMatch(val exactWordCount: Int, val nameLength: Int)

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
    return withoutAccents
        .replace(Regex("[^\\p{L}\\p{N} ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
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

private fun String.containsWholePhrase(phrase: String): Boolean =
    " $this ".contains(" $phrase ")

private fun String.hasNearSubstring(target: String): Boolean {
    if (target.length < 4 || !target.all(Char::isLetter)) return false
    if (length < target.length - 1 || length > target.length + 1) return false
    val minLength = (target.length - 1).coerceAtLeast(1)
    val maxLength = minOf(target.length + 1, length)
    return (minLength..maxLength).any { windowLength ->
        windowed(windowLength).any { candidate ->
            val distance = editDistance(candidate, target)
            distance <= 1 || candidate.hasSingleAdjacentTransposition(target)
        }
    }
}

private fun String.hasSingleAdjacentTransposition(other: String): Boolean {
    if (length != other.length) return false
    val differingIndices = indices.filter { this[it] != other[it] }
    return differingIndices.size == 2 &&
        differingIndices[1] == differingIndices[0] + 1 &&
        this[differingIndices[0]] == other[differingIndices[1]] &&
        this[differingIndices[1]] == other[differingIndices[0]]
}

private fun Destination.allNames(): List<String> = aliases + displayName + id