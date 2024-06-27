package com.udacity.project4.locationreminders.geofence

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

class GeofenceViewModel(state: SavedStateHandle) : ViewModel() {
    private val _geofenceIndex = state.getLiveData(GEOFENCE_INDEX_KEY, -1)
    private val _locationIndex = state.getLiveData(LOCATION_INDEX_KEY, 0)
    val geofenceIndex: LiveData<Int>
        get() = _geofenceIndex


    fun geofenceIsActive() =_geofenceIndex.value == _locationIndex.value
    fun nextGeofenceIndex() = _locationIndex.value ?: 0
}

private const val GEOFENCE_INDEX_KEY = "geofenceIndex"
private const val LOCATION_INDEX_KEY = "locationIndex"
