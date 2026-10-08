package com.example.teminavigator.domain

/**
 * match method for the List<Destination> matching based on aliases and display names
 * for speech recognition
  */

fun List<Destination>.match(text: String): Destination? {
    val query = text.normalize()
    // exact match on id/displayname/alias
    firstOrNull { dest -> (dest.aliases + dest.displayName + dest.id).any { it.normalize() == query } }
        ?.let { return it }

    // query contains a known name
    return filter {
        dest -> (dest.aliases + dest.displayName + dest.id).any {
            it.normalize().let {
                name -> name.isNotEmpty() && query.contains(name)
            }
        }
    }.maxByOrNull { dest -> (dest.aliases + dest.displayName).maxOf { it.length } } // prefer the longest match
}

/**
 * helper normalization func
 * converts to lowercase, removes whitespace from beginning and end.
 * Regex("[^\\p{L}\\p{N} ]"), "" removes every character that is not a Unicode letter/number or a space
 * Regex("\\s+"), " " replaces each longer whitespace with one normal space.
*/
private fun String.normalize() =
    lowercase().trim().replace(Regex("[^\\p{L}\\p{N} ]"), "").replace(Regex("\\s+"), " ")