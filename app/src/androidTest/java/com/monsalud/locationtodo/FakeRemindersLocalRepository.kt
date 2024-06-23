package com.monsalud.locationtodo

import com.monsalud.locationtodo.locationreminders.data.ReminderDataSource
import com.monsalud.locationtodo.locationreminders.data.dto.ReminderDTO
import com.monsalud.locationtodo.locationreminders.data.dto.Result
import kotlinx.coroutines.delay

class FakeRemindersLocalRepository(
    private var reminders: MutableList<ReminderDTO> = mutableListOf(),
    private var shouldReturnError: Boolean = false,
    var check_loading: Boolean = false
) : ReminderDataSource {
    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (check_loading) {
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
        for (reminder in reminders) {
            if (reminder.id == id) {
                return Result.Success(reminder)
            }
        }
        return Result.Error("Reminder not found")
    }

    override suspend fun deleteAllReminders() {
        reminders.clear()
    }

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }

    fun setCheckLoading(value: Boolean) {
        check_loading = value
    }

}