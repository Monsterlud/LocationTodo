package com.udacity.project4

import android.app.Application
import com.udacity.project4.authentication.AuthenticationViewModel
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.geofence.GeofenceViewModel
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.PermissionsHandler
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

class LocationTodoApp : Application() {

	val remindersLocalRepository: RemindersLocalRepository
		get() = ServiceLocator.provideRemindersLocalRepository(this)

	override fun onCreate() {
		super.onCreate()

		/**
		 * use Koin Library as a service locator
		 */
		val locationTodoModule = module {
			//Declare a ViewModel - be later inject into Fragment with dedicated injector using by viewModel()
			viewModel {
				RemindersListViewModel(
					get(),
					get() as ReminderDataSource
				)
			}
			viewModel {
				AuthenticationViewModel(
					get()
				)
			}
			viewModel {
				GeofenceViewModel(
					get()
				)
			}
			//Declare singleton definitions to be later injected using by inject()
			single {
				//This view model is declared singleton to be used across multiple fragments
				SaveReminderViewModel(
					get(),
					get() as ReminderDataSource
				)
			}
			single<ReminderDataSource> { RemindersLocalRepository(get()) }
			single { LocalDB.createRemindersDao(this@LocationTodoApp) }
			single { PermissionsHandler() }
		}

		startKoin {
			androidContext(this@LocationTodoApp)
			modules(listOf(locationTodoModule))
		}
	}
}