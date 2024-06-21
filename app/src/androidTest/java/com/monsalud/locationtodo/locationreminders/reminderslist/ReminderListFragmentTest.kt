package com.monsalud.locationtodo.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.monsalud.locationtodo.R
import com.monsalud.locationtodo.locationreminders.RemindersActivity
import com.monsalud.locationtodo.locationreminders.data.FakeRemindersLocalRepository
import com.monsalud.locationtodo.locationreminders.data.ReminderDataSource
import com.monsalud.locationtodo.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@MediumTest
class ReminderListFragmentTest {

    private lateinit var application: Application
    private lateinit var repository: ReminderDataSource
    private lateinit var viewModel: RemindersListViewModel

    @get:Rule
    val activityRule = ActivityScenarioRule(RemindersActivity::class.java)

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        repository = FakeRemindersLocalRepository()
        viewModel = RemindersListViewModel(application, repository)
    }

    @Test
    fun clickFAB_triggersNavigationToSaveRemindersFragment() = runBlockingTest {
        // GIVEN user is on ReminderListFragment
        val scenario = launchFragmentInContainer<ReminderListFragment>(
            Bundle(),
            R.style.AppTheme
        )

        // WHEN  user clicks on FAB to add a reminder
        val navController = Mockito.mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }
        onView(withId(R.id.addReminderFAB)).perform(click())

        // THEN navigation is triggered to SaveReminderFragment
        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminderFragment())
    }

    private suspend fun populateReminders() {
        repository.saveReminder(ReminderDTO(
            "title",
            "description",
            "location",
            1.0,
            1.0,
            "1"
        ))
        repository.saveReminder(ReminderDTO(
            "title2",
            "description2",
            "location2",
            2.0,
            2.0,
            "2"
        ))
    }
}