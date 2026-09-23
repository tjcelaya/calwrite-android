package com.tjcelaya.calwrite.data.database

/**
 * Key/value labels attached to an event, in the spirit of InfluxDB tags: free-form string pairs
 * such as `location=gym` or `intensity=high` that describe *this* occurrence and can later be
 * grouped or filtered on.
 *
 * One text form is used everywhere - the Room column, the edit fields, and the calendar
 * description - and it is InfluxDB line protocol's tag-set syntax:
 *
 *     intensity=high,location=home gym,with=Sam
 *
 * Pairs are separated by commas, keys from values by the first `=`. A literal comma, equals sign
 * or backslash is written `\,`, `\=` or `\\`. Unlike line protocol, spaces need no escaping and
 * whitespace around each key and value is trimmed, since people type these by hand. Keys are
 * written sorted, which is also line protocol's canonical order, so the same labels always
 * serialize identically.
 *
 * Kept free of Android dependencies so it can be unit tested on a plain JVM.
 */
object EventLabels {

    /** Why a label string could not be parsed; carries the offending pair for the error text. */
    sealed class ParseError {
        abstract val pair: String

        /** A pair with no unescaped `=`, e.g. `gym` instead of `location=gym`. */
        data class MissingEquals(override val pair: String) : ParseError()

        /** A pair whose key is blank, e.g. `=gym`. */
        data class EmptyKey(override val pair: String) : ParseError()

        /** The same key given twice; silently keeping one would drop what the user typed. */
        data class DuplicateKey(override val pair: String, val key: String) : ParseError()
    }

    sealed class ParseResult {
        data class Success(val labels: Map<String, String>) : ParseResult()
        data class Failure(val error: ParseError) : ParseResult()
    }

    /**
     * Parse user- or database-supplied text. Blank input is an empty label set; blank pairs (from
     * a trailing comma, say) are skipped. A pair with an empty value (`mood=`) is kept with an
     * empty value, since the key alone can still be meaningful.
     */
    fun parse(text: String?): ParseResult {
        if (text.isNullOrBlank()) return ParseResult.Success(emptyMap())

        val labels = sortedMapOf<String, String>()
        for (pair in splitUnescaped(text, ',')) {
            if (pair.isBlank()) continue
            val parts = splitUnescaped(pair, '=', limit = 2)
            if (parts.size < 2) return ParseResult.Failure(ParseError.MissingEquals(pair.trim()))
            val key = unescape(parts[0]).trim()
            val value = unescape(parts[1]).trim()
            if (key.isEmpty()) return ParseResult.Failure(ParseError.EmptyKey(pair.trim()))
            if (labels.containsKey(key)) {
                return ParseResult.Failure(ParseError.DuplicateKey(pair.trim(), key))
            }
            labels[key] = value
        }
        return ParseResult.Success(labels)
    }

    /**
     * Lenient parse for stored values: anything malformed yields no labels rather than an error,
     * since there is nobody to show an error to when reading the database.
     */
    fun parseOrEmpty(text: String?): Map<String, String> =
        (parse(text) as? ParseResult.Success)?.labels ?: emptyMap()

    /** Serialize to the canonical text form: keys sorted, separators escaped. Empty map -> "". */
    fun format(labels: Map<String, String>): String =
        labels.toSortedMap().entries.joinToString(",") { (key, value) ->
            "${escape(key.trim())}=${escape(value.trim())}"
        }

    /**
     * [defaults] overlaid with [overrides]: a key present in both takes the override's value.
     * Used to seed a new event from its type's default labels.
     */
    fun merge(defaults: Map<String, String>, overrides: Map<String, String>): Map<String, String> =
        (defaults + overrides).toSortedMap()

    private fun escape(s: String): String = buildString(s.length) {
        for (c in s) {
            if (c == '\\' || c == ',' || c == '=') append('\\')
            append(c)
        }
    }

    private fun unescape(s: String): String = buildString(s.length) {
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                append(s[i + 1])
                i += 2
            } else {
                append(c)
                i++
            }
        }
    }

    /**
     * Split [s] on each [separator] not preceded by an escaping backslash, into at most [limit]
     * pieces. Escapes are left in place for [unescape] to resolve.
     */
    private fun splitUnescaped(s: String, separator: Char, limit: Int = Int.MAX_VALUE): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                current.append(c).append(s[i + 1])
                i += 2
                continue
            }
            if (c == separator && parts.size < limit - 1) {
                parts.add(current.toString())
                current.clear()
            } else {
                current.append(c)
            }
            i++
        }
        parts.add(current.toString())
        return parts
    }
}
