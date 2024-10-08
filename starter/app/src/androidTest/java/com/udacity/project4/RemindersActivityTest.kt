package com.udacity.project4

import android.Manifest
import android.app.Application
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.Root
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isClickable
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.GrantPermissionRule
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.material.internal.ContextUtils.getActivity
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.locationreminders.savereminder.selectreminderlocation.SelectLocationFragment
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.atPosition
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import com.udacity.project4.utils.runningTiramisuOrLater
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Description
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.get
import org.koin.test.inject

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    KoinTest {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private var dataBindingIdlingResource = DataBindingIdlingResource()
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var selectLocationFragment: SelectLocationFragment

    @Rule
    @JvmField
    val grantPermissionRule: GrantPermissionRule =
        if (runningTiramisuOrLater) {
            GrantPermissionRule.grant(
                Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION

            )
        } else {
            GrantPermissionRule.grant(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }


    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }

        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }


//    TODO: add End to End testing to the app

    @Test
    fun createReminder() = runBlocking {

        val scenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(scenario)

        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
        onView(withId(R.id.addReminderFAB)).perform(click())

        onView(withId(R.id.reminderTitle)).check(matches(isDisplayed()))
        onView(withId(R.id.reminderDescription)).check(matches(isDisplayed()))
        onView(withId(R.id.selectLocation)).check(matches(isDisplayed()))
        onView(withId(R.id.saveReminder)).check(matches(isDisplayed()))

        onView(withId(R.id.reminderTitle)).perform(typeText("TITLE1"))
        onView(withId(R.id.reminderDescription)).perform(typeText("DESC1"))
        onView(withId(R.id.selectLocation)).perform(click())
        onView(withId(R.id.selector_map)).check(matches(isDisplayed()))

        scenario.onActivity { activity ->
            val navHostFragment =
                activity.supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            selectLocationFragment =
                navHostFragment?.childFragmentManager?.primaryNavigationFragment as SelectLocationFragment
            mapFragment =
                (selectLocationFragment.childFragmentManager.findFragmentById(R.id.selector_map) as? SupportMapFragment)!!


        }

        val map = onView(withId(R.id.selector_map))
        waitForMarkerIsCreatedWithTimeout(map, 10000L, 100L)
        onView(withId(R.id.selector_map)).perform(click())
        onView(withId(R.id.save_btn)).perform(click())

        onView(withId(R.id.reminderTitle)).check(matches(withText("TITLE1")))
        onView(withId(R.id.reminderDescription)).check(matches(withText("DESC1")))
        onView(withId(R.id.selectedLocation)).check(matches(withText("Custom Location")))
        onView(withId(R.id.saveReminder)).perform(click())

        onView(withText(R.string.reminder_saved))
            .inRoot(withDecorView(not(`is`(getActivity(appContext)?.window?.decorView))))
            .check(matches(isDisplayed()))

        onView(withId(R.id.reminderssRecyclerView)).check(matches(atPosition(0, hasDescendant(withText("TITLE1")))))
        onView(withId(R.id.reminderssRecyclerView)).check(matches(atPosition(0, hasDescendant(withText("DESC1")))))
        onView(withId(R.id.reminderssRecyclerView)).check(matches(atPosition(0, hasDescendant(withText("Custom Location")))))

        scenario.close()
    }

    private suspend fun waitForMarkerIsCreatedWithTimeout(
        viewInteraction: ViewInteraction,
        timeout: Long = 5000L,
        interval: Long = 100L
    ) {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + timeout
        while (System.currentTimeMillis() < endTime) {
            try {
                viewInteraction.check(matches(isDisplayed()))
                    .check(matches(isClickable()))
                    .check(matches(isEnabled()))
                    .perform(click())
                if (selectLocationFragment.currentMarker == null) {
                    throw Exception("Marker yet to be created")
                }
                return
            } catch (e: Exception) {
                Log.i("waitForMapToLoad", "Map not ready yet. Waiting...")
            }
            delay(interval)
        }
    }
}