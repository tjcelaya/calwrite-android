package com.tjcelaya.calwrite.ui.events

import android.content.Context
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.tjcelaya.calwrite.R
import com.tjcelaya.calwrite.data.database.EventFields
import com.tjcelaya.calwrite.data.database.FieldSpec

/**
 * The type editor's row-per-entry editors for labels (key, default value) and fields (key,
 * default value, unit). Rows live in a plain [LinearLayout] so the whole form stays one scroll.
 *
 * Reading validates every row and marks the offending input; a null result means "don't save".
 * Rows with a blank key and nothing else typed are ignored rather than rejected, so an extra
 * tapped "Add" costs nothing.
 */
class AttributeRowsEditor(
    private val container: LinearLayout,
    private val kind: Kind,
    private val unitSource: UnitSource? = null
) {
    enum class Kind { LABELS, FIELDS }

    /** Where the unit dropdown gets its choices, and where a newly added unit is remembered. */
    interface UnitSource {
        fun knownUnits(): List<String>
        fun addUnit(unit: String)
    }

    private val context: Context get() = container.context
    private val inflater = LayoutInflater.from(container.context)

    private class Row(val view: View) {
        val keyLayout: TextInputLayout = view.findViewById(R.id.keyLayout)
        val keyInput: TextInputEditText = view.findViewById(R.id.keyInput)
        val valueLayout: TextInputLayout = view.findViewById(R.id.valueLayout)
        val valueInput: TextInputEditText = view.findViewById(R.id.valueInput)
        val unitInput: MaterialAutoCompleteTextView? = view.findViewById(R.id.unitInput)
        var unit: String = ""
    }

    private val rows = mutableListOf<Row>()

    fun showLabels(labels: Map<String, String>) {
        clear()
        labels.forEach { (key, value) -> addRow(key, value, "") }
    }

    fun showFields(specs: Map<String, FieldSpec>) {
        clear()
        specs.forEach { (key, spec) ->
            addRow(key, spec.default?.let(EventFields::formatNumber).orEmpty(), spec.unit)
        }
    }

    /** Append an empty row and focus its key, for the section's Add button. */
    fun addEmptyRow() {
        addRow("", "", "").keyInput.requestFocus()
    }

    fun readLabels(): Map<String, String>? {
        val keys = readKeys() ?: return null
        return keys.mapValues { (_, row) -> row.valueInput.text?.toString().orEmpty().trim() }
    }

    fun readFields(): Map<String, FieldSpec>? {
        val keys = readKeys() ?: return null
        var valid = true
        val specs = keys.mapValues { (key, row) ->
            val text = row.valueInput.text?.toString().orEmpty().trim()
            val default = if (text.isEmpty()) null else text.replace(',', '.').toDoubleOrNull()
            if (text.isNotEmpty() && default == null) {
                row.valueLayout.error = context.getString(R.string.fields_error_not_a_number, key)
                valid = false
            } else {
                row.valueLayout.error = null
            }
            FieldSpec(row.unit, default)
        }
        return if (valid) specs else null
    }

    // === internals ===

    /** Rows keyed by their trimmed key, or null after marking a missing or duplicate key. */
    private fun readKeys(): Map<String, Row>? {
        val result = LinkedHashMap<String, Row>()
        var valid = true
        for (row in rows) {
            val key = row.keyInput.text?.toString().orEmpty().trim()
            val value = row.valueInput.text?.toString().orEmpty().trim()
            when {
                key.isEmpty() && value.isEmpty() && row.unit.isEmpty() -> row.keyLayout.error = null
                key.isEmpty() -> {
                    row.keyLayout.error = context.getString(R.string.attr_error_key_required)
                    valid = false
                }
                key in result -> {
                    row.keyLayout.error = context.getString(R.string.attr_error_key_duplicate)
                    valid = false
                }
                else -> {
                    row.keyLayout.error = null
                    result[key] = row
                }
            }
        }
        return if (valid) result else null
    }

    private fun clear() {
        container.removeAllViews()
        rows.clear()
    }

    private fun addRow(key: String, value: String, unit: String): Row {
        val layoutRes = if (kind == Kind.LABELS) R.layout.item_type_label_row else R.layout.item_type_field_row
        val row = Row(inflater.inflate(layoutRes, container, false))
        row.keyInput.setText(key)
        row.valueInput.setText(value)
        row.unit = unit
        row.unitInput?.let { bindUnitPicker(row, it) }
        row.view.findViewById<View>(R.id.removeButton).setOnClickListener {
            container.removeView(row.view)
            rows.remove(row)
        }
        container.addView(row.view)
        rows.add(row)
        return row
    }

    /**
     * The dropdown lists "None", every known unit, and "Add a new unit…" last. Picking the last
     * opens a small dialog; the new unit is remembered through [UnitSource] and every row's
     * dropdown is refreshed so it is offered from then on.
     */
    private fun bindUnitPicker(row: Row, input: MaterialAutoCompleteTextView) {
        refreshUnitChoices(input)
        input.setText(displayUnit(row.unit), false)
        input.setOnItemClickListener { _, _, position, _ ->
            val choices = currentChoices()
            when (position) {
                0 -> row.unit = ""
                choices.lastIndex -> {
                    input.setText(displayUnit(row.unit), false)
                    promptForNewUnit { added ->
                        row.unit = added
                        rows.forEach { r -> r.unitInput?.let(::refreshUnitChoices) }
                        input.setText(added, false)
                    }
                }
                else -> row.unit = choices[position]
            }
        }
    }

    private fun currentChoices(): List<String> =
        listOf(context.getString(R.string.attr_unit_none)) +
            unitSource?.knownUnits().orEmpty() +
            context.getString(R.string.attr_unit_add)

    private fun refreshUnitChoices(input: MaterialAutoCompleteTextView) {
        input.setSimpleItems(currentChoices().toTypedArray())
    }

    private fun displayUnit(unit: String): String =
        unit.ifEmpty { context.getString(R.string.attr_unit_none) }

    private fun promptForNewUnit(onAdded: (String) -> Unit) {
        val input = EditText(context).apply {
            hint = context.getString(R.string.unit_dialog_hint)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            maxLines = 1
        }
        val dp = context.resources.displayMetrics.density
        val frame = FrameLayout(context).apply {
            setPadding((24 * dp).toInt(), (8 * dp).toInt(), (24 * dp).toInt(), 0)
            addView(input)
        }
        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.unit_dialog_title)
            .setView(frame)
            .setPositiveButton(R.string.attr_unit_add_confirm, null)
            .setNegativeButton(R.string.cancel, null)
            .show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val unit = input.text?.toString().orEmpty().trim()
            if (unit.isEmpty()) {
                input.error = context.getString(R.string.unit_dialog_required)
                return@setOnClickListener
            }
            unitSource?.addUnit(unit)
            dialog.dismiss()
            onAdded(unit)
        }
        input.requestFocus()
    }
}
