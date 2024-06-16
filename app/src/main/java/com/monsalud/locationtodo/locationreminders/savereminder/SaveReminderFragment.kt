package com.monsalud.locationtodo.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.monsalud.locationtodo.BuildConfig
import com.monsalud.locationtodo.R
import com.monsalud.locationtodo.base.BaseFragment
import com.monsalud.locationtodo.base.NavigationCommand
import com.monsalud.locationtodo.databinding.FragmentSaveReminderBinding
import com.monsalud.locationtodo.locationreminders.geofence.GeofenceBroadcastReceiver
import com.monsalud.locationtodo.locationreminders.geofence.GeofenceConstants
import com.monsalud.locationtodo.locationreminders.geofence.GeofenceUtils
import com.monsalud.locationtodo.locationreminders.geofence.GeofenceViewModel
import com.monsalud.locationtodo.locationreminders.reminderslist.ReminderDataItem
import com.monsalud.locationtodo.utils.setDisplayHomeAsUpEnabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

private const val TAG = "SaveReminderFragment"

class SaveReminderFragment : BaseFragment() {
    private lateinit var binding: FragmentSaveReminderBinding

    override val _viewModel: SaveReminderViewModel by inject()
    private val geofenceViewModel by inject<GeofenceViewModel>()
    private val geofenceUtils = GeofenceUtils()

    private lateinit var geofencingClient: GeofencingClient
    private lateinit var geofencePendingIntent: PendingIntent
    lateinit var reminderDTO: ReminderDataItem
    private var runningQOrLater: Boolean = false

    private lateinit var geofenceLifecycleObserver: GeofenceLifecycleObserver
    private lateinit var locationsSettingsResponseTask: Task<LocationSettingsResponse>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        runningQOrLater = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

