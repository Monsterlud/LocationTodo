package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.geofence.GeofenceConstants
import com.udacity.project4.locationreminders.geofence.GeofenceConstants.REQUEST_NOTIFICATION_ONLY_PERMISSIONS_REQUEST_CODE
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.PermissionsHandler
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {
    private lateinit var binding: FragmentSaveReminderBinding

    override val _viewModel: SaveReminderViewModel by inject()
    private val permissionsHandler: PermissionsHandler by inject()

    private lateinit var reminderDTO: ReminderDataItem




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
//            findNavController().navigate(SaveReminderFragmentDirections.toSelectLocationFragment())
            _viewModel.navigationCommand.postValue(
                NavigationCommand.To(SaveReminderFragmentDirections.toSelectLocationFragment())
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
                !permissionsHandler.isBackgroundLocationPermissionGranted(
                    requireContext(),
                    _viewModel.runningQOrLater.value!!
                )
            ) {
                permissionsHandler.requestBackgroundLocationPermission(
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
