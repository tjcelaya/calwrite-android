# Feature flags: photos, voice, bubbles

Three optional parts of the app are off on a fresh install and turned on from a **Features**
section at the top of Settings. Each switch reveals that feature's own settings below it.

## Shape

```kotlin
enum class Feature(val prefKey: String) { PHOTOS, VOICE, BUBBLES }

class StoragePreferences {
    fun isFeatureEnabled(feature: Feature): Boolean          // default false
    fun setFeatureEnabled(feature: Feature, enabled: Boolean)
    fun getBubbleMode(): BubbleMode                           // NEVER while BUBBLES is off
    fun getChosenBubbleMode(): BubbleMode                     // what the radio group shows
}
```

## What each flag gates

- **Photos.** The "Capture a moment" and "Save an existing photo" actions in the tracking
  screen's menu; images shared into the app (refused with a toast); the Photo Storage, Drive,
  Google Photos and Save-photos-to settings sections.
- **Voice.** `VoiceShortcutPublisher.publish()` removes every voice shortcut and disables the
  `VoiceActionActivity` component while the flag is off, so neither a launcher long-press nor
  Assistant can reach voice control; the activity also finishes with a toast if an intent was
  already in flight. The Voice & Assistant settings section is hidden. The ledger stays, since
  it is not a voice surface.
- **Bubbles.** `getBubbleMode()` reports NEVER while the flag is off, which is the single point
  `NotificationService` and the type editor's bubble switch read; the chosen mode is kept for
  when the flag comes back. The Notifications settings section is hidden.

Nothing is deleted when a flag is turned off: stored photos, calendar links, shortcut plans and
the bubble choice all survive a round trip.
