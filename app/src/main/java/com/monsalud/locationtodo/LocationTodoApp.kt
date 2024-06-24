package com.monsalud.locationtodo

import android.app.Application
import com.monsalud.locationtodo.authentication.AuthenticationViewModel
import com.monsalud.locationtodo.locationreminders.data.ReminderDataSource
import com.monsalud.locationtodo.locationreminders.data.local.LocalDB
import com.monsalud.locationtodo.locationreminders.data.local.RemindersLocalRepository
import com.monsalud.locationtodo.locationreminders.geofence.GeofenceUtils
import com.monsalud.locationtodo.locationreminders.geofence.GeofenceViewModel
import com.monsalud.locationtodo.locationreminders.reminderslist.RemindersListViewModel
import com.monsalud.locationtodo.locationreminders.savereminder.SaveReminderViewModel
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
			single { GeofenceUtils() }
		}

		startKoin {
			androidContext(this@LocationTodoApp)
			modules(listOf(locationTodoModule))
		}
	}
}