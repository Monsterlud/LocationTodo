package com.monsalud.locationtodo.locationreminders.savereminder

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

class GeofenceLifecycleObserver(
    private val fragment: SaveReminderFragment,
    private val onGeofenceAdded: (Boolean) -> Unit
) : LifecycleObserver {

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        // Handle the case when the fragment is destroyed
        // You can cancel any pending operations or clean up resources here
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        // Perform actions when the fragment is resumed
        // For example, you can start the geofence setup process here
        setupGeofence()
    }

    private fun setupGeofence() {
        val latitude = fragment._viewModel.latitude.value
        val longitude = fragment._viewModel.longitude.value
        val reminderDTO = fragment.reminderDTO
        // Perform geofence setup operations here
        // You can call the addLocationReminderGeofence method from this function
        // and handle the success/failure cases accordingly
        fragment.addLocationReminderGeofence(
            latitude!!,
            longitude!!,
            reminderDTO.id,
            onGeofenceAdded
        )
    }
}