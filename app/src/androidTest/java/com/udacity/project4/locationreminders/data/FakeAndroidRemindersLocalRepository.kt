package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.delay

class FakeAndroidRemindersLocalRepository(
    private var reminders: MutableList<ReminderDTO> = mutableListOf(),
    private var shouldReturnError: Boolean = false,
    private var checkLoading: Boolean = false
) : ReminderDataSource {
    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (checkLoading) {
            delay(100)
        }
        return if (shouldReturnError) {
            Result.Error("Error fetching reminders")
        } else {
            try {
                Result.Success(reminders.toList())
            } catch (e: Exception) {
                Result.Error(e.message ?: "An error occurred while fetching reminders")
            }
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        return if (shouldReturnError) {
            return Result.Error("Reminder not found")
        } else {
            val reminder = reminders.firstOrNull { it.id == id }
            if (reminder != null) {
                return Result.Success(reminder)
            } else {
                return Result.Error("There is no reminder with this id")
            }
        }
    }

    override suspend fun deleteAllReminders() {
        reminders.clear()
    }

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }

    fun setCheckLoading(value: Boolean) {
        checkLoading = value
    }

}