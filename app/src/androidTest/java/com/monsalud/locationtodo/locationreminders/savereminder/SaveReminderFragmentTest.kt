package com.monsalud.locationtodo.locationreminders.savereminder

import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.rule.GrantPermissionRule
import com.monsalud.locationtodo.FakeRemindersLocalRepository
import com.monsalud.locationtodo.R
import com.monsalud.locationtodo.locationreminders.data.ReminderDataSource
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@MediumTest
class SaveReminderFragmentTest {

    private lateinit var repository: ReminderDataSource

    @get:Rule
    val permissionsRule: GrantPermissionRule = GrantPermissionRule.grant(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )

    @Before
    fun setup() {
        repository = FakeRemindersLocalRepository()
    }

    @Test
    fun clickSelectLocationButton_triggersNavigationToSelectLocationFragment() = runBlockingTest {
        // GIVEN user is on SaveRemindeFragment
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
}