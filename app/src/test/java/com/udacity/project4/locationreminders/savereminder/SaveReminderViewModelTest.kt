package com.udacity.project4.locationreminders.savereminder

import android.os.Build
import android.util.Log
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.rule.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.notNullValue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@Config(sdk = [Build.VERSION_CODES.O])
class SaveReminderViewModelTest {

    private lateinit var fakeDataSource: FakeDataSource
    private lateinit var remindersViewModel: SaveReminderViewModel

    @get:Rule
    var instantTaskRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setupTestViewModel() {
        stopKoin()
        fakeDataSource = FakeDataSource()
        remindersViewModel =
            SaveReminderViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
    }

    @Test
    fun checkShowLoadingValue_returnsTrueFalse_onSaveReminderRunningAndCompleted() {
        // Given
        val reminderDataItem = ReminderDataItem(
            "title",
            "description",
            "location",
            55.55,
            45.55,
            14F,
            "Enter"
        )

        // When
        mainCoroutineRule.pauseDispatcher() // Pause coroutines
        remindersViewModel.validateAndSaveReminder(reminderDataItem)

        // Then
        assertThat(remindersViewModel.showLoading.value, `is`(true))

        // When
        mainCoroutineRule.resumeDispatcher()

        // Then
        assertThat(remindersViewModel.showLoading.value, `is`(false))
        assertThat(remindersViewModel.showToast.value, `is`("Reminder Saved !"))
    }

    @Test
    fun checkValidateEnteredData_returnsFalseAndShowsSnackbar_whenReminderTitleNull() =
        runBlockingTest {
            // Given
            val reminderDataItem = ReminderDataItem(
                null,
                "description",
                "location",
                55.55,
                45.55,
                14F,
                "Enter"
            )

            // When
            val result = remindersViewModel.validateEnteredData(reminderDataItem)

            // Then
            assertThat(result, `is`(false))
            assertThat(remindersViewModel.showSnackBarInt.value, `is`(R.string.err_enter_title))
        }

    @Test
    fun checkValidateEnteredData_returnsFalseAndShowsSnackbar_whenReminderTitleEmpty() =
        runBlockingTest {
            // Given
            val reminderDataItem = ReminderDataItem(
                "",
                "description",
                "location",
                55.55,
                45.55,
                14F,
                "Enter"
            )

            // When
            val result = remindersViewModel.validateEnteredData(reminderDataItem)

            // Then
            assertThat(result, `is`(false))
            assertThat(remindersViewModel.showSnackBarInt.value, `is`(R.string.err_enter_title))
        }

    @Test
    fun checkValidateEnteredData_returnsFalseAndShowsSnackbar_whenReminderLocationNull() =
        runBlockingTest {
            // Given
            val reminderDataItem = ReminderDataItem(
                "title",
                "description",
                null,
                55.55,
                45.55,
                14F,
                "Enter"
            )

            // When
            val result = remindersViewModel.validateEnteredData(reminderDataItem)

            // Then
            assertThat(result, `is`(false))
            assertThat(remindersViewModel.showSnackBarInt.value, `is`(R.string.err_select_location))
        }

    @Test
    fun checkValidateEnteredData_returnsFalseAndShowsSnackbar_whenReminderLocationEmpty() =
        runBlockingTest {
            // Given
            val reminderDataItem = ReminderDataItem(
                "title",
                "description",
                "",
                55.55,
                45.55,
                14F,
                "Enter"
            )

            // When
            val result = remindersViewModel.validateEnteredData(reminderDataItem)

            // Then
            assertThat(result, `is`(false))
            assertThat(remindersViewModel.showSnackBarInt.value, `is`(R.string.err_select_location))
        }

    @Test
    fun checkSaveReminder_savesReminder_whenCalled() = runBlockingTest {
        val reminderDataItem = ReminderDataItem(
            "title",
            "description",
            "",
            55.55,
            45.55,
            14F,
            "Enter"
        )

        // When
        remindersViewModel.saveReminder(reminderDataItem)

        val result = fakeDataSource.getReminder(reminderDataItem.id)

        // Then
        assertThat(fakeDataSource.reminders, `is`(notNullValue()))
        assertThat(fakeDataSource.reminders?.size, `is`(1))

        Log.i("result", result.toString())
    }

}