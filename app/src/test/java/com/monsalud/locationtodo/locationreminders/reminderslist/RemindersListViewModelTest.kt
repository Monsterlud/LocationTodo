package com.monsalud.locationtodo.locationreminders.reminderslist

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.monsalud.locationtodo.locationreminders.data.FakeRemindersLocalRepository
import com.monsalud.locationtodo.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock


class RemindersListViewModelTest {

    lateinit var repository: FakeRemindersLocalRepository
    lateinit var context: Application

    private lateinit var viewModel: RemindersListViewModel

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = TestCoroutineDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        context = mock(Application::class.java)
        repository = FakeRemindersLocalRepository()
        viewModel = RemindersListViewModel(context, repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun loadReminders_reminderListEmpty_shouldShowNoData() = runTest {
        // GIVEN no reminders (this is the initial state)

        // WHEN loadReminders is called
        viewModel.loadReminders()

        // THEN showNoData should be true
        assert(viewModel.showNoData.value == true)
    }

    @Test
    fun loadReminders_reminderListNotEmpty_shouldShowReminders() = runTest {
        // GIVEN a list of reminders
        populateRemindersList()

        // WHEN loadReminders is called
        viewModel.loadReminders()

        // THEN showNoData should be false & the size of remindersList should be 4
        assert(viewModel.showNoData.value == false)
        assert(viewModel.remindersList.value?.size == 4)
    }

    @Test
    fun loadReminders_errorLoadingReminders_shouldShowError() = runTest {
        // GIVEN an error loading reminders
        repository.setReturnError(true)

        // WHEN loadReminders is called
        viewModel.loadReminders()

        // THEN showSnackBar should show the message
        assert("Error fetching reminders" == viewModel.showSnackBar.value)
    }

    @Test
    fun loadReminders_deletingAllReminders_shouldShowNoData() = runTest {
        // GIVEN a list of reminders
        populateRemindersList()

        // WHEN loadReminders is called there are 4 reminders
        viewModel.loadReminders()
        assert(viewModel.remindersList.value?.size == 4)

        // WHEN deletingAllReminders is called there are no reminders (then reload reminders)
        viewModel.deleteAllReminders()
        viewModel.loadReminders()

        // THEN showNoData should be true and the size of remindersList should be 0
        assert(viewModel.remindersList.value?.size == 0)
        assert(viewModel.showNoData.value == true)
    }

    @Test
    fun loadReminders_loadingReminders_shouldShowLoading() = runTest {
        // GIVEN a list of reminders and a loading state of LOADING
        populateRemindersList()
        repository.setCheckLoading(true)

        // WHEN loadReminders is called
        viewModel.loadReminders()

        // THEN showLoading should be true
        val showLoading = viewModel.showLoading.value!!
        assert(showLoading)
    }

    private suspend fun populateRemindersList() {
        repository.saveReminder(
            ReminderDTO(
                title = "Title 1",
                description = "Description 1",
                location = "Location 1",
                latitude = 1.0,
                longitude = 1.0
            )
        )
        repository.saveReminder(
            ReminderDTO(
                title = "Title 2",
                description = "Description 2",
                location = "Location 2",
                latitude = 2.0,
                longitude = 2.0
            )
        )
        repository.saveReminder(
            ReminderDTO(
                title = "Title 3",
                description = "Description 3",
                location = "Location 3",
                latitude = 3.0,
                longitude = 3.0
            )
        )
        repository.saveReminder(
            ReminderDTO(
                title = "Title 4",
                description = "Description 4",
                location = "Location 4",
                latitude = 4.0,
                longitude = 4.0
            )
        )
    }
}