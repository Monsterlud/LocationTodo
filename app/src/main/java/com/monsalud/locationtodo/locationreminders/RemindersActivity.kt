package com.monsalud.locationtodo.locationreminders

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
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
import com.monsalud.locationtodo.BuildConfig
import com.monsalud.locationtodo.R
import com.monsalud.locationtodo.databinding.ActivityRemindersBinding
import com.monsalud.locationtodo.locationreminders.geofence.GeofenceBroadcastReceiver
import com.monsalud.locationtodo.locationreminders.geofence.GeofenceConstants
import com.monsalud.locationtodo.locationreminders.geofence.GeofenceUtils
import com.monsalud.locationtodo.locationreminders.reminderslist.ReminderDataItem
import com.monsalud.locationtodo.locationreminders.savereminder.SaveReminderFragment
import com.monsalud.locationtodo.locationreminders.savereminder.SaveReminderFragmentDirections
import com.monsalud.locationtodo.locationreminders.savereminder.SaveReminderViewModel
import com.monsalud.locationtodo.locationreminders.savereminder.selectreminderlocation.SelectLocationFragment
import org.koin.android.ext.android.inject

private const val TAG = "RemindersActivity"

/**
 * The RemindersActivity that holds the reminders fragments
 */
class RemindersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRemindersBinding

    private val geofenceUtils = GeofenceUtils()
    private val _viewModel: SaveReminderViewModel by inject()
    private var geofenceSetupListener: GeofenceSetupListener? = null

    private lateinit var navController: NavController
    private lateinit var geofencingClient: GeofencingClient
    private lateinit var geofencePendingIntent: PendingIntent
    private var reminderDTO: ReminderDataItem? = null

    private var isGeofenceSetupPending = false
    private var requestCodeCounter = 0

    interface GeofenceSetupListener {
        fun onGeofenceAdded(success: Boolean)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRemindersBinding.inflate(layoutInflater)

        _viewModel.setRunningQOrLater(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q)
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "***onRequestPermissionsResult() triggered")

        if (grantResults.isEmpty() ||
            grantResults[GeofenceConstants.LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == GeofenceConstants.REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[GeofenceConstants.BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED)
        ) {
            Snackbar.make(
                binding.root,
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        } else {
            (supportFragmentManager.findFragmentById(R.id.selectLocationFragment) as? SelectLocationFragment)?.onLocationPermissionGranted()
        }
    }

    fun checkPermissionsAndStartGeofencing(
        reminderDataItem: ReminderDataItem,
        listener: GeofenceSetupListener
    ) {
        Log.d(
            TAG,
            "checkPermissionsAndStartGeofencing: ***checkpermissionsandstartgeofencing() triggered"
        )
        this.reminderDTO = reminderDataItem
        checkDeviceLocationSettingsAndStartGeofence(reminderDataItem = reminderDTO!!)
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

        val latitude = reminderDTO?.latitude
        val longitude = reminderDTO?.longitude

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
                addLocationReminderGeofence(
                    latitude!!,
                    longitude!!,
                    reminderDTO!!.id,
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

    override fun onSupportNavigateUp(): Boolean {
        navController.navigateUp() || super.onSupportNavigateUp()
        return true
    }
}

