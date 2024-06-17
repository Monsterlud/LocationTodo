package com.monsalud.locationtodo.locationreminders.savereminder

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.monsalud.locationtodo.R
import com.monsalud.locationtodo.base.BaseFragment
import com.monsalud.locationtodo.base.NavigationCommand
import com.monsalud.locationtodo.databinding.FragmentSaveReminderBinding
import com.monsalud.locationtodo.locationreminders.RemindersActivity
import com.monsalud.locationtodo.locationreminders.geofence.GeofenceConstants
import com.monsalud.locationtodo.locationreminders.reminderslist.ReminderDataItem
import com.monsalud.locationtodo.utils.setDisplayHomeAsUpEnabled
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

private const val TAG = "SaveReminderFragment"

class SaveReminderFragment : BaseFragment() {
    private lateinit var binding: FragmentSaveReminderBinding

    override val _viewModel: SaveReminderViewModel by inject()

    lateinit var reminderDTO: ReminderDataItem

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

        binding.saveReminder.setOnClickListener {
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
                    (requireActivity() as? RemindersActivity)?.checkPermissionsAndStartGeofencing(
                        reminderDTO,
                        object : RemindersActivity.GeofenceSetupListener {
                            override fun onGeofenceAdded(success: Boolean) {
                                if (success) {
                                    _viewModel.setReminderSaved(false)
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
                    )

                }
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GeofenceConstants.REQUEST_TURN_DEVICE_LOCATION_ON) {
            (requireActivity() as? RemindersActivity)?.checkDeviceLocationSettingsAndStartGeofence(reminderDataItem = reminderDTO)
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
