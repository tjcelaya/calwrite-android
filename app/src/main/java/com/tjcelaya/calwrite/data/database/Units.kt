package com.tjcelaya.calwrite.data.database

/**
 * Units offered by the type editor's picker. A field's unit is free text in storage; this list
 * only decides what the dropdown shows before the user adds their own.
 */
object Units {
    val BUILT_IN: List<String> = listOf(
        "bpm", "kg", "lb", "g", "km", "mi", "m", "min", "h", "s", "kcal", "ml", "°C", "°F", "%", "mg"
    )

    /** Built-ins first, then everything else seen, deduplicated and in a stable order. */
    fun known(vararg extra: Iterable<String>): List<String> {
        val seen = LinkedHashSet(BUILT_IN)
        extra.forEach { units -> units.map { it.trim() }.filter { it.isNotEmpty() }.sorted().forEach(seen::add) }
        return seen.toList()
    }
}
