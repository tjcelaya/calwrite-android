# Event labels and fields

Attach key/value data to an event, in the sense of InfluxDB's two kinds of column:

- **Labels** (InfluxDB *tags*) identify an occurrence: `location=gym`, `game=tetris`, `with=Sam`.
  String values. What you group or filter on.
- **Fields** (InfluxDB *fields*) measure it: `heart_rate=72bpm`, `score=1500`, `distance=5.2km`.
  A number with a unit. What you chart or aggregate.

"I got a high score of 1500 in Tetris" is one event with the label `game=tetris` and the field
`score=1500`; "my heart rate was 72 bpm" is one event with the field `heart_rate=72bpm`.

## Shape

```kotlin
/** A measured value and the unit it was measured in. Unit may be blank (a bare count or score). */
data class FieldValue(val number: Double, val unit: String = "")

/** One occurrence. Labels and fields describe this occurrence only. */
data class Event(
    val id: Long,
    val eventTypeId: Long,
    val startTime: Long,
    val endTime: Long?,
    val notes: String,
    // ...
    val labels: Map<String, String> = emptyMap(),          // events.labels
    val fields: Map<String, FieldValue> = emptyMap(),      // events.fields
)

/** What a type declares about one field: its unit, and optionally a value the prompt starts from. */
data class FieldSpec(val unit: String = "", val default: Double? = null)

/** A kind of event. Its defaults seed every new Event; its field specs say what to ask for. */
data class EventType(
    val id: Long,
    val name: String,
    // ...
    val defaultLabels: Map<String, String> = emptyMap(),   // event_types.defaultLabels: key -> default value
    val fieldSpecs: Map<String, FieldSpec> = emptyMap(),   // event_types.fieldSpecs: key -> unit + default
)

/** Recording copies the defaults in; later edits to the type don't rewrite past events. */
fun labelsForNewEvent(type: EventType, given: Map<String, String>): Map<String, String> =
    type.defaultLabels + given // given wins on a shared key

/** A field's default only pre-fills the prompt; the occurrence still supplies the value. */
fun fieldsForNewEvent(given: Map<String, FieldValue>): Map<String, FieldValue> = given

/** Text forms: InfluxDB line-protocol tag-set syntax, fields with the unit glued to the number. */
object EventLabels {
    fun parse(text: String?): ParseResult                    // Success(labels) | Failure(ParseError)
    fun format(labels: Map<String, String>): String          // keys sorted, `\,` `\=` `\\` escaped
}
object EventFields {
    fun parse(text: String?): ParseResult                    // "heart_rate=72bpm,score=1500"
    fun format(fields: Map<String, FieldValue>): String
    fun parseValue(text: String): FieldValue?                // "72bpm" | "72 bpm" -> FieldValue(72.0, "bpm")
    fun formatValue(value: FieldValue): String               // FieldValue(5.2, "km") -> "5.2km"
    fun parseSpecs(text: String?): Map<String, FieldSpec>    // "heart_rate=bpm,score=,weight=70kg"
    fun formatSpecs(specs: Map<String, FieldSpec>): String
}
```

## Where values come from

Labels have defaults, so one-tap recording (instant button, start/stop, voice) still produces
labelled events with no extra step. Fields do not: a heart rate without the bpm is nothing, so a
type that declares `fieldUnits` is asked for the values at the moment they are known.

```kotlin
/** One numeric input per declared field, plus the labels line when the type seeds labels. */
class RecordValuesPrompt(context: Context, type: EventType) {
    val view: View
    fun read(): Values?                                      // null (and an inline error) on bad input
    data class Values(val fields: Map<String, FieldValue>, val labels: Map<String, String>)
}
```

- **Instant record** (tracking screen): the prompt appears before the write. Cancel records nothing.
- **Stop** (tracking screen, notification, voice confirmation): the prompt is embedded in the
  existing save dialog, since a timed event's values are usually known at the end.
- **Start**: no prompt; values are entered at stop.
- **Deep link**: `calwrite://action/record?name=HR&fields=heart_rate=72bpm&labels=game=tetris`.
  `labels` rides on record and start, `fields` on record and stop. Malformed text is dropped, not
  fatal. Assistant's built-in intents carry neither.
- **Afterwards**: both are editable per event in the ledger's **Adjust** sheet, as text.

Blank inputs in the prompt are omitted rather than stored as zero. A field whose spec carries a
default has the prompt pre-filled with it.

## Editing a type

The type screen has a **Labels** section above a **Fields** section, each a list of rows with an
Add button. A label row is name and default value; a field row is name, default value and a unit
dropdown. The dropdown offers *None*, a built-in handful (`Units.BUILT_IN`), every unit already
used on any type, units the user added before, and *Add a new unit…* last, which opens a dialog and
remembers the answer in `StoragePreferences` so it is offered from then on.

```kotlin
class AttributeRowsEditor(container: LinearLayout, kind: Kind, unitSource: UnitSource?) {
    enum class Kind { LABELS, FIELDS }
    fun showLabels(labels: Map<String, String>);  fun readLabels(): Map<String, String>?   // null: fix the marked row
    fun showFields(specs: Map<String, FieldSpec>); fun readFields(): Map<String, FieldSpec>?
    fun addEmptyRow()
}
```

The concise text syntax stays for the ledger's Adjust sheet and for deep links.

## One syntax everywhere

Both read and write InfluxDB line protocol's tag-set syntax:

    intensity=high,location=home gym
    distance=5.2km,heart_rate=72bpm,score=1500

Comma between pairs, first `=` between key and value, and `\,` `\=` `\\` for literals. Spaces need
no escaping and are trimmed around keys and values, since people type these by hand. Keys are
written sorted (line protocol's canonical order). A field value is a number immediately followed
by its unit; a space between them is tolerated on input. Whole numbers are written without a
decimal point.

A type's declared fields use the label syntax with the value being an optional default followed
by the unit: `heart_rate=bpm,score=,weight=70kg`.

The same text is used for the Room columns (via `TypeConverter`s), the edit fields, and the
calendar event description, where each kind gets its own line:

    Notes: morning run

    Labels: intensity=high,location=gym

    Fields: distance=5.2km,heart_rate=72bpm

    Event Type: Cardio

so an export script can find each with one prefix match and one parse.

## Storage

Schema 11 (`MIGRATION_10_11`) adds `events.labels` and `event_types.defaultLabels`; schema 12
(`MIGRATION_11_12`) adds `events.fields` and `event_types.fieldUnits`; schema 13
(`MIGRATION_12_13`) renames that last column to `fieldSpecs`. All four are
`TEXT NOT NULL DEFAULT ''`; empty text is an empty set, so existing rows need no backfill. Default
labels and field specs are included in the config backup as `defaultLabels` and `fieldSpecs`.
