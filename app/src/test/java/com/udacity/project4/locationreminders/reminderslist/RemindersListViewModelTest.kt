package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    //TODO: provide testing to the RemindersListViewModel and its live data objects

    private lateinit var fakeDataSource: FakeDataSource
    private lateinit var remindersViewModel: RemindersListViewModel

    @get:Rule
    var instantTaskRule = InstantTaskExecutorRule()

    @Before
    fun setupTestViewModel() {
        fakeDataSource = FakeDataSource()
        remindersViewModel = RemindersListViewModel( ApplicationProvider.getApplicationContext(), fakeDataSource)
    }

    @Test
    fun getShowNoData_noReminder_returnsTrue() {
        // When
        remindersViewModel.loadReminders()

        // Then
        assertThat(remindersViewModel.showNoData.value, `is`(true))
    }

    @Test
    suspend fun getShowNoData_hasReminder_returnsFalse() {
        // Given
        fakeDataSource.saveReminder(ReminderDTO(
            "title",
            "Description",
            "location",
            55.55,
            45.55
        ))

        // When
        remindersViewModel.loadReminders()

        // Then
        assertThat(remindersViewModel.showNoData.value, `is`(false))
    }

}