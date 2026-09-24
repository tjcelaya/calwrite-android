package com.tjcelaya.calwrite.ui.components

import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.tjcelaya.calwrite.R
import com.tjcelaya.calwrite.data.database.EventFields
import com.tjcelaya.calwrite.data.database.EventLabels
import com.tjcelaya.calwrite.data.database.FieldValue

/**
 * Binds a text field that edits a field set in [EventFields] syntax (`key=72bpm,key=1500`).
 * Used by the ledger's Adjust sheet; the record-time prompt uses one input per declared field
 * instead (see [RecordValuesPrompt]).
 */
object FieldsField {

    fun show(input: TextInputEditText, fields: Map<String, FieldValue>) {
        input.setText(EventFields.format(fields))
    }

    /**
     * Parse the field. On a malformed entry, shows why on [layout] and returns null so the caller
     * can refuse to save; otherwise clears any previous error and returns the fields.
     */
    fun read(layout: TextInputLayout, input: TextInputEditText): Map<String, FieldValue>? {
        val context = layout.context
        return when (val result = EventFields.parse(input.text?.toString())) {
            is EventFields.ParseResult.Success -> {
                layout.error = null
                result.fields
            }
            is EventFields.ParseResult.Failure -> {
                layout.error = when (val error = result.error) {
                    is EventFields.ParseError.NotANumber ->
                        context.getString(R.string.fields_error_not_a_number, error.key)
                    is EventFields.ParseError.Malformed -> when (val cause = error.cause) {
                        is EventLabels.ParseError.MissingEquals ->
                            context.getString(R.string.labels_error_missing_equals, cause.pair)
                        is EventLabels.ParseError.EmptyKey ->
                            context.getString(R.string.labels_error_empty_key, cause.pair)
                        is EventLabels.ParseError.DuplicateKey ->
                            context.getString(R.string.labels_error_duplicate_key, cause.key)
                    }
                }
                null
            }
        }
    }

    /**
     * Read-only rendering for list rows: fields first, then labels, one `key=value` per entry.
     * Fields lead because a measurement is usually what the row is about.
     */
    fun displayText(fields: Map<String, FieldValue>, labels: Map<String, String>): String {
        val fieldText = fields.toSortedMap().entries.map { (key, value) ->
            "$key=${EventFields.formatValue(value)}"
        }
        val labelText = labels.toSortedMap().entries.map { (key, value) -> "$key=$value" }
        return (fieldText + labelText).joinToString("  ·  ")
    }
}
