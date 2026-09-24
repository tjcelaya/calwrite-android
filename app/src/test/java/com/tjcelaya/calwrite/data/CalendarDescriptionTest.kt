package com.tjcelaya.calwrite.data

import com.tjcelaya.calwrite.data.database.FieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalendarDescriptionTest {

    @Test
    fun nothingToSayIsNull() {
        assertNull(CalendarDescription.build(null, null, emptyMap()))
        assertNull(CalendarDescription.build(" ", "", emptyMap()))
    }

    @Test
    fun keepsTheExistingNotesAndTypeLayout() {
        assertEquals(
            "Notes: morning run\n\nEvent Type: Cardio",
            CalendarDescription.build("morning run", "Cardio", emptyMap())
        )
    }

    @Test
    fun putsLabelsOnTheirOwnParseableLine() {
        assertEquals(
            "Notes: morning run\n\nLabels: intensity=high,location=gym\n\nEvent Type: Cardio",
            CalendarDescription.build(
                "morning run",
                "Cardio",
                mapOf("location" to "gym", "intensity" to "high")
            )
        )
        assertEquals(
            "Labels: location=gym",
            CalendarDescription.build(null, null, mapOf("location" to "gym"))
        )
    }

    @Test
    fun putsFieldsOnTheirOwnLineAfterLabels() {
        assertEquals(
            "Labels: game=tetris\n\nFields: score=1500\n\nEvent Type: Arcade",
            CalendarDescription.build(
                null,
                "Arcade",
                mapOf("game" to "tetris"),
                mapOf("score" to FieldValue(1500.0))
            )
        )
        assertEquals(
            "Fields: heart_rate=72bpm",
            CalendarDescription.build(null, null, emptyMap(), mapOf("heart_rate" to FieldValue(72.0, "bpm")))
        )
    }
}
