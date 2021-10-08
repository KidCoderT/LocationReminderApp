package com.udacity.project4.locationreminders.reminderslist

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.getOrAwaitValue
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.rule.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@Config(sdk = [Build.VERSION_CODES.O])
class RemindersListViewModelTest {

    //TODO: provide testing to the RemindersListViewModel and its live data objects

    private lateinit var fakeDataSource: FakeDataSource
    private lateinit var remindersViewModel: RemindersListViewModel

    @get:Rule
    var instantTaskRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setupTestViewModel() {
        stopKoin()
        fakeDataSource = FakeDataSource()
        remindersViewModel = RemindersListViewModel( ApplicationProvider.getApplicationContext(), fakeDataSource)
    }

    @Test
    fun checkShowNoData_returnsTrue_onNoReminders() {
        // When
        remindersViewModel.loadReminders()

        // Then
        assertThat(remindersViewModel.showNoData.value, `is`(true))
    }

    @Test
    fun checkShowNoData_returnsFalse_onHasReminders() = runBlockingTest {
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

    @Test
    fun checkShowLoadingValue_returnsTrueFalse_onloadReminderRunningAndCompleted() {
        // When
        mainCoroutineRule.pauseDispatcher() // Pause coroutines
        remindersViewModel.loadReminders()

        // Then
        assertThat(remindersViewModel.showLoading.value, `is`(true))

        // When
        mainCoroutineRule.resumeDispatcher()

        // Then
        assertThat(remindersViewModel.showLoading.value, `is`(false))
    }

}