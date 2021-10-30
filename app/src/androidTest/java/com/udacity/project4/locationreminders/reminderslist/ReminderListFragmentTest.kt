package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.android.gms.maps.model.LatLng
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.KoinTest
import org.mockito.Mockito
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.GlobalContext
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest {

    private lateinit var repository: ReminderDataSource

    @get: Rule
    var instantExecutorRule = InstantTaskExecutorRule()


    @Before
    fun init() {
        stopKoin()
        val appModule = module {
            viewModel {
                RemindersListViewModel(
                    ApplicationProvider.getApplicationContext(),
                    get() as ReminderDataSource
                )
            }
            single<ReminderDataSource> { RemindersLocalRepository(get()) }
            single { LocalDB.createRemindersDao(ApplicationProvider.getApplicationContext()) }
        }
        startKoin {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(listOf(appModule))
        }
        repository = GlobalContext.get().koin.get()
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun check_whenNoReminder_onReminderListFragmentNoReminderShown() {
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        onView(withText(R.string.no_data)).check(matches(isDisplayed()))
    }

    @Test
    fun check_whenReminderInDB_RemindersShowOnReminderListFragment() {
        val reminder = ReminderDTO(
            "title", "description", "location", 55.555, 45.555,
        )
        runBlocking { repository.saveReminder(reminder) }
        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        onView(withText(reminder.title)).check(matches(isDisplayed()))
        onView(withText(reminder.description)).check(matches(isDisplayed()))
        onView(withText(reminder.location)).check(matches(isDisplayed()))
    }

    @Test
    fun check_onClickFabButton_NavigateToSaveReminderScreen() {
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = Mockito.mock(NavController::class.java)
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }
        onView(withId(R.id.addReminderFAB)).perform(ViewActions.click())
        Mockito.verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }
}