package com.udacity.project4.locationreminders.savereminder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.navigation.fragment.findNavController
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.PermissionsHandler
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
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
            _viewModel.navigationCommand.postValue(
                NavigationCommand.To(SaveReminderFragmentDirections.toSelectLocationFragment())
            )
        }

        _viewModel.reminderSaved.observe(viewLifecycleOwner) {
            if (it) {
                (activity as? RemindersActivity)?.checkDeviceLocationSettingsAndStartGeofence(
                    true,
                    reminderDTO
                )
                _viewModel.setReminderSaved(false)
            }
        }

        binding.saveReminder.setOnClickListener {
            reminderDTO = ReminderDataItem(
                binding.reminderTitle.text.toString(),
                binding.reminderDescription.text.toString(),
                _viewModel.reminderSelectedLocationStr.value,
                _viewModel.latitude.value,
                _viewModel.longitude.value
            )
            checkPermissionsAndSaveReminder()
        }
    }

    fun checkPermissionsAndSaveReminder() {
        val hasNotificationPermission = permissionsHandler.isNotificationsPermissionGranted(
            requireContext(),
            _viewModel.runningTiramisuOrLater.value!!
        )
        val hasBackgroundLocationPermission =
            permissionsHandler.isBackgroundLocationPermissionGranted(
                requireContext(),
                _viewModel.runningQOrLater.value!!
            )

        when {
            !hasNotificationPermission || !hasBackgroundLocationPermission -> {
                permissionsHandler.requestNotificationAndBackgroundLocationPermission(
                    activity = requireActivity(),
                    runningTiramisuOrLater = _viewModel.runningTiramisuOrLater.value!!,
                    hasNotificationPermission = hasNotificationPermission,
                    hasBackgroundLocationPermission = hasBackgroundLocationPermission
                )
            }
            _viewModel.validateEnteredData(reminderDTO) -> {
                _viewModel.saveReminder(reminderDTO)
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
        private const val TAG = "SaveReminderFragment"
    }
}
