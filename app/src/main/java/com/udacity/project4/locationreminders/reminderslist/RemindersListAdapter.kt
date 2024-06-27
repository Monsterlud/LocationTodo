package com.udacity.project4.locationreminders.reminderslist

import com.udacity.project4.R
import com.udacity.project4.base.BaseRecyclerViewAdapter
import com.udacity.project4.base.OnClickListener

// Use data binding to show the reminder on the item
class RemindersListAdapter(onClickListener: OnClickListener<ReminderDataItem>) :
    BaseRecyclerViewAdapter<ReminderDataItem>(onClickListener) {
    override fun getLayoutRes(viewType: Int) = R.layout.it_reminder
}