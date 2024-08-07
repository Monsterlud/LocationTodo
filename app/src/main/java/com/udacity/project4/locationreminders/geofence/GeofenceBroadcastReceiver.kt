package com.udacity.project4.locationreminders.geofence

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.R
import com.udacity.project4.locationreminders.ReminderDescriptionActivity
import com.udacity.project4.locationreminders.geofence.GeofenceConstants.CHANNEL_ID
import com.udacity.project4.locationreminders.geofence.GeofenceConstants.EXTRA_GEOFENCE_INDEX
import com.udacity.project4.locationreminders.geofence.GeofenceConstants.EXTRA_REQUEST_ID
import com.udacity.project4.locationreminders.geofence.GeofenceConstants.NOTIFICATION_ID
import com.udacity.project4.utils.PermissionsHandler
import com.udacity.project4.utils.TAG

/**
 * Triggered by the Geofence.  Since we can have many Geofences at once, we pull the request
 * ID from the first Geofence, and locate it within the cached data in our Room DB
 *
 * Or users can add the reminders and then close the app, So our app has to run in the background
 * and handle the geofencing in the background.
 * To do that you can use https://developer.android.com/reference/android/support/v4/app/JobIntentService to do that.
 *
 */



class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent != null) {
            if (geofencingEvent.hasError()) {
                val errorMessage = PermissionsHandler().errorMessage(context, geofencingEvent.errorCode)
                return
            }
        }

        // Get the transition type.
        val geofenceTransition = geofencingEvent?.geofenceTransition

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            Log.d(TAG, "Geofence Entered")
            val triggeringGeofences = geofencingEvent?.triggeringGeofences
            val transitionDetails = getGeofenceTransitionDetails(
                context,
                geofenceTransition,
                triggeringGeofences
            )

            Log.d(
                TAG,
                "Geofence Event: Transition=${geofenceTransition}, Triggering Geofences=${triggeringGeofences?.joinToString { it.requestId }}"
            )

            val notificationManager = ContextCompat.getSystemService(
                context,
                NotificationManager::class.java
            ) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = context.getString(R.string.channel_name)
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(GeofenceConstants.CHANNEL_ID, name, importance).apply {
                    description = context.getString(R.string.channel_description)
                }
                notificationManager.createNotificationChannel(channel)

                notificationManager.sendGeofenceEnteredNotification(
                    context, 1, transitionDetails, triggeringGeofences
                )
            }
        }
    }
}

fun NotificationManager.sendGeofenceEnteredNotification(
    context: Context,
    foundIndex: Int,
    content: String,
    triggeredGeofences: List<Geofence>?
) {
    val contentIntent = Intent(context, ReminderDescriptionActivity::class.java).apply {
        putExtra(EXTRA_GEOFENCE_INDEX, foundIndex)
        putExtra(EXTRA_REQUEST_ID, triggeredGeofences?.first()?.requestId)
    }

    val contentPendingIntent = PendingIntent.getActivity(
        context,
        NOTIFICATION_ID,
        contentIntent,
        PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    val mapImage = BitmapFactory.decodeResource(
        context.resources,
        R.drawable.map_small
    )
    val bigPicStyle = NotificationCompat.BigPictureStyle()
        .bigPicture(mapImage)
        .bigLargeIcon(null as Bitmap?)

    // We use the name resource ID from the LANDMARK_DATA along with content_text to create
    // a custom message when a Geofence triggers.
    val builder = NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle(context.getString(R.string.app_name))
        .setContentText(content)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(contentPendingIntent)
        .setSmallIcon(R.drawable.map_small)
        .setStyle(bigPicStyle)
        .setLargeIcon(mapImage)
        .setAutoCancel(true)

    notify(NOTIFICATION_ID, builder.build())
}

private fun getGeofenceTransitionDetails(
    context: Context,
    geofenceTransition: Int,
    triggeringGeofences: List<Geofence>?
): String {
    val geofenceTransitionString = when (geofenceTransition) {
        Geofence.GEOFENCE_TRANSITION_ENTER -> "ENTER"
        Geofence.GEOFENCE_TRANSITION_EXIT -> "EXIT"
        else -> "UNKNOWN"
    }

    val triggeringGeofencesIdsList = triggeringGeofences?.map { it.requestId } ?: emptyList()
    val triggeringGeofencesIdsString = triggeringGeofencesIdsList.joinToString(", ")

    return "Geofence Transition: $geofenceTransitionString - Triggering Geofences: $triggeringGeofencesIdsString"
}
