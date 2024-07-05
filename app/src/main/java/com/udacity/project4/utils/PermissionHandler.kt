package com.udacity.project4.utils

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.udacity.project4.locationreminders.geofence.GeofenceConstants.REQUEST_BACKGROUND_ONLY_PERMISSIONS_REQUEST_CODE
import com.udacity.project4.locationreminders.geofence.GeofenceConstants.REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
import com.udacity.project4.locationreminders.geofence.GeofenceConstants.REQUEST_NOTIFICATION_ONLY_PERMISSIONS_REQUEST_CODE

class PermissionHandler(
    private val activity: Activity,
    private val onAllPermissionsHandled: () -> Unit
) {

    private val permissions = mutableListOf<String>()
    private var currentPermissionIndex = 0

    fun requestRequiredPermissions(runningQOrLater: Boolean) {
        // Add permissions in the order they should be requested
        if (ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (runningQOrLater && ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestNextPermission()
    }

    private fun requestNextPermission() {
        if (currentPermissionIndex < permissions.size) {
            val permission = permissions[currentPermissionIndex]
            when (permission) {
                Manifest.permission.ACCESS_FINE_LOCATION -> {
                    Log.d(TAG, "Requesting ACCESS_FINE_LOCATION")
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
                    )
                }

                Manifest.permission.ACCESS_BACKGROUND_LOCATION -> {
                    Log.d(TAG, "Requesting ACCESS_BACKGROUND_LOCATION")
                    requestBackgroundLocationPermission()
                }

                Manifest.permission.POST_NOTIFICATIONS -> {
                    Log.d(TAG, "Requesting POST_NOTIFICATIONS")
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        REQUEST_NOTIFICATION_ONLY_PERMISSIONS_REQUEST_CODE
                    )
                }
            }
        } else {
            // All required permissions have been requested
            onAllPermissionsRequested()
        }
    }

    fun onAllPermissionsRequested() {
        Log.d(TAG, "All permissions have been requested")
    }

    fun allPermissionsGranted(runningQOrLater: Boolean): Boolean {
        return (ActivityCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                (runningQOrLater && ActivityCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED) &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ActivityCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED))
    }

    private fun requestBackgroundLocationPermission() {
        when {
            Build.VERSION.SDK_INT == Build.VERSION_CODES.Q -> {
                Log.d(TAG, "Requesting ACCESS_BACKGROUND_LOCATION for Android 10 (Q)")
                activity.requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    REQUEST_BACKGROUND_ONLY_PERMISSIONS_REQUEST_CODE
                )
            }

            Build.VERSION.SDK_INT > Build.VERSION_CODES.Q -> {
                Log.d(TAG, "Requesting ACCESS_BACKGROUND_LOCATION for Android 11 or above")
                showBackgroundLocationRationale()
            }
        }
    }

    private fun showBackgroundLocationRationale() {
        AlertDialog.Builder(activity)
            .setTitle("Background location access is required to use this app")
            .setMessage("To use this app's features, please select \"Allow all the time\" in location settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                // Open app settings
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).also {
                    val uri = Uri.fromParts("package", activity.packageName, null)
                    it.data = uri
                    activity.startActivity(it)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE,
            REQUEST_BACKGROUND_ONLY_PERMISSIONS_REQUEST_CODE,
            REQUEST_NOTIFICATION_ONLY_PERMISSIONS_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, move to next permission
                    currentPermissionIndex++
                    requestNextPermission()
                } else {
                    // Permission denied, you may want to show an explanation or proceed anyway
                    currentPermissionIndex++
                    requestNextPermission()
                }
            }
        }
    }

//    fun handlePermissionResult(permissionsResult: Map<String, Boolean>) {
//        val currentPermission = permissions.getOrNull(currentPermissionIndex)
//        if (currentPermission != null) {
//            val isGranted = permissionsResult[currentPermission] == true
//            if (isGranted) {
//                // Permission granted, move to next permission
//                currentPermissionIndex++
//                requestNextPermission()
//            } else {
//                // Permission denied, you may want to show an explanation or proceed anyway
//                currentPermissionIndex++
//                requestNextPermission()
//            }
//        } else {
//            // We've processed all permissions
//            onAllPermissionsHandled()
//        }
//    }

    companion object {
        private const val TAG = "PermissionHandler"
    }
}