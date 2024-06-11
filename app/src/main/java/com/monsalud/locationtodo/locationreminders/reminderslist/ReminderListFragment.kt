package com.monsalud.locationtodo.locationreminders.reminderslist

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.databinding.DataBindingUtil
import com.firebase.ui.auth.AuthUI
import com.monsalud.locationtodo.R
import com.monsalud.locationtodo.authentication.AuthenticationActivity
import com.monsalud.locationtodo.base.BaseFragment
import com.monsalud.locationtodo.base.NavigationCommand
import com.monsalud.locationtodo.databinding.FragmentRemindersBinding
import com.monsalud.locationtodo.utils.setTitle
import com.monsalud.locationtodo.utils.setup
import org.koin.androidx.viewmodel.ext.android.viewModel

private const val TAG = "ReminderListFragment"

class ReminderListFragment : BaseFragment() {

    private lateinit var binding: FragmentRemindersBinding
    override val _viewModel: RemindersListViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_reminders, container, false
        )
        binding.viewModel = _viewModel
        binding.refreshLayout.setOnRefreshListener { _viewModel.loadReminders() }
        binding.addReminderFAB.setOnClickListener { navigateToAddReminder() }

        setHasOptionsMenu(true)
        setTitle(getString(R.string.app_name))

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lifecycleOwner = this
        setupRecyclerView()

    }

    override fun onResume() {
        super.onResume()
        // Load the reminders list on the ui
        _viewModel.loadReminders()
    }

    private fun navigateToAddReminder() {
        // Use the navigationCommand live data to navigate between the fragments
        _viewModel.navigationCommand.postValue(
            NavigationCommand.To(ReminderListFragmentDirections.toSaveReminderFragment())
        )
    }

    private fun setupRecyclerView() {
        val adapter = RemindersListAdapter {}
        // Setup the recycler view using the extension function
        binding.reminderssRecyclerView.setup(adapter)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.logout -> {
                Log.i(TAG, "***onOptionsItemSelected: LOGOUT USER")
                AuthUI.getInstance().signOut(requireContext())
                val intent = Intent(
                    requireActivity(),
                    AuthenticationActivity::class.java,
                )
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        // Display logout as menu item
        inflater.inflate(R.menu.main_menu, menu)
    }
}