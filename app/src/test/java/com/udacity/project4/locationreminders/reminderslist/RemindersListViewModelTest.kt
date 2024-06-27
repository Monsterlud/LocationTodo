package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.udacity.project4.locationreminders.LiveDataTestUtil.getOrAwaitValue
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeRemindersLocalRepository
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock


class RemindersListViewModelTest {

    private lateinit var repository: FakeRemindersLocalRepository
    private lateinit var context: Application
    private lateinit var viewModel: RemindersListViewModel

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setup() {
        context = mock(Application::class.java)
        repository = FakeRemindersLocalRepository()
        viewModel = RemindersListViewModel(context, repository)
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

        val showNoData = viewModel.showNoData.getOrAwaitValue()
        val remindersList = viewModel.remindersList.getOrAwaitValue()

        // THEN showNoData should be false & the size of remindersList should be 4
        assert(false == showNoData)
        assert(4 == remindersList.size)
    }

    @Test
    fun loadReminders_errorLoadingReminders_shouldShowError() = runTest {
        // GIVEN an error loading reminders
        repository.setReturnError(true)

        // WHEN loadReminders is called
        viewModel.loadReminders()

        val showSnackBar = viewModel.showSnackBar.getOrAwaitValue()

        // THEN showSnackBar should show the message
        assert("Error fetching reminders" == showSnackBar)
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

        val remindersList = viewModel.remindersList.getOrAwaitValue()
        val showNoData = viewModel.showNoData.getOrAwaitValue()

        // THEN showNoData should be true and the size of remindersList should be 0
        assert(remindersList.isEmpty())
        assert(true == showNoData)
    }

    @Test
    fun loadReminders_loadingReminders_shouldShowLoading() = runTest {
        // GIVEN a list of reminders and a loading state of LOADING
        populateRemindersList()
        repository.setCheckLoading(true)

        // WHEN loadReminders is called
        viewModel.loadReminders()

        val showLoading = viewModel.showLoading.getOrAwaitValue()

        // THEN showLoading should be true
        assert(true == showLoading)
    }

    @Test
    fun loadReminders_shouldShowLoading_shouldNotShowLoading() = runTest  {

        populateRemindersList()

        val testDispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(testDispatcher)

        try {
        viewModel.loadReminders()
        assert(viewModel.showLoading.value == true)
        testScheduler.advanceUntilIdle()

        assert(viewModel.showLoading.value == false)
        assert(viewModel.remindersList.value?.isNotEmpty() == true)
        assert(viewModel.showNoData.value == false)
        } finally {
            Dispatchers.resetMain()
        }
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