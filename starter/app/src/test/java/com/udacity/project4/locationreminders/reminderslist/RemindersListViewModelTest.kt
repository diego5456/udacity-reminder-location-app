package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
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
import org.koin.test.inject

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest : KoinTest {
    @ExperimentalCoroutinesApi
    @get:Rule
    val testDispatcher = MainCoroutineRule()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var dataSource: FakeDataSource
    val reminderListViewModel: RemindersListViewModel by inject<RemindersListViewModel>()


    val reminder1 = ReminderDTO("Title1", "Description1", "Location1", 1.0, 2.0)
    val reminder2 = ReminderDTO("Title2", "Description2", "Location2", 3.0, 4.0)
    val reminder3 = ReminderDTO("Title3", "Description3", "Location3", 5.0, 6.0)

    @Before
    fun setupViewModel() = runTest {
        stopKoin()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    ApplicationProvider.getApplicationContext(),
                    get() as FakeDataSource
                )
            }
            single { FakeDataSource() }
        }
        startKoin { modules(listOf(myModule)) }
        dataSource = get()

        dataSource.saveReminder(reminder1)
        dataSource.saveReminder(reminder2)
        dataSource.saveReminder(reminder3)


    }

    @After
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    @Test
    fun loadReminders_remindersListReturned() = runTest {
        val remindersDTOList = listOf(reminder1, reminder2, reminder3)
        val reminderList = ArrayList<ReminderDataItem>()
        remindersDTOList.map {
            reminderList.add(
                ReminderDataItem(
                    it.title,
                    it.description,
                    it.location,
                    it.latitude,
                    it.longitude,
                    it.id
                )
            )
        }
        reminderListViewModel.loadReminders()
        assertThat(reminderListViewModel.remindersList.value?.size, `is`(3))
        assertThat(reminderListViewModel.remindersList.value, `is`(reminderList))

    }

    @Test
    fun loadReminders_showLoading() = runTest {
        Dispatchers.setMain(StandardTestDispatcher())
        reminderListViewModel.loadReminders()
        assertThat(reminderListViewModel.showLoading.getOrAwaitValue(), `is`(true))
        advanceUntilIdle()
        assertThat(reminderListViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun loadReminders_showNoData() = runTest {
        dataSource.deleteAllReminders()
        reminderListViewModel.loadReminders()
        assertThat(reminderListViewModel.showNoData.getOrAwaitValue(), `is`(true))
    }

    @Test
    fun loadReminders_remindersListEmpty() = runTest {
        dataSource.deleteAllReminders()
        reminderListViewModel.loadReminders()
        assertThat(reminderListViewModel.remindersList.value?.size, `is`(0))
    }

    @Test
    fun loadReminders_error() = runTest {
        dataSource.setReturnError(true)
        reminderListViewModel.loadReminders()
        assertThat(reminderListViewModel.showSnackBar.value, `is`("Test exception"))
    }

    @Test
    fun loaadReminders_updateList() = runTest{
        val reminder4 = ReminderDTO("Title4", "Description4", "Location4", 1.0, 2.0)
        reminderListViewModel.loadReminders()
        assertThat(reminderListViewModel.remindersList.value?.size, `is`(3))
        dataSource.reminderServiceData[reminder4.id] = reminder4
        reminderListViewModel.loadReminders()
        assertThat(reminderListViewModel.remindersList.value?.size, `is`(4))


    }


}