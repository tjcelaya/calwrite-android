package com.tjcelaya.calwrite.ui.events

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.tjcelaya.calwrite.data.EventRepository
import com.tjcelaya.calwrite.data.database.EventType
import kotlinx.coroutines.launch

class AddEditEventTypeViewModel(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _saveResult = MutableLiveData<Boolean>()
    val saveResult: LiveData<Boolean> = _saveResult

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _loadedEventType = MutableLiveData<EventType?>()
    val loadedEventType: LiveData<EventType?> = _loadedEventType

    /** Every type, archived included, so the unit picker can offer units already in use. */
    val allEventTypes: LiveData<List<EventType>> = eventRepository.getAllEventTypesIncludingArchived()

    fun saveEventType(eventType: EventType) {
        viewModelScope.launch {
            try {
                // Check for duplicate names, archived ones included: the name index is unique.
                val sameName = eventRepository.findEventTypeByNameIncludingArchived(eventType.name.trim())
                if (sameName != null && sameName.id != eventType.id) {
                    _errorMessage.value = if (sameName.archived) {
                        "That name belongs to an archived event type. Unarchive it from Manage events instead."
                    } else {
                        "An event type with this name already exists"
                    }
                    _saveResult.value = false
                    return@launch
                }

                // An archived type leaves the tracking screen, so a running event of it could
                // never be stopped from there.
                if (eventType.archived && eventType.id != 0L &&
                    eventRepository.getOngoingEventCountForType(eventType.id) > 0
                ) {
                    _errorMessage.value = "Stop the running ${eventType.name} event before archiving it"
                    _saveResult.value = false
                    return@launch
                }

                if (eventType.id == 0L) {
                    // Creating new event type
                    eventRepository.insertEventType(eventType)
                } else {
                    // Updating existing event type
                    eventRepository.updateEventType(eventType)
                }
                _saveResult.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Error saving event type: ${e.message}"
                _saveResult.value = false
            }
        }
    }

    fun loadEventType(eventTypeId: Long) {
        viewModelScope.launch {
            try {
                val eventType = eventRepository.getEventTypeById(eventTypeId)
                _loadedEventType.value = eventType
            } catch (e: Exception) {
                _errorMessage.value = "Error loading event type: ${e.message}"
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}

class AddEditEventTypeViewModelFactory(
    private val eventRepository: EventRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddEditEventTypeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddEditEventTypeViewModel(eventRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
