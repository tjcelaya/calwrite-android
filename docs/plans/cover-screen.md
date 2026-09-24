# Cover-screen presentation

A folding phone's cover screen (the razr's is 1056 x 1066 px at 360 dpi, about 469 x 474 dp)
is a window too small in both directions for the tracking screen's normal chrome. The job there
is "tap to record", so the screen keeps only the list of types and their record actions.

## Shape

```kotlin
/** Small in both directions: a cover screen, not a phone in landscape. Checked at inflate time. */
object CompactWindow {
    const val MAX_DP = 500
    fun isCompact(configuration: Configuration): Boolean =
        configuration.screenWidthDp < MAX_DP && configuration.screenHeightDp < MAX_DP
}

class EventTypesTrackingAdapter {
    fun setCompact(enabled: Boolean)   // one-line rows (item_event_type_compact) instead of list or card
}

// TrackingFragment: chrome that goes on a compact window
private fun applyCompactChrome() {
    viewModeToggle, cardSizeRow, quickAddCard, fab -> GONE
    eventTypesAdapter.setCompact(true)
}
```

## Behaviour

- The manifest opts in to `PROPERTY_COMPAT_ALLOW_SMALL_COVER_SCREEN`, so Android 15+ runs the
  app at the cover's real size rather than letterboxed. The activity is already resizeable, so
  folding and unfolding re-create it and every screen re-inflates for the new size.
- A compact row is 44 dp: colour dot, name, last occurrence or running elapsed time, and the
  record buttons the type's cadence calls for, at 36 dp. Eight or nine types fit without scrolling.
- The user's list/card choice and card size are not touched; they apply again when unfolded.
- Other screens (ledger, type editor, settings) are left as they are: they scroll.

To try it without folding: `adb shell wm size 1056x1066 && adb shell wm density 360`, then
`wm size reset && wm density reset`.
