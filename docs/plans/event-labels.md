# Event labels

Attach one or more key/value **labels** to an event, in the sense of InfluxDB tags:
`location=gym`, `intensity=high`, `with=Sam`. They describe a single occurrence so it can later be
grouped or filtered on, without having to invent a separate event type for every variation.

## Shape

- **Per event**: `events.labels`. Every recorded event carries its own set.
- **Per type, as defaults**: `event_types.defaultLabels`. A new event starts with its type's
  defaults, so one-tap recording (instant button, start/stop, voice) still produces labelled
  events with no extra step. Defaults are copied at record time; changing a type's defaults later
  does not rewrite past events.
- Edit per-event labels from the ledger's **Adjust** sheet; edit defaults on the event type screen.

Values are strings. Numeric *fields* in the InfluxDB sense (`reps=12`) can be written the same
way; anything consuming them can parse the value as a number.

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
