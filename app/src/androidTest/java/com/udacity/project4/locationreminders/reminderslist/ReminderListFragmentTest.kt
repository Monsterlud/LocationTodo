package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.KoinFragmentFactory
import com.udacity.project4.R
import com.udacity.project4.locationreminders.LiveDataTestUtil.getOrAwaitValue
import com.udacity.project4.locationreminders.ReminderDescriptionActivity
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.TestApplication
import com.udacity.project4.locationreminders.data.FakeAndroidRemindersLocalRepository
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.component.KoinComponent
import org.koin.java.KoinJavaComponent.get
import org.mockito.Mockito
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@MediumTest
class ReminderListFragmentTest : KoinComponent {

    @get:Rule
    val activityRule = ActivityScenarioRule(RemindersActivity::class.java)

    @get: Rule
    val intentsTestRule = IntentsTestRule(RemindersActivity::class.java)

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: FakeAndroidRemindersLocalRepository

    @Before
    fun setup() {
        repository = get(FakeAndroidRemindersLocalRepository::class.java)
        val appContext = ApplicationProvider.getApplicationContext<TestApplication>()
        appContext.setupTestModule(repository)
    }

    @After
    fun tearDown() {
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

    @Test
    fun recyclerViewPopulated_itemClicked_navigatesToReminderDescriptionActivity() =
        runBlockingTest {

            // GIVEN there are reminders in the database for the recyclerview to display
            val reminder1 = ReminderDTO("Title 1", "Description 1", "Location 1", 1.0, 1.0)
            val reminder2 = ReminderDTO("Title 2", "Description 2", "Location 2", 2.0, 2.0)
            repository.saveReminder(reminder1)
            repository.saveReminder(reminder2)

            val savedReminders = repository.getReminders() as? Result.Success
            assert(savedReminders?.data?.size == 2) { "Reminders were not saved successfully" }

            val koinViewModel: RemindersListViewModel = get(RemindersListViewModel::class.java)

            // GIVEN the user is on the ReminderListFragment
            val scenario = launchFragmentInContainer<ReminderListFragment>(
                Bundle(),
                R.style.AppTheme,
                factory = KoinFragmentFactory()
            )
            scenario.onFragment { fragment ->
                assert(koinViewModel === fragment._viewModel) {
                    "ViewModels are not the same, Koin: $koinViewModel, Fragment: $fragment._viewModel"
                }
                fragment._viewModel.loadReminders()
                val reminders = fragment._viewModel.remindersList.getOrAwaitValue()
                assert(reminders.isNotEmpty()) { "Reminders list is empty" }
                Log.d(
                    "ReminderListFragmentTest",
                    "remindersList size: ${reminders.size}"
                )
            }

            // Verify that the reminders are displayed in the RecyclerView
            onView(withId(R.id.remindersRecyclerView)).check(
                matches(
                    hasDescendant(
                        withText(
                            reminder1.title
                        )
                    )
                )
            )
            onView(withId(R.id.remindersRecyclerView)).check(
                matches(
                    hasDescendant(
                        withText(
                            reminder2.title
                        )
                    )
                )
            )

            // WHEN user clicks on the second reminder item ("Title 2")
            onView(withText(reminder2.title)).perform(click())

            // THEN ReminderDescriptionActivity is launched
            intended(hasComponent(ReminderDescriptionActivity::class.java.name))

            // THEN the views in the detail activity are populated correctly
            onView(withId(R.id.tvTitleText)).check(matches(withText(reminder2.title)))
            onView(withId(R.id.tvDescriptionText)).check(matches(withText(reminder2.description)))
            onView(withId(R.id.tvLocationText)).check(matches(withText(reminder2.location)))
            onView(withId(R.id.tvLatitudeText)).check(matches(withText(reminder2.latitude.toString())))
            onView(withId(R.id.tvLongitudeText)).check(matches(withText(reminder2.longitude.toString())))
        }
}