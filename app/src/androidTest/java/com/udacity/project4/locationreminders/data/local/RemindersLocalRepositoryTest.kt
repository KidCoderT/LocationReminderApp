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
import org.hamcrest.CoreMatchers.not
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

        val remindersList = repository.getReminders()
        remindersList as Result.Success

        assertThat(remindersList.data[0].id, `is`(testingReminder.id))
        assertThat(remindersList.data[0].title, `is`(testingReminder.title))
        assertThat(remindersList.data[0].description, `is`(testingReminder.description))
        assertThat(remindersList.data[0].latitude, `is`(testingReminder.latitude))
        assertThat(remindersList.data[0].longitude, `is`(testingReminder.longitude))
        assertThat(remindersList.data[0].location, `is`(testingReminder.location))

    }

    @Test
    fun saveReminder_whenAllRemindersDeleted_thanReturningListIsEmpty() = runBlocking {
        repository.saveReminder(testingReminder)
        val reminderListWithData = repository.getReminders() as Result.Success
        repository.deleteAllReminders()
        val remindersListAfterClear = repository.getReminders() as Result.Success

        assertThat(reminderListWithData.data.size, `is`(1))
        assertThat(remindersListAfterClear.data, `is`(emptyList()))
    }

    @Test
    fun getReminder_whenIDIncorrect_returnsError() = runBlocking {
        val gotData = repository.getReminder("1")

        assertThat(gotData, `is`(not(Result.Success(gotData))))
        assertThat(gotData, `is`(Result.Error("Reminder not found!")))
    }
}