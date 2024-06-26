package com.monsalud.locationtodo.locationreminders.savereminder

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.monsalud.locationtodo.R
import com.monsalud.locationtodo.locationreminders.LiveDataTestUtil.getOrAwaitValue
import com.monsalud.locationtodo.locationreminders.data.FakeAndroidRemindersLocalRepository
import com.monsalud.locationtodo.locationreminders.data.ReminderDataSource
import com.monsalud.locationtodo.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@MediumTest
class SaveReminderFragmentTest {

    private lateinit var repository: ReminderDataSource

//    @get:Rule
//    val permissionsRule: GrantPermissionRule = GrantPermissionRule.grant(
//        android.Manifest.permission.ACCESS_FINE_LOCATION,
//        android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
//    )

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

    @Before
    fun setup() {
        repository = FakeAndroidRemindersLocalRepository()
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