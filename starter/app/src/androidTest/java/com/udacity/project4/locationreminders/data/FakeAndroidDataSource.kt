package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.dto.Result.Error
import com.udacity.project4.locationreminders.data.dto.Result.Success


//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeAndroidDataSource : ReminderDataSource {
    private var shouldReturnError = false

    val reminderServiceData: LinkedHashMap<String, ReminderDTO> = LinkedHashMap()

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError) {
            return Error("Test exception")
        }
        return Success(reminderServiceData.values.toList())
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminderServiceData[reminder.id] = reminder
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError) {
            return Error("Test exception")
        }
        reminderServiceData[id]?.let { return Success(it) }
        return  Error("Reminder not found")
    }

    override suspend fun deleteAllReminders() {
        reminderServiceData.clear()
    }


}