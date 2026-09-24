package com.tjcelaya.calwrite.data

import com.tjcelaya.calwrite.data.database.EventFields
import com.tjcelaya.calwrite.data.database.EventLabels
import com.tjcelaya.calwrite.data.database.FieldValue

/**
 * Builds the description written into a calendar event.
 *
 * Labels and fields each go on their own line in [EventLabels] / [EventFields] syntax, so anything
 * reading the calendar back (a script, an export into InfluxDB) can pick them out with one prefix
 * match and one parse each.
 */
object CalendarDescription {

    const val LABELS_PREFIX = "Labels: "
    const val FIELDS_PREFIX = "Fields: "

    fun build(
        notes: String?,
        typeDescription: String?,
        labels: Map<String, String>,
        fields: Map<String, FieldValue> = emptyMap()
    ): String? =
        buildList {
            if (!notes.isNullOrBlank()) add("Notes: $notes")
            if (labels.isNotEmpty()) add(LABELS_PREFIX + EventLabels.format(labels))
            if (fields.isNotEmpty()) add(FIELDS_PREFIX + EventFields.format(fields))
            if (!typeDescription.isNullOrBlank()) add("Event Type: $typeDescription")
        }.joinToString("\n\n").takeIf { it.isNotBlank() }
}
