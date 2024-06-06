package com.monsalud.locationtodo.locationreminders.reminderslist

import com.monsalud.locationtodo.R
import com.monsalud.locationtodo.base.BaseRecyclerViewAdapter
import com.monsalud.locationtodo.locationreminders.reminderslist.ReminderDataItem

// Use data binding to show the reminder on the item
class RemindersListAdapter(callBack: (selectedReminder: ReminderDataItem) -> Unit) :
    BaseRecyclerViewAdapter<ReminderDataItem>(callBack) {
    override fun getLayoutRes(viewType: Int) = R.layout.it_reminder
}