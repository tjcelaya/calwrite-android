package com.tjcelaya.calwrite.ui.dialogs

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import com.tjcelaya.calwrite.data.database.EventType
import com.tjcelaya.calwrite.data.database.FieldValue
import com.tjcelaya.calwrite.data.database.OngoingEvent
import com.tjcelaya.calwrite.ui.components.RecordValuesPrompt
import java.text.SimpleDateFormat
import java.util.*

/**
 * Shared dialog for saving an ongoing event.
 * Used both from in-app UI and notification taps.
 */
object SaveEventDialog {
    
    /**
     * Show the save event confirmation dialog
     * 
     * @param context The context to show the dialog in
     * @param ongoingEvent The event to save
     * @param eventType The type of the event
     * @param onSave Callback when user confirms to save the event, with any measurements entered
     *   for the type's declared fields (empty when the type declares none or they were left blank)
     * @param onDiscardWithoutSaving Callback when user chooses to discard without saving
     * @param onCancel Callback when user cancels
     */
    fun show(
        context: Context,
        ongoingEvent: OngoingEvent,
        eventType: EventType,
        onSave: (fields: Map<String, FieldValue>) -> Unit,
        onDiscardWithoutSaving: () -> Unit,
        onCancel: () -> Unit
    ) {
        // Calculate elapsed time for display
        val elapsedMillis = System.currentTimeMillis() - ongoingEvent.startTime
        val elapsedSeconds = elapsedMillis / 1000
        val hours = elapsedSeconds / 3600
        val minutes = (elapsedSeconds % 3600) / 60
        val seconds = elapsedSeconds % 60
        val elapsedTimeString = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)

        val startTime = Date(ongoingEvent.startTime)
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        
        // Build message with consistent formatting
        val message = buildString {
            append("Elapsed time: $elapsedTimeString\n")
            append("Started at: ${timeFormat.format(startTime)}\n\n")
            append("The event will be saved to your calendar.")
        }
        
        // A type that declares fields is asked for them here, at the end, when the values are
        // known. The positive button is wired after show() so a bad entry keeps the dialog open.
        val prompt = if (RecordValuesPrompt.isNeeded(eventType)) RecordValuesPrompt(context, eventType) else null

        val dialog = AlertDialog.Builder(context)
            .setTitle("Save ${eventType.name}")
            .setMessage(message)
            .apply { prompt?.let { setView(it.view) } }
            .setPositiveButton("Save", null)
            .setNeutralButton("Discard without saving") { _, _ ->
                onDiscardWithoutSaving()
            }
            .setNegativeButton("Cancel") { _, _ ->
                onCancel()
            }
            .setOnCancelListener {
                onCancel()
            }
            .show()

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val fields = if (prompt == null) emptyMap() else prompt.read()?.fields ?: return@setOnClickListener
            dialog.dismiss()
            onSave(fields)
        }
    }
}
