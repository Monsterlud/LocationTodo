package com.udacity.project4.locationreminders

import android.os.Handler
import android.os.Looper
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel

class ToastIdlingResourceHelper(
    private val viewModel: SaveReminderViewModel
) {
    val toastIdlingResource = ToastIdlingResource()
    private var toastShown = false

    var toastMessage: String? = null

    init {
        viewModel.showToast.observeForever { message ->
            if (message != null) {
                toastShown = true
                toastIdlingResource.setIdleState(false)
                toastMessage = message
                // Simulate the duration of the toast
                Handler(Looper.getMainLooper()).postDelayed({
                    toastIdlingResource.setIdleState(true)
                }, 1000)
            }
        }
    }

    fun resetToastFlag() {
        toastShown = false
    }

    fun wasToastShown(): Boolean = toastShown
}
