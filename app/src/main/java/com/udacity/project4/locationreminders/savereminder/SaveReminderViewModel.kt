package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.base.BaseViewModel
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SaveReminderViewModel(val app: Application, val dataSource: ReminderDataSource) :
    BaseViewModel(app) {
    val reminderTitle = MutableLiveData<String?>()
    val reminderDescription = MutableLiveData<String?>()
    val reminderSelectedLocationStr = MutableLiveData<String?>()
    val selectedPOI = MutableLiveData<PointOfInterest?>()
    val latitude = MutableLiveData<Double?>()
    val longitude = MutableLiveData<Double?>()

    private val _runningQOrLater = MutableLiveData<Boolean>()
    val runningQOrLater: LiveData<Boolean>
        get() = _runningQOrLater

    private val _runningTiramisuOrLater = MutableLiveData<Boolean>()
    val runningTiramisuOrLater: LiveData<Boolean>
        get() = _runningTiramisuOrLater

    val _reminderSaved = MutableLiveData<Boolean>()
    val reminderSaved: LiveData<Boolean>
        get() = _reminderSaved

    /**
     * Clear the live data objects to start fresh next time the view model gets called
     */
    fun onClear() {
        reminderTitle.value = null
        reminderDescription.value = null
        reminderSelectedLocationStr.value = null
        selectedPOI.value = null
        latitude.value = null
        longitude.value = null
    }

    fun setRunningQOrLater(value: Boolean) {
        _runningQOrLater.value = value
    }

    fun setRunningTiramisuOrLater(value: Boolean) {
        _runningTiramisuOrLater.value = value
    }

    fun setReminderSaved(value: Boolean) {
        _reminderSaved.value = value
    }

    /**
     * Validate the entered data then saves the reminder data to the DataSource
     */
    fun validateAndSaveReminder(reminderData: ReminderDataItem) {
        if (validateEnteredData(reminderData)) {
            saveReminder(reminderData)
        } else {
            Log.e(TAG, "Invalid Data")
        }
    }

    /**
     * Save the reminder to the data source
     */
    fun saveReminder(reminderData: ReminderDataItem) {
        showLoading.value = true
        viewModelScope.launch {
            dataSource.saveReminder(
                ReminderDTO(
                    reminderData.title,
                    reminderData.description,
                    reminderData.location,
                    reminderData.latitude,
                    reminderData.longitude,
                    reminderData.id
                )
            )
            latitude.value = reminderData.latitude
            longitude.value = reminderData.longitude
            setReminderSaved(true)
            showLoading.value = false
            showToast.value = app.getString(R.string.reminder_saved)
            navigationCommand.value = NavigationCommand.Back
        }
    }

    fun getReminder(id: String): LiveData<Result<ReminderDTO>> {
        return liveData(viewModelScope.coroutineContext + Dispatchers.IO) {

            // Switch to the main thread to update showLoading
            withContext(Dispatchers.Main) {
                showLoading.value = true
            }

            try {
                val reminderResult = dataSource.getReminder(id)
                emit(reminderResult)
            } catch (e: Exception) {
                emit(Result.Error(e.message ?: "Unknown error")) // Provide a default error message
            } finally {
                // Switch back to the main thread to update showLoading
                withContext(Dispatchers.Main) {
                    showLoading.value = false
                }
            }
        }
    }

    /**
     * Validate the entered data and show error to the user if there's any invalid data
     */
    fun validateEnteredData(reminderData: ReminderDataItem): Boolean {
        if (reminderData.title.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_enter_title
            return false
        }

        if (reminderData.location.isNullOrEmpty()) {
            showSnackBarInt.value = R.string.err_select_location
            return false
        }
        return true
    }

    companion object {
        private const val TAG = "SaveReminderViewModel"
    }
}