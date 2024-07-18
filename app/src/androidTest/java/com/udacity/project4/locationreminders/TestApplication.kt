package com.udacity.project4.locationreminders

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.udacity.project4.locationreminders.data.FakeAndroidRemindersLocalRepository
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.geofence.GeofenceUtils
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

class TestApplication : Application() {

    lateinit var testModule: Module

    override fun onCreate() {
        super.onCreate()
        Log.d("TestApplication", "onCreate() called")

        testModule = module {
            single { FakeAndroidRemindersLocalRepository() }
            single<ReminderDataSource> { FakeAndroidRemindersLocalRepository() }
            single {
                RemindersListViewModel(
                    this@TestApplication,
                    get() as ReminderDataSource
                )
            }
            single {
                Log.d("Koin", "Creating SaveReminderViewModel")
                SaveReminderViewModel(this@TestApplication, get())
            }
            single { GeofenceUtils() }
            factory<ViewModelProvider.Factory> { CustomViewModelFactory(get()) }
        }

        startKoin {
            androidContext(this@TestApplication)
            modules(
                testModule
            )
        }
    }

    fun setupTestModule(repository: ReminderDataSource) {
        testModule = module {
            single<ReminderDataSource> { repository }
            single {
                RemindersListViewModel(
                    this@TestApplication,
                    get() as ReminderDataSource
                )
            }
            single {
                Log.d("Koin", "Creating SaveReminderViewModel")
                SaveReminderViewModel(this@TestApplication, get())
            }
            factory<ViewModelProvider.Factory> { CustomViewModelFactory(get()) }
        }
        loadKoinModules(testModule)
    }


}

class CustomViewModelFactory(private val viewModel: RemindersListViewModel) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RemindersListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return viewModel as T
        } else {
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
