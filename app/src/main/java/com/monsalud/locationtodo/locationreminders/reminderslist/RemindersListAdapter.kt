package com.monsalud.locationtodo.locationreminders.reminderslist

import com.monsalud.locationtodo.R
import com.monsalud.locationtodo.base.BaseRecyclerViewAdapter
import com.monsalud.locationtodo.base.OnClickListener

// Use data binding to show the reminder on the item
class RemindersListAdapter(onClickListener: OnClickListener<ReminderDataItem>) :
    BaseRecyclerViewAdapter<ReminderDataItem>(onClickListener) {
    override fun getLayoutRes(viewType: Int) = R.layout.it_reminder
}