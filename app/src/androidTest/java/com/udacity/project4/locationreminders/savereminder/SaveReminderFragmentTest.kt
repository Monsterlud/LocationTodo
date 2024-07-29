package com.udacity.project4.locationreminders.savereminder

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.udacity.project4.MainCoroutineRule
import com.udacity.project4.R
import com.udacity.project4.locationreminders.LiveDataTestUtil.getOrAwaitValue
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.TestApplication
import com.udacity.project4.locationreminders.ToastIdlingResourceHelper
import com.udacity.project4.locationreminders.data.FakeAndroidRemindersLocalRepository
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.java.KoinJavaComponent.get
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@LargeTest
class SaveReminderFragmentTest {

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Context
    private lateinit var viewModel: SaveReminderViewModel
    private lateinit var toastIdlingResourceHelper: ToastIdlingResourceHelper

    private fun grantLocationPermission() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand(
            "pm grant ${instrumentation.targetContext.packageName} android.permission.ACCESS_FINE_LOCATION"
        )
        instrumentation.uiAutomation.executeShellCommand(
            "pm grant ${instrumentation.targetContext.packageName} android.permission.ACCESS_BACKGROUND_LOCATION"
        )
    }

    private fun revokeLocationPermission() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand(
            "pm revoke ${instrumentation.targetContext.packageName} android.permission.ACCESS_FINE_LOCATION"
        )
        instrumentation.uiAutomation.executeShellCommand(
            "pm revoke ${instrumentation.targetContext.packageName} android.permission.ACCESS_BACKGROUND_LOCATION"
        )
    }

    private fun grantNotificationPermission() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand(
            "pm grant ${instrumentation.targetContext.packageName} android.permission.POST_NOTIFICATIONS"
        )
    }

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    val activityRule = ActivityScenarioRule(RemindersActivity::class.java)

    @Before
    fun setup() {
        repository = get(FakeAndroidRemindersLocalRepository::class.java)
        appContext = ApplicationProvider.getApplicationContext<TestApplication>()
        (appContext as TestApplication?)?.setupTestModule(repository)
        viewModel = get(SaveReminderViewModel::class.java)
        toastIdlingResourceHelper = ToastIdlingResourceHelper(viewModel)
        IdlingRegistry.getInstance().register(toastIdlingResourceHelper.toastIdlingResource)
    }

    @After
    fun cleanup() {
        IdlingRegistry.getInstance().unregister(toastIdlingResourceHelper.toastIdlingResource)
    }

    @Test
    fun clickSelectLocationButton_triggersNavigationToSelectLocationFragment() = runBlockingTest {
        // GIVEN user is on SaveRemindeFragment with location permissions
        grantLocationPermission()
        val scenario = launchFragmentInContainer<SaveReminderFragment>(Bundle(), R.style.AppTheme)

        // WHEN user clicks on select location button
        val navController = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }
        onView(withId(R.id.selectLocation)).perform(click())

        // THEN navigation is triggered to SelectLocationFragment
        verify(navController).navigate(SaveReminderFragmentDirections.toSelectLocationFragment())
    }

    @Test
    fun noLocationPermissionsAndNoLocation_snackbarShown() {
        // GIVEN user user is on SaveReminderFragment and does not have location permissions
        revokeLocationPermission()
        val scenario = launchFragmentInContainer<SaveReminderFragment>(Bundle(), R.style.AppTheme)
        val incompleteReminder = ReminderDataItem("Title", "Description", "", 0.0, 0.0)

        // WHEN user clicks on Reminder Location Button
        scenario.onFragment {
            it._viewModel.validateAndSaveReminder(incompleteReminder)

            val message = it._viewModel.showSnackBarInt.getOrAwaitValue()
            Log.d("SaveReminderFragmentTest", "message: $message")
        }

        // THEN snackbar is shown with message "You need to grant location permission..."

        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText("Please select location")))
    }
}
