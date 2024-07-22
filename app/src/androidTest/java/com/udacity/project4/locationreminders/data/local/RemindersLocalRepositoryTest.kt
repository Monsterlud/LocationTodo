package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class RemindersLocalRepositoryTest {

    private lateinit var database: RemindersDatabase
    private lateinit var repository: RemindersLocalRepository

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
        repository = RemindersLocalRepository(database.reminderDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun getReminder_nonExistingId_returnsError() = runTest {
        val result = repository.getReminder("1")
        assert(result is Result.Error)
        result as Result.Error
        assert(result.message == "Reminder not found!")
    }

    @Test
    fun getReminder_existingId_returnsSuccess() = runTest {
        val reminder = ReminderDTO("title", "description", "location", 0.0, 0.0)
        repository.saveReminder(reminder)
        val result = repository.getReminder(reminder.id)
        assert(result is Result.Success)
    }
}
