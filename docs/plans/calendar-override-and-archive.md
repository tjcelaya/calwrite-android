# Per-type calendars and archived types

Two small changes to what an event type can say about itself.

## Shape

```kotlin
data class EventType(
    val id: Long,
    val name: String,
    // ...
    /** CalendarContract calendar this type's events go to; null means the one chosen in Settings. */
    val calendarId: Long? = null,       // event_types.calendarId
    /** Leaves every view except Manage events, where it can be unarchived. */
    val archived: Boolean = false,      // event_types.archived
)

/** Where a new calendar entry goes. */
fun calendarFor(type: EventType, settings: CalendarRepository): Long? =
    type.calendarId ?: settings.getSelectedCalendarId()

/** Every listing the UI reads goes through the repository, which drops archived types. */
class EventRepository {
    fun getAllEventTypes(): LiveData<List<EventType>>                   // minus archived
    fun getAllEventTypesIncludingArchived(): LiveData<List<EventType>>  // Manage events only
    suspend fun getAllEventTypesSync(): List<EventType>                 // minus archived (voice, shortcuts)
    fun getRecentCompletedEventsWithType(limit: Int): Flow<...>         // minus events of archived types
    // ... getTodaysEventsWithType, getOngoingEventsWithType, getAllOngoingEvents, getOngoingEventsSync
}
```

## Calendar override

The type editor has a **Calendar** dropdown: *Default (name of the Settings calendar)* first, then
every calendar the device knows. Only new entries are affected; an event already written stays
in the calendar it went to, and adjusting it later updates it in place there. Import's
last-occurrence seeding looks in the type's calendar. The backup carries `calendarId`.

## Archived types

The type editor has an **Archived** checkbox. An archived type, and every event of it, is left
out of the tracking screen, the ledger, voice resolution and the published voice shortcuts. It
stays in the Manage events list, marked *(archived)*, so it can be edited and unarchived. The
backup carries `archived`, so a restore keeps the marks.

## Storage

Schema 14 (`MIGRATION_13_14`) adds `event_types.calendarId INTEGER` (null) and
`event_types.archived INTEGER NOT NULL DEFAULT 0`.
