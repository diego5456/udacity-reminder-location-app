package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.FakeAndroidDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()
    private lateinit var database: RemindersDatabase
    private lateinit var localDataSource: RemindersLocalRepository

    @Before
    fun initDb(){
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        localDataSource = RemindersLocalRepository(database.reminderDao(), Dispatchers.Main)
    }

    @After
    fun closeDb() = database.close()

    @ExperimentalCoroutinesApi
    @Test
    fun getReminders_receiveReminders() = runBlocking {
        val reminder1 = ReminderDTO("Title1", "Description1", "Location1", 1.0, 1.0)
        val reminder2 = ReminderDTO("Title2", "Description2", "Location2", 2.0, 2.0)
        val reminder3 = ReminderDTO("Title3", "Description3", "Location3", 3.0, 3.0)

        localDataSource.saveReminder(reminder1)
        localDataSource.saveReminder(reminder2)
        localDataSource.saveReminder(reminder3)

        var result = localDataSource.getReminders()
        assertThat(result, `is`(Result.Success(listOf(reminder1, reminder2, reminder3))))
        result as Result.Success
        assertThat(result.data.size, `is`(3))
    }

    @ExperimentalCoroutinesApi
    @Test
    fun getReminders_receiveEmptyList() = runBlocking {
        val result = localDataSource.getReminders()
        assertThat(result, `is`(Result.Success(emptyList())))
        result as Result.Success
        assertThat(result.data.size, `is`(0))
    }

    @ExperimentalCoroutinesApi
    @Test
    fun saveReminder_saveReminder() = runBlocking {
        val reminder1 = ReminderDTO("Title1", "Description1", "Location1", 1.0, 1.0)
        localDataSource.saveReminder(reminder1)
        val result = localDataSource.getReminder(reminder1.id)
        assertThat(result, `is`(Result.Success(reminder1)))
        result as Result.Success
        assertThat(result.data, `is`(reminder1))
    }

    @ExperimentalCoroutinesApi
    @Test
    fun getReminder_receiveReminderById() = runBlocking {
        val reminder1 = ReminderDTO("Title1", "Description1", "Location1", 1.0, 1.0)
        localDataSource.saveReminder(reminder1)
        val result = localDataSource.getReminder(reminder1.id)
        assertThat(result, `is`(Result.Success(reminder1)))
        result as Result.Success
        assertThat(result.data, `is`(reminder1))
    }

    @ExperimentalCoroutinesApi
    @Test
    fun getReminder_recieveNull() = runBlocking {
        val reminder1 = ReminderDTO("Title1", "Description1", "Location1", 1.0, 1.0)
        val result = localDataSource.getReminder(reminder1.id)
        assertThat(result, `is`(Result.Error("Reminder not found!")))
        result as Result.Error
        assertThat(result.statusCode, `is`(nullValue()))
    }

    @ExperimentalCoroutinesApi
    @Test
    fun deleteAllReminders_receiveEmptyList() = runBlocking {
        val reminder1 = ReminderDTO("Title1", "Description1", "Location1", 1.0, 1.0)
        val reminder2 = ReminderDTO("Title2", "Description2", "Location2", 2.0, 2.0)
        val reminder3 = ReminderDTO("Title3", "Description3", "Location3", 3.0, 3.0)
        localDataSource.saveReminder(reminder1)
        localDataSource.saveReminder(reminder2)
        localDataSource.saveReminder(reminder3)

        var result = localDataSource.getReminders()
        assertThat(result, `is`(Result.Success(listOf(reminder1, reminder2, reminder3))))
        result as Result.Success
        assertThat(result.data.size, `is`(3))

        localDataSource.deleteAllReminders()
        result = localDataSource.getReminders()
        assertThat(result, `is`(Result.Success(emptyList())))
        result as Result.Success
        assertThat(result.data.size, `is`(0))

    }


}