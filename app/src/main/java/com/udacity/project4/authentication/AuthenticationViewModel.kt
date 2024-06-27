package com.udacity.project4.authentication

import android.app.Application
import android.util.Log
import androidx.lifecycle.map
import com.udacity.project4.base.BaseViewModel

class AuthenticationViewModel(
    app: Application
) : BaseViewModel(app) {
    enum class AuthenticationState {
        AUTHENTICATED, UNAUTHENTICATED
    }

    val authenticationState = FirebaseUserLiveData().map { user ->
        if (user != null) {
            Log.i(TAG, "*** User is signed in ***")
            AuthenticationState.AUTHENTICATED
        } else {
            Log.i(TAG, "*** User is signed out ***")
            AuthenticationState.UNAUTHENTICATED
        }
    }

    companion object {
        private const val TAG = "AuthenticationViewModel"
    }
}