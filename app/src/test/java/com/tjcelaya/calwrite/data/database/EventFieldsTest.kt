package com.tjcelaya.calwrite.data.database

import com.tjcelaya.calwrite.data.database.EventFields.ParseError
import com.tjcelaya.calwrite.data.database.EventFields.ParseResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EventFieldsTest {

    private fun parsed(text: String?): Map<String, FieldValue> =
        (EventFields.parse(text) as ParseResult.Success).fields

    private fun error(text: String): ParseError =
        (EventFields.parse(text) as ParseResult.Failure).error

    @Test
    fun blankInputIsNoFields() {
        assertEquals(emptyMap<String, FieldValue>(), parsed(null))
        assertEquals(emptyMap<String, FieldValue>(), parsed("  "))
    }

    @Test
    fun parsesNumberFollowedByUnit() {
        assertEquals(
            mapOf(
                "distance" to FieldValue(5.2, "km"),
                "heart_rate" to FieldValue(72.0, "bpm"),
                "score" to FieldValue(1500.0)
            ),
            parsed("heart_rate=72bpm,score=1500,distance=5.2km")
        )
    }

    @Test
    fun toleratesSpaceBetweenNumberAndUnitAndSignedOrDecimalNumbers() {
        assertEquals(FieldValue(72.0, "bpm"), parseValue("72 bpm"))
        assertEquals(FieldValue(-3.5, "°C"), parseValue("-3.5 °C"))
        assertEquals(FieldValue(0.5, "kg"), parseValue(".5kg"))
        assertEquals(FieldValue(95.0, "%"), parseValue("95%"))
    }

    @Test
    fun aValueWithoutANumberIsAnError() {
        assertEquals(ParseError.NotANumber("heart_rate=high", "heart_rate"), error("heart_rate=high"))
        assertEquals(ParseError.NotANumber("score=", "score"), error("score="))
        assertNull(parseValue("bpm72"))
    }

    @Test
    fun structuralErrorsComeFromTheLabelParser() {
        assertEquals(
            ParseError.Malformed(EventLabels.ParseError.MissingEquals("72")),
            error("heart_rate=72bpm,72")
        )
        assertEquals(
            ParseError.Malformed(EventLabels.ParseError.DuplicateKey("score=2", "score")),
            error("score=1,score=2")
        )
    }

    @Test
    fun formatsWholeNumbersWithoutADecimalPoint() {
        assertEquals("72bpm", EventFields.formatValue(FieldValue(72.0, "bpm")))
        assertEquals("1500", EventFields.formatValue(FieldValue(1500.0)))
        assertEquals("5.2km", EventFields.formatValue(FieldValue(5.2, "km")))
        assertEquals("0.1", EventFields.formatValue(FieldValue(0.1)))
        assertEquals("1000000", EventFields.formatValue(FieldValue(1_000_000.0)))
    }

    @Test
    fun formatsSortedAndRoundTrips() {
        val fields = mapOf(
            "score" to FieldValue(1500.0),
            "heart_rate" to FieldValue(72.0, "bpm"),
            "odd,key" to FieldValue(1.25, "m/s")
        )
        assertEquals("heart_rate=72bpm,odd\\,key=1.25m/s,score=1500", EventFields.format(fields))
        assertEquals(fields, parsed(EventFields.format(fields)))
        assertEquals("", EventFields.format(emptyMap()))
    }

    @Test
    fun lenientParseDropsMalformedInput() {
        assertEquals(emptyMap<String, FieldValue>(), EventFields.parseOrEmpty("heart_rate=high"))
        assertEquals(mapOf("score" to FieldValue(9.0)), EventFields.parseOrEmpty("score=9"))
    }

    private fun parseValue(text: String): FieldValue? = EventFields.parseValue(text)

    @Test
    fun specsAreUnitWithOptionalDefault() {
        val specs = mapOf(
            "heart_rate" to FieldSpec("bpm"),
            "weight" to FieldSpec("kg", 70.0),
            "score" to FieldSpec()
        )
        assertEquals("heart_rate=bpm,score=,weight=70kg", EventFields.formatSpecs(specs))
        assertEquals(specs, EventFields.parseSpecs("heart_rate=bpm,score=,weight=70kg"))
        assertEquals(FieldSpec("bpm"), EventFields.parseSpec(" bpm "))
        assertEquals(FieldSpec("", 5.0), EventFields.parseSpec("5"))
        assertEquals(FieldValue(70.0, "kg"), FieldSpec("kg", 70.0).defaultValue())
        assertNull(FieldSpec("kg").defaultValue())
    }
}
