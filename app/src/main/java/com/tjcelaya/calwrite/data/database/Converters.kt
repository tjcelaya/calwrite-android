package com.tjcelaya.calwrite.data.database

import androidx.room.TypeConverter

/**
 * Room type converters for CalWrite entities.
 */
class Converters {

    @TypeConverter
    fun fromCadence(cadence: Cadence): String = cadence.name

    @TypeConverter
    fun toCadence(value: String?): Cadence = Cadence.fromName(value)

    @TypeConverter
    fun fromLabels(labels: Map<String, String>): String = EventLabels.format(labels)

    @TypeConverter
    fun toLabels(value: String?): Map<String, String> = EventLabels.parseOrEmpty(value)

    @TypeConverter
    fun fromFields(fields: Map<String, FieldValue>): String = EventFields.format(fields)

    @TypeConverter
    fun toFields(value: String?): Map<String, FieldValue> = EventFields.parseOrEmpty(value)

    @TypeConverter
    fun fromFieldSpecs(specs: Map<String, FieldSpec>): String = EventFields.formatSpecs(specs)

    @TypeConverter
    fun toFieldSpecs(value: String?): Map<String, FieldSpec> = EventFields.parseSpecs(value)
}