        setDisplayHomeAsUpEnabled(true)
        setHasOptionsMenu(true)
        binding.viewModel = _viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())
        runningQOrLater = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q

        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        val requestCode = System.currentTimeMillis().toInt()
        geofencePendingIntent = PendingIntent.getBroadcast(
            requireContext(),
            requestCode,
            intent,
            PendingIntent.FLAG_MUTABLE
        )

        binding.lifecycleOwner = this
        binding.selectLocation.setOnClickListener {
            // Navigate to another fragment to get the user location
            _viewModel.navigationCommand.postValue(
                NavigationCommand.To(SaveReminderFragmentDirections.toSelectLocationFragment())
            )
        }

        binding.saveReminder.setOnClickListener {
            Log.d(TAG, "***onViewCreated: isAdded: $isAdded")
            reminderDTO = ReminderDataItem(
                binding.reminderTitle.text.toString(),
                binding.reminderDescription.text.toString(),
                _viewModel.reminderSelectedLocationStr.value,
                _viewModel.latitude.value,
                _viewModel.longitude.value
            )

            viewLifecycleOwner.lifecycleScope.launch {
                _viewModel.validateAndSaveReminder(reminderDTO)
            }
            _viewModel.reminderSaved.observe(viewLifecycleOwner) { reminderSaved ->
                if (reminderSaved) {
                    checkPermissionsAndStartGeofencing { success ->
                        Log.d(TAG, "***onViewCreated: isAdded: $isAdded")
                        if (success) {
                            _viewModel.setReminderSaved(false)
                            Log.d(TAG, "***onViewCreated: ***popbackstackedcalled")
                            findNavController().popBackStack()
                        } else {
                            Snackbar.make(
                                binding.root,
                                R.string.geofences_not_added,
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
        }
    }

    private fun checkPermissionsAndStartGeofencing(onGeofenceAdded: (Boolean) -> Unit) {
        Log.d(TAG, "***checkPermissionsAndStartGeofencing: isAdded: $isAdded")
        if (geofenceUtils.foregroundAndBackgroundLocationPermissionApproved(
                requireContext(),
                runningQOrLater
            )
        ) {
            checkDeviceLocationSettingsAndStartGeofence(onGeofenceAdded = onGeofenceAdded)
        } else {
            geofenceUtils.requestForegroundAndBackgroundLocationPermissions(
                requireContext(),
                requireActivity(),
                runningQOrLater
            )
        }
    }

    private fun checkDeviceLocationSettingsAndStartGeofence(
        resolve: Boolean = true,
        onGeofenceAdded: (Boolean) -> Unit
    ) {
        Log.d(TAG, "***checkDeviceLocationSettingsAndStartGeofence: isAdded: $isAdded")


        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())

        Log.d(TAG, "*** right before locationssettingsresponsetask: isAdded: $isAdded ")


        viewLifecycleOwner.lifecycleScope.launch {
            locationsSettingsResponseTask =
                settingsClient.checkLocationSettings(builder.build())
            _viewModel.setLocationsSettingsResponseTaskFinished(true)
        }
        _viewModel.locationsSettingsResponseTaskFinished.observe(viewLifecycleOwner) { taskFinished ->
            if (taskFinished) {
                Log.d(TAG, "*** right after locationssettingsresponsetask: isAdded: $isAdded ")
                handleLocationSettingsResponse(resolve, onGeofenceAdded)
            }
        }
    }

    private fun handleLocationSettingsResponse(
        resolve: Boolean,
        onGeofenceAdded: (Boolean) -> Unit
    ) {
        val latitude = _viewModel.latitude.value
        val longitude = _viewModel.longitude.value

        locationsSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                onGeofenceAdded(false)
                try {
                    exception.startResolutionForResult(
                        requireActivity(),
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
                    checkDeviceLocationSettingsAndStartGeofence(onGeofenceAdded = onGeofenceAdded)
                }.show()
            }
            _viewModel.setLocationsSettingsResponseTaskFinished(false)
        }
        locationsSettingsResponseTask.addOnCompleteListener {
            Log.d(
                TAG,
                "***locationssettingsresponsetask.addoncompletelistener (this is right before addlocationgeofence: isAdded: $isAdded"
            )
            if (it.isSuccessful) {
                addLocationReminderGeofence(
                    latitude!!,
                    longitude!!,
                    reminderDTO.id,
                    onGeofenceAdded
                )
            }
            _viewModel.setLocationsSettingsResponseTaskFinished(false)
        }
    }


    @SuppressLint("VisibleForTests", "MissingPermission")
    fun addLocationReminderGeofence(
        latitude: Double,
        longitude: Double,
        id: String,
        onGeofenceAdded: (Boolean) -> Unit
    ) {
        Log.d(TAG, "***addLocationReminderGeofence: isAdded: $isAdded")
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

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        Log.d(TAG, "***addLocationReminderGeofence: adding geofence")
        Log.d(TAG, "addLocationReminderGeofence: ***right before addgeofences: isAdded: $isAdded")

        geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                if (isAdded) {
                    Log.d(TAG, "***registered geofence: $geofence")
                    Log.d(
                        TAG,
                        "***addLocationReminderGeofence: geofence: $geofence, geofencingrequest: ${geofencingRequest}, geofencependingintent${geofencePendingIntent}"
                    )
                    Toast.makeText(
                        requireContext(),
                        R.string.geofence_added,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                onGeofenceAdded(true)
            }
            addOnFailureListener {
                Log.d(TAG, "addLocationReminderGeofence: addonfailurelistener: isAdded: $isAdded")
                if (isAdded) {
                    Toast.makeText(
                        requireContext(),
                        R.string.geofences_not_added,
                        Toast.LENGTH_SHORT
                    ).show()
                    it.message?.let { Log.w(TAG, it) }
                }
                Log.d(
                    TAG,
                    "***addLocationReminderGeofence: geofence: $geofence, geofencingrequest: ${geofencingRequest}, geofencependingintent${geofencePendingIntent}"
                )
                Log.d(TAG, "***addLocationReminderGeofence: isAdded: $isAdded")
                onGeofenceAdded(false)
            }
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d(TAG, "onRequestPermissionResult triggered")
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
            checkDeviceLocationSettingsAndStartGeofence(onGeofenceAdded = {

            })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GeofenceConstants.REQUEST_TURN_DEVICE_LOCATION_ON) {
            checkDeviceLocationSettingsAndStartGeofence(onGeofenceAdded = {

            })
        }
    }

    private fun removeGeofences() {
        if (!geofenceUtils.foregroundAndBackgroundLocationPermissionApproved(
                requireContext(),
                runningQOrLater
            )
        ) return

        geofencingClient.removeGeofences(geofencePendingIntent)?.run {
            addOnSuccessListener {
                Log.d(
                    com.monsalud.locationtodo.locationreminders.geofence.TAG,
                    getString(R.string.geofences_removed)
                )
                Toast.makeText(
                    requireContext(),
                    getString(R.string.geofences_removed),
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
            addOnFailureListener {
                Log.d(
                    com.monsalud.locationtodo.locationreminders.geofence.TAG,
                    getString(R.string.geofences_not_removed)
                )
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                findNavController().navigateUp()
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    companion object {
        internal const val ACTION_GEOFENCE_EVENT =
            "SaveReminderFragment"
    }
}
