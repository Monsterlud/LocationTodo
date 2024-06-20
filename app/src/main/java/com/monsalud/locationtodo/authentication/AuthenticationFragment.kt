package com.monsalud.locationtodo.authentication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.monsalud.locationtodo.base.BaseFragment
import com.monsalud.locationtodo.databinding.FragmentAuthenticationBinding
import com.monsalud.locationtodo.locationreminders.RemindersActivity
import org.koin.androidx.viewmodel.ext.android.viewModel

class AuthenticationFragment : BaseFragment() {
    private lateinit var binding: FragmentAuthenticationBinding
    override val _viewModel: AuthenticationViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAuthenticationBinding.inflate(inflater, container, false)

        binding.btnLogin.setOnClickListener {
            launchSigninFlow()
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeAuthenticationState()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val response = IdpResponse.fromResultIntent(data)
        if (requestCode == SIGN_IN_RESULT_CODE) {
            Log.i(
                TAG,
                "***onActivityResult: Successfully signed in user ${FirebaseAuth.getInstance().currentUser?.displayName}"
            )
        } else {
            Log.i(TAG, "***onActivityResult: Sign in unsuccessful ${response?.error?.errorCode}")
        }
    }

    private fun observeAuthenticationState() {
        _viewModel.authenticationState.observe(viewLifecycleOwner) {
            when (it) {
                AuthenticationViewModel.AuthenticationState.AUTHENTICATED -> {
                    Log.i(TAG, "***observeAuthenticationState: User is authenticated")
                    val intent = Intent(
                        requireActivity(),
                        RemindersActivity::class.java,
                    )
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } else -> {
                    Log.i(TAG, "***observeAuthenticationState: User in Unauthenticated")
                }
            }
        }
    }

    private fun launchSigninFlow() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            SIGN_IN_RESULT_CODE
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    companion object {
        private const val TAG = "AuthenticationActivity"
        private const val SIGN_IN_RESULT_CODE = 1001
    }
}