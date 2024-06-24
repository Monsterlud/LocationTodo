package com.monsalud.locationtodo

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.Room
import com.monsalud.locationtodo.locationreminders.data.local.RemindersDatabase
import com.monsalud.locationtodo.locationreminders.data.local.RemindersLocalRepository

object ServiceLocator {
    private var database: RemindersDatabase? = null

    @Volatile
    var remindersLocalRepository: RemindersLocalRepository? = null
    @VisibleForTesting set

    fun provideRemindersLocalRepository(context: Context): RemindersLocalRepository {
        synchronized(this) {
            return remindersLocalRepository ?: createRemindersLocalRepository(context)
        }
    }

    private fun createRemindersLocalRepository(context: Context): RemindersLocalRepository {

        val newRepo = RemindersLocalRepository(createRemindersDatabase(context).reminderDao())
        remindersLocalRepository = newRepo
        return newRepo
    }

    private fun createRemindersDatabase(context: Context): RemindersDatabase {
        val result = Room.databaseBuilder(
            context.applicationContext,
            RemindersDatabase::class.java, "locationReminders.db"

        ).build()
        database = result
        return result
    }
}