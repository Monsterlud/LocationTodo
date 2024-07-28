package com.udacity.project4.locationreminders

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.platform.app.InstrumentationRegistry
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeAndroidRemindersLocalRepository
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.java.KoinJavaComponent

class RemindersActivityTest {

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Context
    private lateinit var viewModel: SaveReminderViewModel
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    private fun grantLocationPermission() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand(
            "pm grant ${instrumentation.targetContext.packageName} android.permission.ACCESS_FINE_LOCATION"
        )
        instrumentation.uiAutomation.executeShellCommand(
            "pm grant ${instrumentation.targetContext.packageName} android.permission.ACCESS_BACKGROUND_LOCATION"
        )
    }

    private fun grantNotificationPermission() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        instrumentation.uiAutomation.executeShellCommand(
            "pm grant ${instrumentation.targetContext.packageName} android.permission.POST_NOTIFICATIONS"
        )
    }

    @Before
    fun setup() {
        repository = KoinJavaComponent.get(FakeAndroidRemindersLocalRepository::class.java)
        appContext = ApplicationProvider.getApplicationContext<TestApplication>()
        (appContext as TestApplication?)?.setupTestModule(repository)
        viewModel = KoinJavaComponent.get(SaveReminderViewModel::class.java)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun cleanup() {
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    @Test
    fun reminderSaved_toastMessageShown() = runBlockingTest {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // GIVEN the user has location and notification permissions granted
        grantLocationPermission()
        grantNotificationPermission()

        // WHEN the user clicks to add a reminder
        Espresso.onView(ViewMatchers.withId(R.id.addReminderFAB)).perform(ViewActions.click())

        // WHEN the user adds a reminder and clicks to add a location
        Espresso.onView(ViewMatchers.withId(R.id.reminderTitle)).perform(ViewActions.typeText("Title"))
        Espresso.onView(ViewMatchers.withId(R.id.reminderDescription)).perform(ViewActions.typeText("Description"))
        Espresso.onView(ViewMatchers.withId(R.id.selectLocation)).perform(ViewActions.click())

        // WHEN the user dismisses the dialog, long clicks to add a location and clicks to save the location
        Espresso.onView(ViewMatchers.withText("OK"))
            .inRoot(RootMatchers.isDialog())
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            .perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.locationChooserMap)).perform(ViewActions.longClick())
        Espresso.onView(ViewMatchers.withId(R.id.btn_save_location)).perform(ViewActions.click())

        Espresso.onView(ViewMatchers.withId(R.id.saveReminder))
            .perform(ViewActions.click())

        // THEN verify the Toast was shown
        Espresso.onView(ViewMatchers.withText("Title")).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText("Description")).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        activityScenario.close()
    }
}