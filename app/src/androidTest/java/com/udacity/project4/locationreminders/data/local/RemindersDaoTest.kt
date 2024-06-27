package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class RemindersDaoTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var database: RemindersDatabase

    @Before
    fun initDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun insertReminderAndGetById() = runBlockingTest {
        // GIVEN a reminder has been inserted into the database
        val reminder = ReminderDTO("title", "description", "location",1.0, 1.0)
        database.reminderDao().saveReminder(reminder)

        // WHEN the reminder is requested by id
        val loaded = database.reminderDao().getReminderById(reminder.id)

        // THEN the loaded data contains the expected values
        assert(loaded?.id == reminder.id)
        assert(loaded?.title == reminder.title)
        assert(loaded?.description == reminder.description)
        assert(loaded?.location == reminder.location)
        assert(loaded?.latitude == reminder.latitude)
        assert(loaded?.longitude == reminder.longitude)
    }

    @Test
    fun insertReminders_getReminders_deleteReminders() = runBlockingTest {
        // GIVEN reminders have been inserted into the database
        val reminder1 = ReminderDTO("title1", "description1", "location1",1.0, 1.0)
        val reminder2 = ReminderDTO("title2", "description2", "location2",2.0, 2.0)
        val reminder3 = ReminderDTO("title3", "description3", "location3", 3.0, 3.0)

        database.reminderDao().saveReminder(reminder1)
        database.reminderDao().saveReminder(reminder2)
        database.reminderDao().saveReminder(reminder3)

        // WHEN reminders are requested from the database THEN they are all there
        val reminders = database.reminderDao().getReminders()
        assert(reminders.size == 3)

        // WHEN reminders are deleted from the database THEN they are empty
        database.reminderDao().deleteAllReminders()
        val remindersAfterDelete = database.reminderDao().getReminders()
        assert(remindersAfterDelete.isEmpty())
    }
}