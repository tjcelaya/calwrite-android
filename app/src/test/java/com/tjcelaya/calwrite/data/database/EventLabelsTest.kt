package com.tjcelaya.calwrite.data.database

import com.tjcelaya.calwrite.data.database.EventLabels.ParseError
import com.tjcelaya.calwrite.data.database.EventLabels.ParseResult
import org.junit.Assert.assertEquals
import org.junit.Test

class EventLabelsTest {

    private fun parsed(text: String?): Map<String, String> =
        (EventLabels.parse(text) as ParseResult.Success).labels

    private fun error(text: String): ParseError =
        (EventLabels.parse(text) as ParseResult.Failure).error

    @Test
    fun blankInputIsNoLabels() {
        assertEquals(emptyMap<String, String>(), parsed(null))
        assertEquals(emptyMap<String, String>(), parsed(""))
        assertEquals(emptyMap<String, String>(), parsed("   "))
    }

    @Test
    fun parsesLineProtocolTagSet() {
        assertEquals(
            mapOf("location" to "gym", "intensity" to "high"),
            parsed("location=gym,intensity=high")
        )
    }

    @Test
    fun trimsWhitespaceAndSkipsEmptyPairs() {
        assertEquals(
            mapOf("location" to "home gym", "with" to "Sam"),
            parsed(" location = home gym , , with=Sam, ")
        )
    }

    @Test
    fun keepsEmptyValues() {
        assertEquals(mapOf("mood" to ""), parsed("mood="))
    }

    @Test
    fun onlyTheFirstEqualsSeparatesKeyFromValue() {
        assertEquals(mapOf("formula" to "a=b"), parsed("formula=a=b"))
    }

    @Test
    fun honoursEscapes() {
        assertEquals(
            mapOf("a,b" to "c=d", "path" to "C:\\x"),
            parsed("a\\,b=c\\=d,path=C:\\\\x")
        )
    }

    @Test
    fun reportsMalformedPairs() {
        assertEquals(ParseError.MissingEquals("gym"), error("location=home,gym"))
        assertEquals(ParseError.EmptyKey("=gym"), error("=gym"))
        assertEquals(ParseError.DuplicateKey("a=2", "a"), error("a=1, a=2"))
    }

    @Test
    fun lenientParseDropsMalformedInput() {
        assertEquals(emptyMap<String, String>(), EventLabels.parseOrEmpty("not a label"))
        assertEquals(mapOf("a" to "1"), EventLabels.parseOrEmpty("a=1"))
    }

    @Test
    fun formatsSortedAndEscaped() {
        assertEquals("", EventLabels.format(emptyMap()))
        assertEquals(
            "a\\,b=c\\=d,location=gym,z=1",
            EventLabels.format(mapOf("z" to "1", "location" to "gym", "a,b" to "c=d"))
        )
    }

    @Test
    fun formatThenParseRoundTrips() {
        val labels = mapOf(
            "location" to "home gym",
            "weird,key" to "x=y\\z",
            "empty" to ""
        )
        assertEquals(labels, parsed(EventLabels.format(labels)))
    }

    @Test
    fun mergeLetsOverridesWin() {
        assertEquals(
            mapOf("kind" to "cardio", "location" to "park"),
            EventLabels.merge(
                defaults = mapOf("kind" to "cardio", "location" to "gym"),
                overrides = mapOf("location" to "park")
            )
        )
    }
}
