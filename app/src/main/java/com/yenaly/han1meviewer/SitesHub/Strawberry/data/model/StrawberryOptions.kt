package com.yenaly.han1meviewer.SitesHub.Strawberry.data.model

data class StrawberrySelectOption(
    val value: String,
    val label: String,
    val isSelected: Boolean = false,
)

object StrawberryOptions {

    val searchFields: List<StrawberrySelectOption> = listOf(
        StrawberrySelectOption("title", "Title"),
        StrawberrySelectOption("uploader", "Uploader"),
        StrawberrySelectOption("tag", "Tag"),
    )

    val lengths: List<StrawberrySelectOption> = listOf(
        StrawberrySelectOption("any", "Any length"),
        StrawberrySelectOption("xshort", "Under 1 min"),
        StrawberrySelectOption("short", "1-4 min"),
        StrawberrySelectOption("medium", "4-20 min"),
        StrawberrySelectOption("long", "20-60 min"),
        StrawberrySelectOption("xlong", "Over 1 hour"),
    )

    val dates: List<StrawberrySelectOption> = listOf(
        StrawberrySelectOption("any", "Any time"),
        StrawberrySelectOption("day", "Past 24 hours"),
        StrawberrySelectOption("week", "Past week"),
        StrawberrySelectOption("month", "Past month"),
        StrawberrySelectOption("year", "Past year"),
    )

    val sorts: List<StrawberrySelectOption> = listOf(
        StrawberrySelectOption("newest", "Newest"),
        StrawberrySelectOption("oldest", "Oldest"),
        StrawberrySelectOption("most_viewed", "Most viewed"),
        StrawberrySelectOption("least_viewed", "Least viewed"),
        StrawberrySelectOption("longest", "Longest"),
        StrawberrySelectOption("shortest", "Shortest"),
    )

    val qualities: List<String> = listOf("original", "480", "360", "240", "144")

    fun qualityLabel(quality: String): String = when (quality) {
        "original" -> "Original"
        else -> "${quality}p"
    }
}
