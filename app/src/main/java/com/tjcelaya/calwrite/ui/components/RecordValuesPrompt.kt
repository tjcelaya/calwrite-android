package com.tjcelaya.calwrite.ui.components

import android.content.Context
import android.text.InputType
import android.view.View
import android.widget.LinearLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.tjcelaya.calwrite.R
import com.tjcelaya.calwrite.data.database.EventFields
import com.tjcelaya.calwrite.data.database.EventType
import com.tjcelaya.calwrite.data.database.FieldValue

/**
 * The inputs shown when recording an event whose type declares fields: one numeric input per
 * declared field, hinted with its key and unit, plus a labels line when the type seeds labels
 * (so a per-event value such as `game=` can be filled in at the same time).
 *
 * Built in code rather than XML because the number of inputs comes from the type. Hosts embed
 * [view] in whatever dialog fits the flow and call [read] on confirm.
 */
class RecordValuesPrompt(private val context: Context, private val eventType: EventType) {

    private val inputs = LinkedHashMap<String, Pair<TextInputLayout, TextInputEditText>>()
    private var labelsLayout: TextInputLayout? = null
    private var labelsInput: TextInputEditText? = null

    val view: View = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        val dp = context.resources.displayMetrics.density
        setPadding((24 * dp).toInt(), (8 * dp).toInt(), (24 * dp).toInt(), 0)

        for ((key, spec) in eventType.fieldSpecs) {
            val unit = spec.unit
            val layout = TextInputLayout(context).apply {
                hint = if (unit.isBlank()) key else "$key ($unit)"
                suffixText = unit.takeIf { it.isNotBlank() }
            }
            val input = TextInputEditText(layout.context).apply {
                inputType = InputType.TYPE_CLASS_NUMBER or
                    InputType.TYPE_NUMBER_FLAG_DECIMAL or
                    InputType.TYPE_NUMBER_FLAG_SIGNED
                maxLines = 1
                spec.default?.let { setText(EventFields.formatNumber(it)) }
            }
            layout.addView(input)
            addView(layout, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (8 * dp).toInt() })
            inputs[key] = layout to input
        }

        if (eventType.defaultLabels.isNotEmpty()) {
            val layout = TextInputLayout(context).apply {
                hint = context.getString(R.string.labels_hint)
                helperText = context.getString(R.string.labels_helper)
            }
            val input = TextInputEditText(layout.context).apply {
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                maxLines = 3
            }
            layout.addView(input)
            addView(layout, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (16 * dp).toInt() })
            LabelsField.show(input, eventType.defaultLabels)
            labelsLayout = layout
            labelsInput = input
        }
    }

    /** What the user entered: fields left blank are omitted, and the labels line as typed. */
    data class Values(val fields: Map<String, FieldValue>, val labels: Map<String, String>)

    /**
     * Validate and collect the inputs. Returns null after marking the offending input when a
     * value is not a number or the labels line is malformed, so the host keeps the dialog open.
     */
    fun read(): Values? {
        val fields = LinkedHashMap<String, FieldValue>()
        var valid = true
        for ((key, pair) in inputs) {
            val (layout, input) = pair
            val text = input.text?.toString().orEmpty().trim()
            if (text.isEmpty()) {
                layout.error = null
                continue
            }
            val number = text.toDoubleOrNull()
            if (number == null) {
                layout.error = context.getString(R.string.fields_error_not_a_number, key)
                valid = false
            } else {
                layout.error = null
                fields[key] = FieldValue(number, eventType.fieldSpecs[key]?.unit.orEmpty())
            }
        }
        val labels = when {
            labelsLayout == null || labelsInput == null -> eventType.defaultLabels
            else -> LabelsField.read(labelsLayout!!, labelsInput!!) ?: run { valid = false; emptyMap() }
        }
        return if (valid) Values(fields, labels) else null
    }

    companion object {
        /** Whether recording this type needs the prompt at all. */
        fun isNeeded(eventType: EventType): Boolean = eventType.fieldSpecs.isNotEmpty()

        /** One-line summary of entered values for a confirmation message. */
        fun summary(fields: Map<String, FieldValue>): String =
            fields.entries.joinToString(", ") { (key, value) -> "$key ${EventFields.formatValue(value)}" }
    }
}
