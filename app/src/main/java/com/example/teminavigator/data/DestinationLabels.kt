package com.example.teminavigator.data

import kotlinx.serialization.Serializable

/**
* Format of destination-infos to save in App's memory
 */
@Serializable
data class DestinationLabels(
    val displayName: String,
    val aliases: List<String> = emptyList()
)