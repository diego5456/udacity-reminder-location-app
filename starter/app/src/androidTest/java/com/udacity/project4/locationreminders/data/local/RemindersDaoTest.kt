package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    @Before
    fun setupDB(){
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDB() = database.close()

    @ExperimentalCoroutinesApi
    @Test
    fun getReminders_receiveReminders() = runTest {
        val reminder1 = ReminderDTO("Reminder1", "Description1", "Location1", 1.0, 1.0)
        val reminder2 = ReminderDTO("Reminder2", "Description2", "Location2", 2.0, 2.0)
        val reminderList = listOf(reminder1, reminder2)

        database.reminderDao().saveReminder(reminder1)
        database.reminderDao().saveReminder(reminder2)

        val result = database.reminderDao().getReminders()
        assertThat(result.size, `is`(2))
        assertThat(result, `is`(reminderList))
    }

    @ExperimentalCoroutinesApi
    @Test
    fun getReminders_receiveEmptyList() = runTest {
        val result = database.reminderDao().getReminders()
        assertThat(result.size, `is`(0))
        assertThat(result, `is`(emptyList()))
    }

    @ExperimentalCoroutinesApi
    @Test
    fun getReminderById_receiveReminderById() = runTest {
        val reminder1 = ReminderDTO("Reminder1", "Description1", "Location1", 1.0, 1.0)
        database.reminderDao().saveReminder(reminder1)
        val result = database.reminderDao().getReminderById(reminder1.id)
        assertThat(result, `is`(notNullValue()))
        assertThat(result, `is`(reminder1))
    }
    @ExperimentalCoroutinesApi
    @Test
    fun getReminderById_receiveNull() = runTest {
        val result = database.reminderDao().getReminderById("1")
        assertThat(result, `is`(nullValue()))
    }

    @ExperimentalCoroutinesApi
    @Test
    fun saveReminder_saveReminder() = runTest {
        var reminder1 = ReminderDTO("Reminder1", "Description1", "Location1", 1.0, 1.0)
        database.reminderDao().saveReminder(reminder1)
        val result = database.reminderDao().getReminderById(reminder1.id)
        assertThat(result, `is`(notNullValue()))
        assertThat(result, `is`(reminder1))
    }

    @ExperimentalCoroutinesApi
    @Test
    fun saveReminder_updateReminder() = runTest {
        val reminder1 = ReminderDTO("Reminder1", "Description1", "Location1", 1.0, 1.0)
        val reminder2 =
            ReminderDTO("Reminder2", "Description2", "Location2", 2.0, 2.0, reminder1.id)
        database.reminderDao().saveReminder(reminder1)
        val result = database.reminderDao().getReminderById(reminder1.id)
        assertThat(result, `is`(notNullValue()))
        assertThat(result, `is`(reminder1))

        database.reminderDao().saveReminder(reminder2)
        val result2 = database.reminderDao().getReminderById(reminder1.id)
        assertThat(result2, `is`(notNullValue()))
        assertThat(result2, `is`(reminder2))
    }

    @ExperimentalCoroutinesApi
    @Test
    fun deleteAllReminders_receiveEmptyList() = runTest {
        val reminder1 = ReminderDTO("Reminder1", "Description1", "Location1", 1.0, 1.0)
        val reminder2 = ReminderDTO("Reminder2", "Description2", "Location2", 2.0, 2.0)
        database.reminderDao().saveReminder(reminder1)
        database.reminderDao().saveReminder(reminder2)

        var result = database.reminderDao().getReminders()
        assertThat(result.size, `is`(2))

        database.reminderDao().deleteAllReminders()
        result = database.reminderDao().getReminders()
        assertThat(result, `is`(emptyList()))
    }
}