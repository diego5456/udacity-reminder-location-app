package com.udacity.project4.locationreminders.reminderslist

import android.os.Bundle
import android.view.View
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.BoundedMatcher
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.rule.GrantPermissionRule
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeAndroidDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.util.atPosition
import com.udacity.project4.utils.runningTiramisuOrLater
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify


@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest : KoinTest {

    private lateinit var dataSource: FakeAndroidDataSource

    @Rule
    @JvmField
    val grantPermissionRule: GrantPermissionRule =
        if (runningTiramisuOrLater) {
            GrantPermissionRule.grant(
                android.Manifest.permission.POST_NOTIFICATIONS,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }

    @Before
    fun setupViewModel() {
        stopKoin()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    getApplicationContext(),
                    get() as FakeAndroidDataSource
                )
            }
            single { FakeAndroidDataSource() }
        }
        startKoin { modules(listOf(myModule)) }
        dataSource = get()
    }

    @After
    fun tearDown() = runTest {
        stopKoin()
        dataSource.deleteAllReminders()

    }

    @Test
    fun fabPressed_navigatesToSaveReminder() = runTest {
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navControl = mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navControl)
        }
        onView(withId(R.id.addReminderFAB)).perform(click())

        verify(navControl).navigate(ReminderListFragmentDirections.toSaveReminder())
    }

    @Test
    fun reminders_DisplayedInUi() = runTest {
        val reminder = ReminderDTO("title", "description", "location", 1.0, 1.0)
        val reminder2 = ReminderDTO("title2", "description2", "location2", 2.0, 2.0)
        val reminder3 = ReminderDTO("title3", "description3", "location3", 3.0, 3.0)
        val reminders = listOf(reminder, reminder2, reminder3)
        dataSource.saveReminder(reminder)
        dataSource.saveReminder(reminder2)
        dataSource.saveReminder(reminder3)
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        reminders.forEachIndexed { index, reminder ->
            onView(withId(R.id.reminderssRecyclerView)).check(
                matches(
                    atPosition(
                        index,
                        hasDescendant(withText(reminder.title))
                    )
                )
            )

            onView(withId(R.id.reminderssRecyclerView)).check(
                matches(
                    atPosition(
                        index,
                        hasDescendant(withText(reminder.description))
                    )
                )
            )

            onView(withId(R.id.reminderssRecyclerView)).check(
                matches(atPosition(index, hasDescendant(withText(reminder.location))))
            )
        }

    }

    @Test
    fun reminders_noData(){
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }

    @Test
    fun reminders_receivedError_snackBarDisplayed(){
        dataSource.setReturnError(true)
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(withText("Test exception")))

    }



}

