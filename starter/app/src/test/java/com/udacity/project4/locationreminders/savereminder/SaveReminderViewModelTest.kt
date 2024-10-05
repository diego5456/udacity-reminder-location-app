package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
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

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest: KoinTest {
    @ExperimentalCoroutinesApi
    @get:Rule
    val testDispatcher = MainCoroutineRule()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var dataSource: FakeDataSource
    private lateinit var reminderDataItem: ReminderDataItem
    val viewModel: SaveReminderViewModel by inject<SaveReminderViewModel>()

    @Before
    fun setupViewModel() {
        stopKoin()
        val myModule = module {
            viewModel {
                SaveReminderViewModel(
                    ApplicationProvider.getApplicationContext(),
                    get() as FakeDataSource
                )
            }
            single { FakeDataSource() }
        }
        startKoin { modules(listOf(myModule)) }
        dataSource = get()
        reminderDataItem = ReminderDataItem("Title", "Description", "Location", 1.0, 2.0)

    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun onClear_clearsReminderFields() {
        viewModel.reminderTitle.value = "Title"
        viewModel.reminderDescription.value = "Description"
        viewModel.reminderSelectedLocationStr.value = "Location"
        viewModel.latitude.value = 1.0
        viewModel.longitude.value = 2.0
        viewModel.selectedPOI.value = PointOfInterest(LatLng(1.0, 2.0), "place_id", "name")
        viewModel.onClear()
        assertThat(viewModel.reminderTitle.value, `is`(nullValue()))
        assertThat(viewModel.reminderDescription.value, `is`(nullValue()))
        assertThat(viewModel.reminderSelectedLocationStr.value, `is`(nullValue()))
        assertThat(viewModel.latitude.value, `is`(nullValue()))
        assertThat(viewModel.longitude.value, `is`(nullValue()))
        assertThat(viewModel.selectedPOI.value, `is`(nullValue()))
    }

    @Test
    fun validateEnteredData_missingTitle_returnsFalseAndSnackbarSet() {
        reminderDataItem.title = ""
        val result = viewModel.validateEnteredData(reminderDataItem)
        assertThat(result, `is`(false))
        assertThat(viewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_enter_title))
    }

    @Test
    fun validateEnteredData_missingLocation_returnsFalseAndSnackbarSet() {
        reminderDataItem.location = ""
        val result = viewModel.validateEnteredData(reminderDataItem)
        assertThat(result, `is`(false))
        assertThat(viewModel.showSnackBarInt.getOrAwaitValue(), `is`(R.string.err_select_location))
    }

    @Test
    fun validateEnteredData_returnsTrue() {
        val result = viewModel.validateEnteredData(reminderDataItem)
        assertThat(result, `is`(true))
    }

    @Test
    fun saveReminder_setsLiveDataValues() = runTest {
        Dispatchers.setMain(StandardTestDispatcher())
        viewModel.validateAndSaveReminder(reminderDataItem)
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(true))
        advanceUntilIdle()
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(false))
        assertThat(viewModel.showToast.getOrAwaitValue(), `is`("Reminder Saved !"))
    }

    @Test
    fun saveReminder_savesReminderToDataSource() = runTest {
        viewModel.validateAndSaveReminder(reminderDataItem)
        val reminderDTO = ReminderDTO(
            reminderDataItem.title,
            reminderDataItem.description,
            reminderDataItem.location,
            reminderDataItem.latitude,
            reminderDataItem.longitude,
            reminderDataItem.id
        )
        assertThat(dataSource.reminderServiceData.size, `is`(1))
        assertThat(dataSource.reminderServiceData.get(reminderDTO.id), `is`(reminderDTO))
    }

    @Test
    fun saveReminder_invalidTitle() = runTest {
        reminderDataItem.title = ""
        viewModel.validateAndSaveReminder(reminderDataItem)
        assertThat(dataSource.reminderServiceData.size, `is`(0))
        assertThat(dataSource.reminderServiceData.get(reminderDataItem.id), `is`(nullValue()))
    }

    @Test
    fun saveReminder_invalidLocation() = runTest {
        reminderDataItem.title = ""
        viewModel.validateAndSaveReminder(reminderDataItem)
        assertThat(dataSource.reminderServiceData.size, `is`(0))
        assertThat(dataSource.reminderServiceData.get(reminderDataItem.id), `is`(nullValue()))
    }
}