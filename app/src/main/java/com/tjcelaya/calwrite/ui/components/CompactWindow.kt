package com.tjcelaya.calwrite.ui.components

import android.content.res.Configuration

/**
 * A window too small in both directions for the normal chrome: a folding phone's cover screen
 * (the razr's is 1056 x 1066 px at 360 dpi, about 469 x 474 dp) rather than a phone in landscape, which is wide. Screens
 * check this once at inflate time; folding or unfolding resizes the activity, which re-creates it.
 */
object CompactWindow {
    const val MAX_DP = 500

    fun isCompact(configuration: Configuration): Boolean =
        configuration.screenWidthDp < MAX_DP && configuration.screenHeightDp < MAX_DP
}
