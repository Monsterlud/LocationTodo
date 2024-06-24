package com.monsalud.locationtodo.locationreminders

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.monsalud.locationtodo.locationreminders.data.FakeAndroidRemindersLocalRepository
import com.monsalud.locationtodo.locationreminders.data.ReminderDataSource
import com.monsalud.locationtodo.locationreminders.reminderslist.RemindersListViewModel
import com.monsalud.locationtodo.locationreminders.savereminder.SaveReminderViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class TestApplication : Application() {

    val testModule = module {
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
        factory<ViewModelProvider.Factory> { CustomViewModelFactory(get()) }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("TestApplication", "onCreate() called")
        startKoin {
            androidContext(this@TestApplication)
            modules(
                testModule
            )
        }
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
