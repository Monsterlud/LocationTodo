package com.udacity.project4.utils

import android.Manifest
import android.annotation.TargetApi
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.GeofenceStatusCodes
import com.udacity.project4.R
import com.udacity.project4.locationreminders.geofence.GeofenceConstants.REQUEST_BACKGROUND_ONLY_PERMISSIONS_REQUEST_CODE
import com.udacity.project4.locationreminders.geofence.GeofenceConstants.REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE

const val TAG = "PermissionsHandler"

class PermissionsHandler {

    fun isForegroundLocationPermissionGranted(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isBackgroundLocationPermissionGranted(context: Context, runningQOrLater: Boolean) : Boolean {
        return if (runningQOrLater) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @TargetApi(29)
    fun requestForegroundLocationPermission(activity: Activity) {
        if (isForegroundLocationPermissionGranted(activity)) return

        Log.d(TAG, "Requesting foreground location permission")
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        )
    }

    fun requestBackgroundLocationPermission(activity: Activity) {
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
                showBackgroundLocationRationale(activity)
            }
        }
    }

    private fun showBackgroundLocationRationale(activity: Activity) {
        AlertDialog.Builder(activity)
            .setTitle("Background location permission is required to use Geofencing features")
            .setMessage("To use this app's geofencing features, please select \"Allow all the time\" in location settings then navigate back to the app.")
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

    /**
     * Returns the error string for a geofencing error code.
     */
    fun errorMessage(context: Context, errorCode: Int): String {
        val resources = context.resources
        return when (errorCode) {
            GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> resources.getString(
                R.string.geofence_not_available
            )

            GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> resources.getString(
                R.string.geofence_too_many_geofences
            )

            GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> resources.getString(
                R.string.geofence_too_many_pending_intents
            )

            else -> resources.getString(R.string.unknown_geofence_error)
        }
    }
}
