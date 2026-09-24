package com.tjcelaya.calwrite.data.database

import java.math.BigDecimal

/**
 * A measured value attached to an event: the number and the unit it was measured in. The unit is
 * free text (`bpm`, `kg`, `km`) and may be blank for a bare count or score.
 */
data class FieldValue(val number: Double, val unit: String = "")

/**
 * What an event type declares about one of its fields: the unit the value is recorded in, and
 * optionally a value the record-time prompt starts from. A blank unit is a bare count or score.
 */
data class FieldSpec(val unit: String = "", val default: Double? = null) {
    /** The prompt's starting value, when there is one. */
    fun defaultValue(): FieldValue? = default?.let { FieldValue(it, unit) }
}

/**
 * Key/value fields attached to an event, in the spirit of InfluxDB fields: numeric measurements
 * such as `heart_rate=72bpm` or `score=1500` that describe *this* occurrence and can later be
 * charted or aggregated. Labels ([EventLabels]) identify an occurrence; fields measure it.
 *
 * One text form is used everywhere - the Room column, the edit fields, and the calendar
 * description - and it is the [EventLabels] tag-set syntax with each value written as a number
 * followed directly by its unit:
 *
 *     distance=5.2km,heart_rate=72bpm,score=1500
 *
 * A space between the number and the unit is tolerated on input. Everything else (comma between
 * pairs, first `=` splits key from value, `\,` `\=` `\\` escapes, sorted keys on output) is
 * exactly as for labels, so the two forms can share one parser.
 *
 * Kept free of Android dependencies so it can be unit tested on a plain JVM.
 */
object EventFields {

    sealed class ParseError {
        abstract val pair: String

        /** The pair's structure is wrong; see [EventLabels.ParseError] for which way. */
        data class Malformed(val cause: EventLabels.ParseError) : ParseError() {
            override val pair: String get() = cause.pair
        }

        /** A value that does not start with a number, e.g. `heart_rate=high`. */
        data class NotANumber(override val pair: String, val key: String) : ParseError()
    }

    sealed class ParseResult {
        data class Success(val fields: Map<String, FieldValue>) : ParseResult()
        data class Failure(val error: ParseError) : ParseResult()
    }

    /** Parse user- or database-supplied text. Blank input is an empty field set. */
    fun parse(text: String?): ParseResult {
        val pairs = when (val result = EventLabels.parse(text)) {
            is EventLabels.ParseResult.Success -> result.labels
            is EventLabels.ParseResult.Failure -> return ParseResult.Failure(ParseError.Malformed(result.error))
        }
        val fields = sortedMapOf<String, FieldValue>()
        for ((key, raw) in pairs) {
            fields[key] = parseValue(raw)
                ?: return ParseResult.Failure(ParseError.NotANumber("$key=$raw", key))
        }
        return ParseResult.Success(fields)
    }

    /**
     * Lenient parse for stored values: anything malformed yields no fields rather than an error,
     * since there is nobody to show an error to when reading the database.
     */
    fun parseOrEmpty(text: String?): Map<String, FieldValue> =
        (parse(text) as? ParseResult.Success)?.fields ?: emptyMap()

    /** Serialize to the canonical text form: keys sorted, separators escaped. Empty map -> "". */
    fun format(fields: Map<String, FieldValue>): String =
        EventLabels.format(fields.mapValues { (_, value) -> formatValue(value) })

    /**
     * `72bpm`, `5.2km`, `1500`. Whole numbers are written without a decimal point, other numbers
     * as the shortest exact decimal.
     */
    fun formatValue(value: FieldValue): String = formatNumber(value.number) + value.unit.trim()

    fun formatNumber(number: Double): String {
        if (number.isNaN() || number.isInfinite()) return number.toString()
        val decimal = BigDecimal(number.toString()).stripTrailingZeros()
        return if (decimal.scale() <= 0) decimal.toBigInteger().toString() else decimal.toPlainString()
    }

    /**
     * `"72bpm"` or `"72 bpm"` -> `FieldValue(72.0, "bpm")`; `"1500"` -> `FieldValue(1500.0)`.
     * Null when the text does not begin with a number.
     */
    fun parseValue(text: String): FieldValue? {
        val trimmed = text.trim()
        val match = NUMBER_PREFIX.find(trimmed) ?: return null
        val number = match.value.toDoubleOrNull() ?: return null
        val unit = trimmed.substring(match.range.last + 1).trim()
        return FieldValue(number, unit)
    }

    // === Field specs (what a type declares) ===

    /**
     * Specs use the same pair syntax with the value being an optional number followed by the
     * unit: `heart_rate=bpm` (unit only), `weight=70kg` (default and unit), `score=` (a bare
     * number with no default). Anything that parses as a label set parses as specs.
     */
    fun parseSpecs(text: String?): Map<String, FieldSpec> =
        EventLabels.parseOrEmpty(text).mapValues { (_, raw) -> parseSpec(raw) }

    fun formatSpecs(specs: Map<String, FieldSpec>): String =
        EventLabels.format(specs.mapValues { (_, spec) -> formatSpec(spec) })

    fun parseSpec(text: String): FieldSpec {
        val value = parseValue(text)
        return if (value == null) FieldSpec(unit = text.trim()) else FieldSpec(value.unit, value.number)
    }

    fun formatSpec(spec: FieldSpec): String =
        spec.defaultValue()?.let(::formatValue) ?: spec.unit.trim()

    private val NUMBER_PREFIX = Regex("""^[+-]?(\d+\.?\d*|\.\d+)([eE][+-]?\d+)?""")
}
