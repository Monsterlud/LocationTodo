package com.udacity.project4.locationreminders

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityReminderDescriptionBinding
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.dto.toReminderDataItem
import com.udacity.project4.locationreminders.geofence.GeofenceConstants
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import org.koin.android.ext.android.inject


/**
 * Activity that displays the reminder details after the user clicks on the notification
 */
class ReminderDescriptionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReminderDescriptionBinding

    private val _viewModel by inject<SaveReminderViewModel>()

    companion object {
        const val EXTRA_ReminderDataItem = "EXTRA_ReminderDataItem"

        // Receive the reminder object after the user clicks on the notification
        fun newIntent(context: Context, reminderDataItem: ReminderDataItem): Intent {
            val intent = Intent(context, ReminderDescriptionActivity::class.java)
            intent.putExtra(EXTRA_ReminderDataItem, reminderDataItem)
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layoutId = R.layout.activity_reminder_description

        binding = DataBindingUtil.setContentView(this, layoutId)
        binding.lifecycleOwner = this

        val reminderDataItemFromList =
            intent.getSerializableExtra(EXTRA_ReminderDataItem) as? ReminderDataItem

        if (reminderDataItemFromList != null) {
            binding.reminderDataItem = reminderDataItemFromList
        } else {
            val reminderDataIdFromNotification = intent.getStringExtra(GeofenceConstants.EXTRA_REQUEST_ID)
            if (reminderDataIdFromNotification != null) {
                _viewModel.getReminder(reminderDataIdFromNotification).observe(this) { result ->
                    when (result) {
                        is Result.Success -> {
                            binding.reminderDataItem = result.data.toReminderDataItem()
                        }

                        is Result.Error -> {
                            Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "No reminder data provided", Toast.LENGTH_SHORT).show()
            }
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun getSupportActionBar(): ActionBar? {
        return super.getSupportActionBar()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}