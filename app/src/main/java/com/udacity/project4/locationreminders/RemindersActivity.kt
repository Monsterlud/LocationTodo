package com.udacity.project4.locationreminders

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityRemindersBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofenceConstants
import com.udacity.project4.locationreminders.geofence.GeofenceConstants.REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
import com.udacity.project4.locationreminders.geofence.GeofenceConstants.REQUEST_NOTIFICATION_AND_BACKGROUND_PERMISSIONS_REQUEST_CODE
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.savereminder.SaveReminderFragment
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.locationreminders.savereminder.selectreminderlocation.SelectLocationFragment
import org.koin.android.ext.android.inject

/**
 * The RemindersActivity that holds the reminders fragments
 */
class RemindersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRemindersBinding

    private val _viewModel: SaveReminderViewModel by inject()
    private var geofenceSetupListener: GeofenceSetupListener? = null

    private lateinit var navController: NavController
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var geofencePendingIntent: PendingIntent

    private var reminderDTO: ReminderDataItem? = null
    private var requestCodeCounter = 0

    interface GeofenceSetupListener {
        fun onGeofenceAdded(success: Boolean)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRemindersBinding.inflate(layoutInflater)

        _viewModel.setRunningQOrLater(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)
        _viewModel.setRunningTiramisuOrLater(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
        geofencingClient = LocationServices.getGeofencingClient(this)

        geofencePendingIntent = createGeofencePendingIntent()

        setContentView(binding.root)
    }

    override fun onResume() {
        super.onResume()

        navController = findNavController(R.id.nav_host_fragment)
        val appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)
    }

    private fun createGeofencePendingIntent(): PendingIntent {
        val intent = Intent(applicationContext, GeofenceBroadcastReceiver::class.java)
        intent.action = GeofenceConstants.ACTION_GEOFENCE_EVENT

        val requestCode = requestCodeCounter++
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_MUTABLE
        )
        return pendingIntent
    }

    fun checkDeviceLocationSettingsAndStartGeofence(
        resolve: Boolean = true,
        reminderDataItem: ReminderDataItem
    ) {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(this)

        val locationsSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())

        val latitude = reminderDataItem.latitude
        val longitude = reminderDataItem.longitude

        locationsSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    exception.startResolutionForResult(
                        this,
                        GeofenceConstants.REQUEST_TURN_DEVICE_LOCATION_ON
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(
                        TAG,
                        "*****Error getting location settings resolution: " + sendEx.message
                    )
                }
            } else {
                Snackbar.make(
                    binding.root,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(android.R.string.ok) {
                    checkDeviceLocationSettingsAndStartGeofence(reminderDataItem = reminderDTO!!)
                }.show()
            }
        }
        locationsSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                val dto = reminderDTO
                addLocationReminderGeofence(
                    latitude!!,
                    longitude!!,
                    reminderDataItem.id,
                )
            }
        }
    }

    @SuppressLint("VisibleForTests", "MissingPermission")
    fun addLocationReminderGeofence(
        latitude: Double,
        longitude: Double,
        id: String
    ) {
        val geofence = Geofence.Builder()
            .setRequestId(id)
            .setCircularRegion(
                latitude,
                longitude,
                GeofenceConstants.CIRCULAR_REGION_RADIUS
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()
        Log.d(
            TAG,
            "addLocationReminderGeofence: GEOFENCE: ${geofence.latitude}, ${geofence.longitude}, ${geofence.requestId}, ${geofence.radius}, ${geofence.expirationTime}, ${geofence.transitionTypes}"
        )

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        geofencingClient.removeGeofences(geofencePendingIntent).run {
            addOnCompleteListener {
                geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                    addOnSuccessListener {
                        Toast.makeText(
                            applicationContext,
                            R.string.geofence_added,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    addOnFailureListener { exception ->
                        if (exception is ApiException) {
                            val statusCode = exception.statusCode
                            val errorMessage = GeofenceStatusCodes.getStatusCodeString(statusCode)
                            println("status code: ${exception.statusCode}")
                            println("error message: $errorMessage")
                        }
                        Log.d(
                            TAG,
                            "addLocationReminderGeofence: Exception ${exception.message}, ${exception.localizedMessage}, ${exception.stackTrace}"
                        )
                        Toast.makeText(
                            applicationContext,
                            R.string.geofences_not_added,
                            Toast.LENGTH_SHORT
                        ).show()
                        exception.message?.let {
                            Log.w(TAG, exception.message!!)
                        }
                        geofenceSetupListener?.onGeofenceAdded(false)
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        Log.d(TAG, "onRequestPermissionsResult called with requestCode: $requestCode")
        Log.d(TAG, "Permissions: ${permissions.joinToString()}")
        Log.d(TAG, "GrantResults: ${grantResults.joinToString()}")

        when (requestCode) {
            REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Foreground location permission granted")
                    // Now request background location permission if running Q or later
                    val currentFragment =
                        supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                            ?.childFragmentManager?.fragments?.firstOrNull()
                    Log.d(TAG, "Current Fragment: $currentFragment")
                    if (currentFragment is SelectLocationFragment) {
                        currentFragment.onLocationPermissionGranted()
                    }
                } else {
                    Log.d(TAG, "Foreground location permission denied")
                    Toast.makeText(this, "My Location feature of Google Maps is not available", Toast.LENGTH_LONG).show()
                }
            }
            REQUEST_NOTIFICATION_AND_BACKGROUND_PERMISSIONS_REQUEST_CODE -> {
                val allPermissionsGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
                val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
                    ?.childFragmentManager?.fragments?.firstOrNull() as? SaveReminderFragment

                if (allPermissionsGranted) {
                    currentFragment?.checkPermissionsAndSaveReminder()
                } else {
                    Toast.makeText(this, "You must grant all permissions to use this feature", Toast.LENGTH_LONG).show()
                }
            }
            else -> {}
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        navController.navigateUp() || super.onSupportNavigateUp()
        return true
    }

    companion object {
        private const val TAG = "RemindersActivity"
    }
}
