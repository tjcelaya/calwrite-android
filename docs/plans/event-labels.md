# Event labels

Attach one or more key/value **labels** to an event, in the sense of InfluxDB tags:
`location=gym`, `intensity=high`, `with=Sam`. They describe a single occurrence so it can later be
grouped or filtered on, without having to invent a separate event type for every variation.

## Shape

```kotlin
/** One occurrence. Labels describe this occurrence only. */
data class Event(
    val id: Long,
    val eventTypeId: Long,
    val startTime: Long,
    val endTime: Long?,
    val notes: String,
    // ...
    val labels: Map<String, String> = emptyMap(),        // events.labels
)

/** A kind of event. Its defaults seed every new Event of this type. */
data class EventType(
    val id: Long,
    val name: String,
    // ...
    val defaultLabels: Map<String, String> = emptyMap(), // event_types.defaultLabels
)

/** Recording copies the defaults in; later edits to the type don't rewrite past events. */
fun labelsForNewEvent(type: EventType, given: Map<String, String>): Map<String, String> =
    type.defaultLabels + given // given wins on a shared key

/** The one text form: InfluxDB line-protocol tag set, e.g. "intensity=high,location=gym". */
object EventLabels {
    fun parse(text: String?): ParseResult          // Success(labels) | Failure(ParseError)
    fun format(labels: Map<String, String>): String // keys sorted, `\,` `\=` `\\` escaped
}
```

Because every type has defaults, one-tap recording (instant button, start/stop, voice) still
produces labelled events with no extra step. You edit per-event labels in the ledger's
**Adjust** sheet and defaults on the event type screen. Values are strings. Numeric *fields*
in the InfluxDB sense (`reps=12`) are written the same way, and whatever reads them parses the
value as a number.

## One syntax everywhere

`EventLabels` reads and writes InfluxDB line protocol's tag-set syntax:

    intensity=high,location=home gym

Comma between pairs, first `=` between key and value, and `\,` `\=` `\\` for literals. Spaces need
no escaping and are trimmed around keys and values, since people type these by hand. Keys are
written sorted (line protocol's canonical order).

The same text is used for the Room column (via a `TypeConverter`), the edit fields, and the
calendar event description, where labels get their own line:

    Notes: morning run

    Labels: intensity=high,location=gym

    Event Type: Cardio

so an export script can find them with one prefix match and one parse.

## Storage

Schema 11 (`MIGRATION_10_11`) adds both columns as `TEXT NOT NULL DEFAULT ''`; empty text is an
empty label set, so existing rows need no backfill. Default labels are included in the config
backup as `defaultLabels`.
