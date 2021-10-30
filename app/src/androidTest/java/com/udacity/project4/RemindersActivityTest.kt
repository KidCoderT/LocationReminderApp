package com.udacity.project4

import android.app.Activity
import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    private val dataBindingIdlingResource = DataBindingIdlingResource()

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @Before
    fun registerIdlingResource(): Unit = IdlingRegistry.getInstance().run {
        register(EspressoIdlingResource.countingIdlingResource)
        register(dataBindingIdlingResource)
    }

    @After
    fun unregisterIdlingResource(): Unit = IdlingRegistry.getInstance().run {
        unregister(EspressoIdlingResource.countingIdlingResource)
        unregister(dataBindingIdlingResource)
    }

    @Test
    fun launchRemindersActivityWithAReminderShown() {

        val reminder = ReminderDTO(
            "title", "description", "location", 55.555, 45.555,
        )

        runBlocking {
            repository.saveReminder(reminder)
        }

        val scenario = ActivityScenario.launch(RemindersActivity::class.java)

        dataBindingIdlingResource.monitorActivity(scenario)

        onView(withText(reminder.title))
            .check(matches(isDisplayed()))
        onView(withText(reminder.description))
            .check(matches(isDisplayed()))
        onView(withText(reminder.location))
            .check(matches(isDisplayed()))
    }

    @Test
    fun addReminderAndNavigateBack() {
        val reminder = ReminderDTO(
            "title", "description", "location", 55.555, 45.555,
        )

        val scenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(scenario)

        // Check There are no Reminders
        onView(withId(R.id.noDataTextView)).check(matches(withEffectiveVisibility(Visibility.VISIBLE)))
        // Go the save reminder Screen
        onView(withId(R.id.addReminderFAB)).perform(ViewActions.click())
        // fill in form text
        onView(withId(R.id.reminderTitle))
            .perform(ViewActions.typeText(reminder.title))
        onView(withId(R.id.reminderDescription))
            .perform(ViewActions.typeText(reminder.description))
        // Close keyboard because blocking view
        Espresso.closeSoftKeyboard()
        // Go to select location screen
        onView(withId(R.id.selectLocation)).perform(ViewActions.click())
        // Select Random Location
        // note: it doesn't need to be specific as no notification test will be there
        onView(withId(R.id.google_map)).perform(ViewActions.click())
        Thread.sleep(1000)
        // Check bottom sheet opened by checking whether bottom most item is shown
        onView(withId(R.id.geofence_request_expiration_field_container)).check(matches(isDisplayed()))
        // Click the Select location fab button
        onView(withId(R.id.select_location_fab)).perform(ViewActions.click())
        // Save the reminder finally
        onView(withId(R.id.saveReminder)).perform(ViewActions.click())
        // Check on Reminder List scree whether no Data is gone and new Reminder is shown
        onView(withId(R.id.noDataTextView)).check(matches(withEffectiveVisibility(Visibility.GONE)))
        onView(withText(reminder.title))
            .check(matches(isDisplayed()))
        onView(withText(reminder.description))
            .check(matches(isDisplayed()))
    }
}
