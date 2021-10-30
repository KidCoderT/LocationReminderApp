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
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@SmallTest
class RemindersDaoTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    @Before
    fun initDb() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun checkSaveReminderAndGetReminderWorks() = runBlockingTest {
        // GIVEN - Insert a reminder.
        val reminder = ReminderDTO(
            "title", "description", "location", 55.555, 45.555,
        )
        database.reminderDao().saveReminder(reminder)

        // WHEN - Get the reminder by id from the database.
        val loaded = database.reminderDao().getReminderById(reminder.id)

        // THEN - The loaded data is not null and is the same reminder
        assertThat(loaded as ReminderDTO, notNullValue())
        assertThat(loaded, `is`(reminder))
    }

    @Test
    fun givenTwoReminders_whenSaved_thenLengthOfReturnIs2() = runBlockingTest {
        // GIVEN
        val reminder = ReminderDTO(
            "title", "description", "location", 55.555, 45.555,
        )
        val reminder2 = ReminderDTO(
            "title", "description", "location", 55.555, 45.555,
        )

        // WHEN
        database.reminderDao().saveReminder(reminder)
        database.reminderDao().saveReminder(reminder2)
        val listOfReminders = database.reminderDao().getReminders()

        // THAN
        assertThat(listOfReminders.size, `is`(2))
        assertThat(listOfReminders, `is`(not(emptyList())))
    }

    @Test
    fun givenTwoRemindersSaved_WhenDAOElementsClearedAndGetAllReminders_thanReturnsEmptyList() = runBlockingTest {
        // GIVEN
        val reminder = ReminderDTO(
            "title", "description", "location", 55.555, 45.555,
        )
        val reminder2 = ReminderDTO(
            "title", "description", "location", 55.555, 45.555,
        )
        database.reminderDao().saveReminder(reminder)
        database.reminderDao().saveReminder(reminder2)

        // WHEN
        database.reminderDao().deleteAllReminders()
        val listOfReminders = database.reminderDao().getReminders()

        // THAN
        assertThat(listOfReminders, `is`(emptyList()))
    }

}