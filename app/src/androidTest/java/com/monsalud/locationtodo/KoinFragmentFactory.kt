package com.monsalud.locationtodo

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.monsalud.locationtodo.locationreminders.reminderslist.ReminderListFragment

class KoinFragmentFactory : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        return when (className) {
            ReminderListFragment::class.java.name -> ReminderListFragment()
            else -> super.instantiate(classLoader, className)
        }
    }
}