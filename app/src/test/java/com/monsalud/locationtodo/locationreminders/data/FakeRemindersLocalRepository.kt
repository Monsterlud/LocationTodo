package com.monsalud.locationtodo.locationreminders.data

import com.monsalud.locationtodo.locationreminders.data.dto.ReminderDTO
import com.monsalud.locationtodo.locationreminders.data.dto.Result

class FakeRemindersLocalRepository(
    var reminders: MutableList<ReminderDTO> = mutableListOf()
) : ReminderDataSource {
    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return try {
            Result.Success(reminders.toList())
        } catch (e: Exception) {
            Result.Error(e.message ?: "An error occurred while fetching reminders")
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
}