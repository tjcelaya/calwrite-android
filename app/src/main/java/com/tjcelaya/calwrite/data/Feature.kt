package com.tjcelaya.calwrite.data

/**
 * Optional features the user turns on from Settings. All are off on a fresh install; while off,
 * a feature's entry points are hidden and its background behaviour is inert.
 */
enum class Feature(val prefKey: String) {
    /** Camera and gallery actions, shared-image intents, and Drive / Google Photos storage. */
    PHOTOS("feature_photos"),

    /** Assistant capabilities, voice shortcuts and calwrite:// deep links. */
    VOICE("feature_voice"),

    /** Bubble notifications for ongoing events. */
    BUBBLES("feature_bubbles");
}
