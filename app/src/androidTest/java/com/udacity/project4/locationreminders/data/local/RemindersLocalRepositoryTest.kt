package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@MediumTest
class RemindersLocalRepositoryTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val testingReminder = ReminderDTO(
        "title", "description", "location", 55.555, 45.555,
    )

    private lateinit var database: RemindersDatabase
    private lateinit var repository: RemindersLocalRepository

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        repository = RemindersLocalRepository(
            database.reminderDao(),
            Dispatchers.Main
        )
    }

    @After
    fun cleanUp() {
        database.close()
    }

    @Test
    fun checkSaveReminderWorks() = runBlocking {
        repository.saveReminder(testingReminder)

        val remindersList = (repository.getReminders() as Result.Success).data

        assertThat(remindersList[0].id, `is`(testingReminder.id))
        assertThat(remindersList[0].title, `is`(testingReminder.title))
        assertThat(remindersList[0].description, `is`(testingReminder.description))
        assertThat(remindersList[0].latitude, `is`(testingReminder.latitude))
        assertThat(remindersList[0].longitude, `is`(testingReminder.longitude))
        assertThat(remindersList[0].location, `is`(testingReminder.location))

    }

    @Test
    fun saveReminder_whenAllRemindersDeleted_thanReturningListIsEmpty() = runBlocking {
        repository.saveReminder(testingReminder)
        repository.deleteAllReminders()
        val remindersList = (repository.getReminders() as Result.Success).data
        assertThat(remindersList, `is`(emptyList()))
    }
}