package com.udacity.project4.locationreminders.data.local

import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever


@ExperimentalCoroutinesApi
class RemindersLocalRepositoryTest {

    private lateinit var repository: RemindersLocalRepository

    val dao = mock(RemindersDao::class.java)

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setup() {
        repository = RemindersLocalRepository(dao)
    }

    @Test
    fun getReminder_nonExistingId_returnsError() = runTest {

        whenever(dao.getReminderById(anyString())).thenReturn(null)

        val result = repository.getReminder("1")
        assert(result is Result.Error)
        result as Result.Error
        assert(result.message == "Reminder not found!")
    }
}