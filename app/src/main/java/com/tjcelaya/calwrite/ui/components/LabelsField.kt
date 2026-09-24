package com.tjcelaya.calwrite.ui.components

import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.tjcelaya.calwrite.R
import com.tjcelaya.calwrite.data.database.EventLabels

/**
 * Binds a text field that edits a label set in [EventLabels] syntax (`key=value,key=value`).
 * Shared by the ledger's Adjust sheet (per-event labels) and the event type editor (defaults).
 */
object LabelsField {

    fun show(input: TextInputEditText, labels: Map<String, String>) {
        input.setText(EventLabels.format(labels))
    }

    /**
     * Parse the field. On a malformed entry, shows why on [layout] and returns null so the caller
     * can refuse to save; otherwise clears any previous error and returns the labels.
     */
    fun read(layout: TextInputLayout, input: TextInputEditText): Map<String, String>? {
        val context = layout.context
        return when (val result = EventLabels.parse(input.text?.toString())) {
            is EventLabels.ParseResult.Success -> {
                layout.error = null
                result.labels
            }
            is EventLabels.ParseResult.Failure -> {
                layout.error = when (val error = result.error) {
                    is EventLabels.ParseError.MissingEquals ->
                        context.getString(R.string.labels_error_missing_equals, error.pair)
                    is EventLabels.ParseError.EmptyKey ->
                        context.getString(R.string.labels_error_empty_key, error.pair)
                    is EventLabels.ParseError.DuplicateKey ->
                        context.getString(R.string.labels_error_duplicate_key, error.key)
                }
                null
            }
        }
    }

    /** Read-only rendering for list rows: unescaped, one `key=value` per label. */
    fun displayText(labels: Map<String, String>): String =
        labels.toSortedMap().entries.joinToString("  ·  ") { (key, value) -> "$key=$value" }
}
