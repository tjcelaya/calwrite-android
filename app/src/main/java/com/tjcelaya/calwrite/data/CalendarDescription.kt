package com.tjcelaya.calwrite.data

import com.tjcelaya.calwrite.data.database.EventLabels

/**
 * Builds the description written into a calendar event.
 *
 * Labels go on their own `Labels:` line in [EventLabels] syntax, so anything reading the calendar
 * back (a script, an export into InfluxDB) can pick them out with one prefix match and one parse.
 */
object CalendarDescription {

    const val LABELS_PREFIX = "Labels: "

    fun build(notes: String?, typeDescription: String?, labels: Map<String, String>): String? =
        buildList {
            if (!notes.isNullOrBlank()) add("Notes: $notes")
            if (labels.isNotEmpty()) add(LABELS_PREFIX + EventLabels.format(labels))
            if (!typeDescription.isNullOrBlank()) add("Event Type: $typeDescription")
        }.joinToString("\n\n").takeIf { it.isNotBlank() }
}
