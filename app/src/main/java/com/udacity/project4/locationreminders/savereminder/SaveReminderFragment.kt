package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.geofence.GeofenceConstants
import com.udacity.project4.locationreminders.geofence.GeofenceConstants.REQUEST_NOTIFICATION_ONLY_PERMISSIONS_REQUEST_CODE
import com.udacity.project4.locationreminders.geofence.GeofenceUtils
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {
    private lateinit var binding: FragmentSaveReminderBinding

    override val _viewModel: SaveReminderViewModel by inject()
    private val geofenceUtils: GeofenceUtils by inject()

    private lateinit var reminderDTO: ReminderDataItem
    private var shouldShowSnackbarInsteadOfDialog = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val areAllPermissionsGranted = permissions.values.all { it }
        if (areAllPermissionsGranted) {
            _viewModel.navigationCommand.postValue(
                NavigationCommand.To(
                    SaveReminderFragmentDirections.toSelectLocationFragment()
                )
            )
        } else {
            if (shouldShowSnackbarInsteadOfDialog) {
                Snackbar.make(
                    binding.root,
                    R.string.both_permissions_denied_explanation,
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
                geofenceUtils.requestForegroundLocationPermission(
                    requireActivity(),
                )
                shouldShowSnackbarInsteadOfDialog = true
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)
        setHasOptionsMenu(true)

        binding.viewModel = _viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = this

        binding.selectLocation.setOnClickListener {
            // Navigate to another fragment to get the user location
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }

        binding.saveReminder.setOnClickListener {
            reminderDTO = ReminderDataItem(
                binding.reminderTitle.text.toString(),
                binding.reminderDescription.text.toString(),
                _viewModel.reminderSelectedLocationStr.value,
                _viewModel.latitude.value,
                _viewModel.longitude.value
            )

            // Check notification permissions if on Tiramisu or later
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val notificationManager =
                    requireContext().getSystemService(NotificationManager::class.java)
                if (!notificationManager.areNotificationsEnabled()) {
                    requestNotificationPermission()
                    return@setOnClickListener // Exit early if permissions are missing
                }
            }

            // Check background location permissions if running Q or later
            if (_viewModel.runningQOrLater.value == true &&
                !geofenceUtils.isBackgroundLocationPermissionGranted(
                    requireContext(),
                    _viewModel.runningQOrLater.value!!
                )
            ) {
                geofenceUtils.requestBackgroundLocationPermission(
                    requireActivity()
                )
                return@setOnClickListener // Exit early if permissions are missing
            }

            viewLifecycleOwner.lifecycleScope.launch {
                _viewModel.validateAndSaveReminder(reminderDTO)
            }

            _viewModel.reminderSaved.observe(viewLifecycleOwner) { reminderSaved ->
                if (reminderSaved) {
                    findNavController().popBackStack()
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_NOTIFICATION_ONLY_PERMISSIONS_REQUEST_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GeofenceConstants.REQUEST_TURN_DEVICE_LOCATION_ON) {
            (requireActivity() as? RemindersActivity)?.checkDeviceLocationSettingsAndStartGeofence(
                false,
                reminderDataItem = reminderDTO
            )
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
        private const val TAG = "SaveReminderFragment"
    }
}
