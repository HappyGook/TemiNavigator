package com.example.teminavigator.domain

import java.text.Normalizer

/**
 * Finds the destination that best matches a free-text query, typically
 * recognized speech.
 *
 * Names considered per destination are its [Destination.id],
 * [Destination.displayName] and [Destination.aliases]. Both query and names are
 * normalized first (see [normalizeForMatch]). Matching then goes through
 * several stages, from strict to tolerant, and returns the result of the first
 * stage that finds something:
 * 1. **Exact match:** the whole query equals one of the names.
 * 2. **Contained name:** the query contains a name as whole word(s), e.g.
 *    "take me to room 204 please". If several names are contained, the longest
 *    wins.
 * 3. **Partly exact, partly fuzzy:** for multi-word names, at least one word
 *    must match a query word exactly, and every remaining word must nearly
 *    match another query word (one typo or one swapped neighboring letter
 *    pair). Preferred: more exact words, then longer names.
 * 4. **Edit distance:** the name, spaces removed, is compared to windows of
 *    consecutive query words using [editDistance]. The allowed distance depends
 *    on the name length: 0 below 4 characters, 1 below 9, otherwise 2. Names
 *    containing numbers only match if the number appears exactly in the query,
 *    so "room 204" is not confused with "room 205". Preferred: lowest error
 *    ratio, then longer names.
 *
 * @receiver The destinations to search in.
 * @param text The raw query, e.g. the recognized speech.
 * @return The best matching destination, or null if the query is empty or
 * nothing matches (an unknown destination).
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
 * Result of the edit-distance stage for one destination.
 *
 * @property errorRatio Edit distance divided by the name length. Lower is better.
 * @property nameLength Length of the matched name without spaces. Used as a tie-breaker.
 */
private data class FuzzyMatch(val errorRatio: Float, val nameLength: Int)

/**
 * Result of the partly-exact-partly-fuzzy stage for one destination.
 *
 * @property exactWordCount Number of name words that matched a query word exactly.
 * @property nameLength Total character count of the name's words. Used as a tie-breaker.
 */
private data class SupportedFuzzyMatch(val exactWordCount: Int, val nameLength: Int)

/**
 * Normalizes text so spoken and written variants compare equal.
 *
 * Lowercases, folds German umlaut spellings ("ä" and "ae" both become "a", and
 * likewise for ö/oe and ü/ue; "ß" becomes "ss"), strips accents, replaces
 * punctuation with spaces and collapses repeated whitespace. Apply it to both
 * the query and the names, so both sides are folded the same way.
 *
 * @return The normalized string, possibly empty.
 */
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

/**
 * Computes the Levenshtein distance between two strings: the minimum number
 * of single-character insertions, deletions and substitutions needed to turn
 * one into the other.
 *
 * @return The edit distance. 0 means the strings are identical.
 */
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

/**
 * Checks whether [phrase] appears in this string as complete word(s), so
 * "room" is found in "go to room 5" but not in "bathroom".
 *
 * Both strings are expected to be normalized.
 */
private fun String.containsWholePhrase(phrase: String): Boolean =
    " $this ".contains(" $phrase ")

/**
 * Checks whether this query word is a close misspelling of [target].
 *
 * Only applies to alphabetic targets of at least 4 letters, since short words
 * would produce too many false matches. This word must be within one
 * character of the target's length. It is accepted if a section of it differs
 * from [target] by at most one edit, or by one swapped neighboring pair of
 * letters.
 */
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

/**
 * Checks whether this string equals [other] except for exactly one swapped
 * a pair of neighboring characters, e.g. "recpetion" and "reception".
 * Counts as one error, where plain edit distance would count two.
 */
private fun String.hasSingleAdjacentTransposition(other: String): Boolean {
    if (length != other.length) return false
    val differingIndices = indices.filter { this[it] != other[it] }
    return differingIndices.size == 2 &&
        differingIndices[1] == differingIndices[0] + 1 &&
        this[differingIndices[0]] == other[differingIndices[1]] &&
        this[differingIndices[1]] == other[differingIndices[0]]
}

/** All names this destination can be called by: aliases, display name and id. */
private fun Destination.allNames(): List<String> = aliases + displayName + id