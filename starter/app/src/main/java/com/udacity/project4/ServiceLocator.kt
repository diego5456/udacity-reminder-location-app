package com.udacity.project4

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.Room
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.RemindersDatabase
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import kotlinx.coroutines.runBlocking

object ServiceLocator {
    private val lock = Any()
    private var database: RemindersDatabase? = null

    @Volatile
    var remindersRepository: ReminderDataSource? = null
        @VisibleForTesting set

    fun provideReminderLocalRepository(context: Context): ReminderDataSource{
        synchronized(this){
            return remindersRepository?: createReminderLocalRepository(context)
        }
    }

    private fun createReminderLocalRepository(context: Context): RemindersLocalRepository{
        val newRepo = RemindersLocalRepository(createDatabase(context).reminderDao())
        remindersRepository = newRepo
        return newRepo
    }

    private fun createDatabase(context: Context): RemindersDatabase{
        val result =Room.databaseBuilder(
            context.applicationContext,
            RemindersDatabase::class.java,
            "Reminders.db"

        ).build()
        database = result
        return result
    }

    @VisibleForTesting
    fun resetRepository(){
        synchronized(lock){
            database?.apply {
                clearAllTables()
                close()
            }
        }
        database = null
        remindersRepository = null
    }
}