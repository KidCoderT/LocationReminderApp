package com.udacity.project4.locationreminders.reminderslist

import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.rule.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is.`is`
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

    private lateinit var fakeDataSource: FakeDataSource
    private lateinit var remindersListViewModel: RemindersListViewModel

    @get:Rule
    var instantTaskRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setupTestViewModel() {
        stopKoin()
        fakeDataSource = FakeDataSource()
        remindersListViewModel = RemindersListViewModel( ApplicationProvider.getApplicationContext(), fakeDataSource)
    }

    @Test
    fun checkShowNoData_returnsTrue_onNoReminders() {
        // When
        remindersListViewModel.loadReminders()

        // Then
        assertThat(remindersListViewModel.showNoData.value, `is`(true))
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
        remindersListViewModel.loadReminders()

        // Then
        assertThat(remindersListViewModel.showNoData.value, `is`(false))
    }

    @Test
    fun checkShowLoadingValue_returnsTrueFalse_onLoadReminderRunningAndCompleted() {
        // When
        mainCoroutineRule.pauseDispatcher() // Pause coroutines
        remindersListViewModel.loadReminders()

        // Then
        assertThat(remindersListViewModel.showLoading.value, `is`(true))

        // When
        mainCoroutineRule.resumeDispatcher()

        // Then
        assertThat(remindersListViewModel.showLoading.value, `is`(false))
    }

    @Test
    fun onLoadReminders_showErrorMessage() {
        fakeDataSource.setReturnError(true)
        remindersListViewModel.loadReminders()

        assertThat(remindersListViewModel.showSnackBar.value, `is`("Could not find reminder"))
    }

}